// pkg/python_bridge/client.go
package python_bridge

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"lfk-backend/internal/config"
	"lfk-backend/internal/redis"
	"log"
	"net/http"
	"sync"
	"time"

	"github.com/google/uuid"
)

type Client struct {
	pool        *ProcessorPool
	redisClient *redis.RedisClient // Изменено с redisQueue на redisClient
	queueName   string
	config      *config.PythonProcessorConfig
	httpClient  *http.Client
}

type ProcessorInfo struct {
	ID          string    `json:"id"`
	Address     string    `json:"address"`
	Healthy     bool      `json:"healthy"`
	LastSeen    time.Time `json:"last_seen"`
	Load        int       `json:"load"`
	TotalFrames int64     `json:"total_frames"`
	AvgTime     float64   `json:"avg_time_ms"`
}

type ProcessorPool struct {
	processors map[string]*ProcessorInfo
	mu         sync.RWMutex
	discovery  ServiceDiscovery
	config     *config.PythonProcessorConfig
	nextIdx    int
}

type ServiceDiscovery interface {
	DiscoverProcessors() ([]ProcessorInfo, error)
	Watch(ctx context.Context) (<-chan []ProcessorInfo, error)
}

// Структурированные данные для упражнения
type StructuredData struct {
	State       interface{} `json:"state"`
	StateName   string      `json:"state_name"`
	Countdown   *int        `json:"countdown,omitempty"`
	Progress    float64     `json:"progress_percent"`
	Cycle       int         `json:"current_cycle"`
	TotalCycles int         `json:"total_cycles"`
	Status      string      `json:"status,omitempty"`
	Completed   bool        `json:"completed"`
	Message     string      `json:"message"`
	AutoReset   bool        `json:"auto_reset"`
	Step        int         `json:"step,omitempty"`
	StepName    string      `json:"step_name,omitempty"`
}

type FrameResponse struct {
	FistDetected    bool            `json:"fist_detected"`
	HandDetected    bool            `json:"hand_detected"`
	RaisedFingers   int             `json:"raised_fingers"`
	FingerStates    []bool          `json:"finger_states"`
	Message         string          `json:"message"`
	ProcessedFrame  string          `json:"processed_frame"`
	CurrentExercise string          `json:"current_exercise"`
	ExerciseName    string          `json:"exercise_name"`
	Status          string          `json:"status"`
	Structured      *StructuredData `json:"structured,omitempty"`
	Error           string          `json:"error,omitempty"`
}

type FrameTask struct {
	TaskID       string          `json:"task_id"`
	UserID       string          `json:"user_id"`
	SessionID    string          `json:"session_id"`
	FrameData    string          `json:"frame_data"`
	ExerciseType string          `json:"exercise_type"`
	Timestamp    int64           `json:"timestamp"`
	Priority     int             `json:"priority"`
	RetryCount   int             `json:"retry_count"`
	MaxRetries   int             `json:"max_retries"`
	Metadata     json.RawMessage `json:"metadata,omitempty"`
}

type FrameResult struct {
	TaskID      string         `json:"task_id"`
	UserID      string         `json:"user_id"`
	SessionID   string         `json:"session_id"`
	Feedback    *FrameResponse `json:"feedback"`
	Error       string         `json:"error,omitempty"`
	ProcessedAt time.Time      `json:"processed_at"`
	ProcessorID string         `json:"processor_id"`
	ProcessTime int64          `json:"process_time_ms"` // Изменено на int64 для миллисекунд
}

func NewClient(redisClient *redis.RedisClient, cfg *config.PythonProcessorConfig) (*Client, error) {
	pool, err := NewProcessorPool(cfg)
	if err != nil {
		return nil, fmt.Errorf("failed to create processor pool: %w", err)
	}

	httpClient := &http.Client{
		Timeout: cfg.Pool.RequestTimeout,
		Transport: &http.Transport{
			MaxIdleConns:        100,
			MaxIdleConnsPerHost: 10,
			IdleConnTimeout:     90 * time.Second,
		},
	}

	return &Client{
		pool:        pool,
		redisClient: redisClient,
		queueName:   "queue:python:tasks", // Добавляем имя очереди
		config:      cfg,
		httpClient:  httpClient,
	}, nil
}

// ProcessFrame отправляет кадр на обработку (синхронно)
func (c *Client) ProcessFrame(ctx context.Context, request map[string]interface{}) (*FrameResponse, error) {
	processor := c.pool.GetNextProcessor()
	if processor == nil {
		return nil, fmt.Errorf("no healthy processors available")
	}

	jsonData, err := json.Marshal(request)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	url := fmt.Sprintf("http://%s/process", processor.Address)

	req, err := http.NewRequestWithContext(ctx, "POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		c.pool.MarkUnhealthy(processor.ID)
		return nil, fmt.Errorf("failed to send request to processor %s: %w", processor.ID, err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response: %w", err)
	}

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("processor returned error %d: %s", resp.StatusCode, string(body))
	}

	var result FrameResponse
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, fmt.Errorf("failed to unmarshal response: %w", err)
	}

	return &result, nil
}

// ProcessFrameAsync отправляет кадр на асинхронную обработку через очередь
func (c *Client) ProcessFrameAsync(ctx context.Context, userID, sessionID, frameData, exerciseType string, priority int) (string, error) {
	task := FrameTask{
		TaskID:       uuid.New().String(),
		UserID:       userID,
		SessionID:    sessionID,
		FrameData:    frameData,
		ExerciseType: exerciseType,
		Timestamp:    time.Now().Unix(),
		Priority:     priority,
		RetryCount:   0,
		MaxRetries:   3, // Значение по умолчанию
	}

	taskJSON, err := json.Marshal(task)
	if err != nil {
		return "", fmt.Errorf("failed to marshal task: %w", err)
	}

	// Используем LPUSH для очереди
	err = c.redisClient.GetClient().LPush(ctx, c.queueName, taskJSON).Err()
	if err != nil {
		return "", fmt.Errorf("failed to push task to queue: %w", err)
	}

	return task.TaskID, nil
}

// GetResult получает результат обработки из Redis
func (c *Client) GetResult(ctx context.Context, taskID string) (*FrameResult, error) {
	resultKey := fmt.Sprintf("result:%s", taskID)

	data, err := c.redisClient.Get(ctx, resultKey)
	if err != nil {
		return nil, fmt.Errorf("failed to get result: %w", err)
	}

	var result FrameResult
	if err := json.Unmarshal([]byte(data), &result); err != nil {
		return nil, fmt.Errorf("failed to unmarshal result: %w", err)
	}

	return &result, nil
}

// WaitForResult ожидает результат с таймаутом
func (c *Client) WaitForResult(ctx context.Context, taskID string, timeout time.Duration) (*FrameResult, error) {
	ctx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()

	// Подписываемся на канал результатов
	pubsub := c.redisClient.Subscribe(ctx, "result:"+taskID)
	defer pubsub.Close()

	ch := pubsub.Channel()

	for {
		select {
		case <-ctx.Done():
			return nil, ctx.Err()
		case msg := <-ch:
			var result FrameResult
			if err := json.Unmarshal([]byte(msg.Payload), &result); err != nil {
				continue
			}
			return &result, nil
		default:
			// Проверяем, может результат уже есть
			result, err := c.GetResult(ctx, taskID)
			if err == nil && result != nil {
				return result, nil
			}
			time.Sleep(100 * time.Millisecond)
		}
	}
}

// NewProcessorPool создает новый пул процессоров
func NewProcessorPool(cfg *config.PythonProcessorConfig) (*ProcessorPool, error) {
	pool := &ProcessorPool{
		processors: make(map[string]*ProcessorInfo),
		config:     cfg,
		mu:         sync.RWMutex{},
		nextIdx:    0,
	}

	// Инициализируем статический список если есть
	for _, addr := range cfg.StaticAddresses {
		pool.processors[addr] = &ProcessorInfo{
			ID:       addr,
			Address:  addr,
			Healthy:  true,
			LastSeen: time.Now(),
		}
	}

	// Запускаем discovery если нужно
	if cfg.ServiceDiscovery != "static" {
		go pool.watchProcessors()
	}

	// Запускаем health check
	go pool.healthCheck()

	return pool, nil
}

func (p *ProcessorPool) GetNextProcessor() *ProcessorInfo {
	p.mu.RLock()
	defer p.mu.RUnlock()

	if len(p.processors) == 0 {
		return nil
	}

	// Round-robin выбор с проверкой здоровья
	healthy := make([]*ProcessorInfo, 0)
	for _, proc := range p.processors {
		if proc.Healthy {
			healthy = append(healthy, proc)
		}
	}

	if len(healthy) == 0 {
		return nil
	}

	p.nextIdx = (p.nextIdx + 1) % len(healthy)
	return healthy[p.nextIdx]
}

func (p *ProcessorPool) MarkUnhealthy(processorID string) {
	p.mu.Lock()
	defer p.mu.Unlock()

	if proc, ok := p.processors[processorID]; ok {
		proc.Healthy = false
		log.Printf("Processor %s marked as unhealthy", processorID)
	}
}

func (p *ProcessorPool) healthCheck() {
	ticker := time.NewTicker(p.config.Pool.HealthCheckInterval)
	defer ticker.Stop()

	client := &http.Client{
		Timeout: 2 * time.Second,
	}

	for range ticker.C {
		p.mu.RLock()
		processors := make([]*ProcessorInfo, 0, len(p.processors))
		for _, proc := range p.processors {
			processors = append(processors, proc)
		}
		p.mu.RUnlock()

		for _, proc := range processors {
			go func(pInfo *ProcessorInfo) {
				url := fmt.Sprintf("http://%s/health", pInfo.Address)
				resp, err := client.Get(url)

				p.mu.Lock()
				defer p.mu.Unlock()

				if err == nil && resp.StatusCode == http.StatusOK {
					pInfo.Healthy = true
					pInfo.LastSeen = time.Now()
					resp.Body.Close()
				} else {
					pInfo.Healthy = false
				}
			}(proc)
		}
	}
}

func (p *ProcessorPool) watchProcessors() {
	// Здесь можно реализовать динамическое обнаружение через Consul, Kubernetes API и т.д.
}
