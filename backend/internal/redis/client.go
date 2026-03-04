// internal/redis/client.go
package redis

import (
	"context"
	"encoding/json"
	"fmt"
	"lfk-backend/internal/config"
	"time"

	"github.com/go-redis/redis/v8"
	_ "github.com/google/uuid"
)

type RedisClient struct {
	client   redis.UniversalClient
	cluster  bool
	config   *config.RedisConfig
	serverID string
}

type RedisTask struct {
	ID         string          `json:"id"`
	Type       string          `json:"type"`
	Data       json.RawMessage `json:"data"`
	Priority   int             `json:"priority"`
	CreatedAt  time.Time       `json:"created_at"`
	RetryCount int             `json:"retry_count"`
}

type RedisResult struct {
	TaskID      string          `json:"task_id"`
	Data        json.RawMessage `json:"data"`
	Error       string          `json:"error,omitempty"`
	ProcessedAt time.Time       `json:"processed_at"`
	ProcessorID string          `json:"processor_id"`
}

func NewRedisClient(cfg *config.RedisConfig, serverID string) (*RedisClient, error) {
	var client redis.UniversalClient

	switch cfg.Mode {
	case "cluster":
		client = redis.NewClusterClient(&redis.ClusterOptions{
			Addrs:        cfg.Addresses,
			Password:     cfg.Password,
			PoolSize:     cfg.PoolSize,
			MinIdleConns: cfg.MinIdleConns,
			DialTimeout:  cfg.DialTimeout,
			ReadTimeout:  cfg.ReadTimeout,
			WriteTimeout: cfg.WriteTimeout,
		})
	case "sentinel":
		// Для sentinel режима
		client = redis.NewFailoverClient(&redis.FailoverOptions{
			MasterName:    "mymaster",
			SentinelAddrs: cfg.Addresses,
			Password:      cfg.Password,
			PoolSize:      cfg.PoolSize,
			MinIdleConns:  cfg.MinIdleConns,
		})
	default:
		// Одиночный экземпляр
		client = redis.NewClient(&redis.Options{
			Addr:         cfg.Addresses[0],
			Password:     cfg.Password,
			DB:           0,
			PoolSize:     cfg.PoolSize,
			MinIdleConns: cfg.MinIdleConns,
			DialTimeout:  cfg.DialTimeout,
			ReadTimeout:  cfg.ReadTimeout,
			WriteTimeout: cfg.WriteTimeout,
		})
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := client.Ping(ctx).Err(); err != nil {
		return nil, fmt.Errorf("failed to connect to redis: %w", err)
	}

	return &RedisClient{
		client:   client,
		cluster:  cfg.Mode == "cluster",
		config:   cfg,
		serverID: serverID,
	}, nil
}

// Close закрывает соединение с Redis
func (r *RedisClient) Close() error {
	return r.client.Close()
}

// GetClient возвращает сырой redis клиент
func (r *RedisClient) GetClient() redis.UniversalClient {
	return r.client
}

// Publish публикует сообщение в канал
func (r *RedisClient) Publish(ctx context.Context, channel string, message interface{}) error {
	return r.client.Publish(ctx, channel, message).Err()
}

// Subscribe подписывается на каналы
func (r *RedisClient) Subscribe(ctx context.Context, channels ...string) *redis.PubSub {
	return r.client.Subscribe(ctx, channels...)
}

// Set устанавливает значение с TTL
func (r *RedisClient) Set(ctx context.Context, key string, value interface{}, ttl time.Duration) error {
	return r.client.Set(ctx, key, value, ttl).Err()
}

// Get получает значение по ключу
func (r *RedisClient) Get(ctx context.Context, key string) (string, error) {
	return r.client.Get(ctx, key).Result()
}

// Del удаляет ключи
func (r *RedisClient) Del(ctx context.Context, keys ...string) error {
	return r.client.Del(ctx, keys...).Err()
}

// Exists проверяет существование ключа
func (r *RedisClient) Exists(ctx context.Context, key string) (bool, error) {
	result, err := r.client.Exists(ctx, key).Result()
	return result > 0, err
}

// Incr увеличивает счетчик
func (r *RedisClient) Incr(ctx context.Context, key string) (int64, error) {
	return r.client.Incr(ctx, key).Result()
}

// Decr уменьшает счетчик
func (r *RedisClient) Decr(ctx context.Context, key string) (int64, error) {
	return r.client.Decr(ctx, key).Result()
}

// Expire устанавливает время жизни ключа
func (r *RedisClient) Expire(ctx context.Context, key string, ttl time.Duration) error {
	return r.client.Expire(ctx, key, ttl).Err()
}

// HSet устанавливает поле в хеше
func (r *RedisClient) HSet(ctx context.Context, key string, values ...interface{}) error {
	return r.client.HSet(ctx, key, values).Err()
}

// HGet получает поле из хеша
func (r *RedisClient) HGet(ctx context.Context, key, field string) (string, error) {
	return r.client.HGet(ctx, key, field).Result()
}

// HGetAll получает все поля хеша
func (r *RedisClient) HGetAll(ctx context.Context, key string) (map[string]string, error) {
	return r.client.HGetAll(ctx, key).Result()
}

// HDel удаляет поля из хеша
func (r *RedisClient) HDel(ctx context.Context, key string, fields ...string) error {
	return r.client.HDel(ctx, key, fields...).Err()
}

// SAdd добавляет элемент в множество
func (r *RedisClient) SAdd(ctx context.Context, key string, members ...interface{}) error {
	return r.client.SAdd(ctx, key, members...).Err()
}

// SMembers получает все элементы множества
func (r *RedisClient) SMembers(ctx context.Context, key string) ([]string, error) {
	return r.client.SMembers(ctx, key).Result()
}

// SRem удаляет элемент из множества
func (r *RedisClient) SRem(ctx context.Context, key string, members ...interface{}) error {
	return r.client.SRem(ctx, key, members...).Err()
}

// ZAdd добавляет элемент в сортированное множество
func (r *RedisClient) ZAdd(ctx context.Context, key string, score float64, member interface{}) error {
	return r.client.ZAdd(ctx, key, &redis.Z{
		Score:  score,
		Member: member,
	}).Err()
}

// ZRange получает диапазон из сортированного множества
func (r *RedisClient) ZRange(ctx context.Context, key string, start, stop int64) ([]string, error) {
	return r.client.ZRange(ctx, key, start, stop).Result()
}

// ZRevRange получает обратный диапазон
func (r *RedisClient) ZRevRange(ctx context.Context, key string, start, stop int64) ([]string, error) {
	return r.client.ZRevRange(ctx, key, start, stop).Result()
}
