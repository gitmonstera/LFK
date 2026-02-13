package python_bridge

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

type Client struct {
	BaseURL    string
	HTTPClient *http.Client
}

func NewClient(baseURL string) *Client {
	return &Client{
		BaseURL: baseURL,
		HTTPClient: &http.Client{
			Timeout: 30 * time.Second, // Увеличиваем таймаут
		},
	}
}

type FrameResponse struct {
	FistDetected    bool   `json:"fist_detected"`
	HandDetected    bool   `json:"hand_detected"`
	RaisedFingers   int    `json:"raised_fingers"`
	FingerStates    []bool `json:"finger_states"`
	Message         string `json:"message"`
	ProcessedFrame  string `json:"processed_frame"`
	CurrentExercise string `json:"current_exercise"`
	Status          string `json:"status"`
	Error           string `json:"error,omitempty"`
}

// ProcessFrame отправляет запрос в Python
func (c *Client) ProcessFrame(requestData interface{}) (*FrameResponse, error) {
	jsonData, err := json.Marshal(requestData)
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

	// Проверяем статус ответа
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("Python server returned status %d: %s", resp.StatusCode, string(body))
	}

	// Парсим ответ
	var result FrameResponse
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, fmt.Errorf("error unmarshaling response: %v", err)
	}

	return &result, nil
}
