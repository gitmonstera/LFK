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
			Timeout: 30 * time.Second,
		},
	}
}

// Структурированные данные для упражнения Кулак-ладонь
type StructuredData struct {
	Step        int     `json:"step"`
	StepName    string  `json:"step_name"`
	Countdown   *int    `json:"countdown,omitempty"`
	Progress    float64 `json:"progress"`
	Cycle       int     `json:"cycle"`
	TotalCycles int     `json:"total_cycles"`
	Status      string  `json:"status"`
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

// ProcessFrame отправляет запрос в Python
func (c *Client) ProcessFrame(requestData interface{}) (*FrameResponse, error) {
	jsonData, err := json.Marshal(requestData)
	if err != nil {
		return nil, fmt.Errorf("error marshaling request: %v", err)
	}

	resp, err := c.HTTPClient.Post(c.BaseURL+"/process", "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, fmt.Errorf("error sending request to Python: %v", err)
	}
	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {

		}
	}(resp.Body)

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("error reading response: %v", err)
	}

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("Python server returned status %d: %s", resp.StatusCode, string(body))
	}

	var result FrameResponse
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, fmt.Errorf("error unmarshaling response: %v", err)
	}

	return &result, nil
}
