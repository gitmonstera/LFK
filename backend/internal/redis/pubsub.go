// internal/redis/pubsub.go
package redis

import (
	"context"
	"encoding/json"
	"log"
	"sync"

	"github.com/go-redis/redis/v8"
)

type MessageHandler func(channel string, data []byte)

type PubSubManager struct {
	client     *RedisClient
	handlers   map[string]MessageHandler
	handlersMu sync.RWMutex
	pubsub     *redis.PubSub
	ctx        context.Context
	cancel     context.CancelFunc
}

func NewPubSubManager(client *RedisClient) *PubSubManager {
	ctx, cancel := context.WithCancel(context.Background())
	return &PubSubManager{
		client:   client,
		handlers: make(map[string]MessageHandler),
		ctx:      ctx,
		cancel:   cancel,
	}
}

// Subscribe подписывается на каналы с обработчиками
func (m *PubSubManager) Subscribe(handlers map[string]MessageHandler) error {
	m.handlersMu.Lock()
	for channel, handler := range handlers {
		m.handlers[channel] = handler
	}
	m.handlersMu.Unlock()

	channels := make([]string, 0, len(handlers))
	for channel := range handlers {
		channels = append(channels, channel)
	}

	m.pubsub = m.client.Subscribe(m.ctx, channels...)

	go m.listen()
	return nil
}

// listen слушает сообщения из Redis
func (m *PubSubManager) listen() {
	ch := m.pubsub.Channel()

	for {
		select {
		case <-m.ctx.Done():
			return
		case msg, ok := <-ch:
			if !ok {
				return
			}

			m.handlersMu.RLock()
			handler, exists := m.handlers[msg.Channel]
			m.handlersMu.RUnlock()

			if exists {
				func() {
					defer func() {
						if r := recover(); r != nil {
							log.Printf("Panic in message handler for channel %s: %v", msg.Channel, r)
						}
					}()
					handler(msg.Channel, []byte(msg.Payload))
				}()
			}
		}
	}
}

// Publish публикует сообщение в канал
func (m *PubSubManager) Publish(channel string, data interface{}) error {
	return m.client.Publish(m.ctx, channel, data)
}

// PublishJSON публикует JSON сообщение
func (m *PubSubManager) PublishJSON(channel string, data interface{}) error {
	jsonData, err := json.Marshal(data)
	if err != nil {
		return err
	}
	return m.client.Publish(m.ctx, channel, jsonData)
}

// Close закрывает подписку
func (m *PubSubManager) Close() error {
	m.cancel()
	if m.pubsub != nil {
		return m.pubsub.Close()
	}
	return nil
}
