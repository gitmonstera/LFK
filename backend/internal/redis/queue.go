// internal/redis/queue.go
package redis

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/go-redis/redis/v8"
	"github.com/google/uuid"
)

type QueueManager struct {
	client    *RedisClient
	queueName string
}

type QueueTask struct {
	ID         string          `json:"id"`
	Type       string          `json:"type"`
	Data       json.RawMessage `json:"data"`
	Priority   int             `json:"priority"`
	CreatedAt  time.Time       `json:"created_at"`
	RetryCount int             `json:"retry_count"`
	MaxRetries int             `json:"max_retries"`
}

func NewQueueManager(client *RedisClient, queueName string) *QueueManager {
	return &QueueManager{
		client:    client,
		queueName: queueName,
	}
}

// Push добавляет задачу в очередь
func (q *QueueManager) Push(ctx context.Context, taskType string, data interface{}, priority int, maxRetries int) (string, error) {
	taskData, err := json.Marshal(data)
	if err != nil {
		return "", fmt.Errorf("failed to marshal task data: %w", err)
	}

	task := QueueTask{
		ID:         uuid.New().String(),
		Type:       taskType,
		Data:       taskData,
		Priority:   priority,
		CreatedAt:  time.Now(),
		MaxRetries: maxRetries,
	}

	taskJSON, err := json.Marshal(task)
	if err != nil {
		return "", fmt.Errorf("failed to marshal task: %w", err)
	}

	// Используем LPUSH для приоритетной очереди (чем выше приоритет, тем раньше)
	score := float64(time.Now().UnixNano()) - float64(priority)*1e12

	err = q.client.client.ZAdd(ctx, q.queueName, &redis.Z{
		Score:  score,
		Member: taskJSON,
	}).Err()

	if err != nil {
		return "", fmt.Errorf("failed to push task to queue: %w", err)
	}

	return task.ID, nil
}

// Pop забирает задачу из очереди (блокирующая операция)
func (q *QueueManager) Pop(ctx context.Context, timeout time.Duration) (*QueueTask, error) {
	// Используем ZRANGE с удалением
	pipeliner := q.client.client.TxPipeline()

	// Получаем задачу с наименьшим score (самую старую с высоким приоритетом)
	cmd := pipeliner.ZRange(ctx, q.queueName, 0, 0)
	pipeliner.ZRemRangeByRank(ctx, q.queueName, 0, 0)

	_, err := pipeliner.Exec(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to pop task from queue: %w", err)
	}

	vals := cmd.Val()
	if len(vals) == 0 {
		return nil, nil
	}

	var task QueueTask
	if err := json.Unmarshal([]byte(vals[0]), &task); err != nil {
		return nil, fmt.Errorf("failed to unmarshal task: %w", err)
	}

	return &task, nil
}

// PopBlocking забирает задачу с блокировкой (ждет если очередь пуста)
func (q *QueueManager) PopBlocking(ctx context.Context, timeout time.Duration) (*QueueTask, error) {
	for {
		select {
		case <-ctx.Done():
			return nil, ctx.Err()
		default:
			task, err := q.Pop(ctx, 0)
			if err != nil {
				return nil, err
			}
			if task != nil {
				return task, nil
			}

			// Ждем немного перед следующим запросом
			time.Sleep(100 * time.Millisecond)
		}
	}
}

// Peek просматривает задачу без удаления
func (q *QueueManager) Peek(ctx context.Context) (*QueueTask, error) {
	vals, err := q.client.client.ZRange(ctx, q.queueName, 0, 0).Result()
	if err != nil {
		return nil, fmt.Errorf("failed to peek task: %w", err)
	}

	if len(vals) == 0 {
		return nil, nil
	}

	var task QueueTask
	if err := json.Unmarshal([]byte(vals[0]), &task); err != nil {
		return nil, fmt.Errorf("failed to unmarshal task: %w", err)
	}

	return &task, nil
}

// Len возвращает длину очереди
func (q *QueueManager) Len(ctx context.Context) (int64, error) {
	return q.client.client.ZCard(ctx, q.queueName).Result()
}

// Remove удаляет задачу из очереди по ID
func (q *QueueManager) Remove(ctx context.Context, taskID string) error {
	// Получаем все задачи
	vals, err := q.client.client.ZRange(ctx, q.queueName, 0, -1).Result()
	if err != nil {
		return err
	}

	for _, val := range vals {
		var task QueueTask
		if err := json.Unmarshal([]byte(val), &task); err != nil {
			continue
		}
		if task.ID == taskID {
			return q.client.client.ZRem(ctx, q.queueName, val).Err()
		}
	}

	return nil
}

// Clear очищает очередь
func (q *QueueManager) Clear(ctx context.Context) error {
	return q.client.client.Del(ctx, q.queueName).Err()
}
