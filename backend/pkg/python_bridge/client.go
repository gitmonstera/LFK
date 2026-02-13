package python_bridge

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

// Client для связи с Python сервером
type Client struct {
	BaseURL    string
	HTTPClient *http.Client
}

// NewClient создает нового клиента
func NewClient(baseURL string) *Client {
	return &Client{
		BaseURL: baseURL,
		HTTPClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

// FrameRequest структура запроса к Python
type FrameRequest struct {
	Frame string `json:"frame"`
}

// FrameResponse структура ответа от Python
type FrameResponse struct {
	FistDetected   bool   `json:"fist_detected"`
	HandDetected   bool   `json:"hand_detected"`
	RaisedFingers  int    `json:"raised_fingers"`
	FingerStates   []bool `json:"finger_states"`
	Message        string `json:"message"`
	ProcessedFrame string `json:"processed_frame"`
	Status         string `json:"status"`
	Error          string `json:"error,omitempty"`
	Timestamp      int64  `json:"timestamp"`
}

// ProcessFrame отправляет кадр в Python для обработки
func (c *Client) ProcessFrame(frameBase64 string) (*FrameResponse, error) {
	// Создаем запрос
	req := FrameRequest{
		Frame: frameBase64,
	}

	jsonData, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("error marshaling request: %v", err)
	}

	// Отправляем запрос
	resp, err := c.HTTPClient.Post(c.BaseURL+"/process", "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, fmt.Errorf("error sending request to Python: %v", err)
	}
	defer resp.Body.Close()

	// Читаем ответ
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("error reading response: %v", err)
	}

	// Парсим ответ
	var result FrameResponse
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, fmt.Errorf("error unmarshaling response: %v", err)
	}

	return &result, nil
}
