// internal/config/config.go
package config

import (
	"fmt"
	"strings"
	"time"

	"github.com/spf13/viper"
)

type Config struct {
	Server          ServerConfig          `mapstructure:"server"`
	Database        DatabaseConfig        `mapstructure:"database"`
	Redis           RedisConfig           `mapstructure:"redis"`
	PythonProcessor PythonProcessorConfig `mapstructure:"python_processor"`
	Auth            AuthConfig            `mapstructure:"auth"`
	WebSocket       WebSocketConfig       `mapstructure:"websocket"`
	LoadBalancer    LoadBalancerConfig    `mapstructure:"load_balancer"`
	Queues          QueueConfig           `mapstructure:"queues"`
	Cache           CacheConfig           `mapstructure:"cache"`
	Monitoring      MonitoringConfig      `mapstructure:"monitoring"`
	Logging         LoggingConfig         `mapstructure:"logging"`
	RateLimiting    RateLimitingConfig    `mapstructure:"rate_limiting"`
	Security        SecurityConfig        `mapstructure:"security"`
}

type ServerConfig struct {
	Name            string        `mapstructure:"name"`
	Environment     string        `mapstructure:"environment"`
	HTTPPort        int           `mapstructure:"http_port"`
	GRPCPort        int           `mapstructure:"grpc_port"`
	ShutdownTimeout time.Duration `mapstructure:"shutdown_timeout"`
	Instances       struct {
		Min int    `mapstructure:"min"`
		Max int    `mapstructure:"max"`
		ID  string `mapstructure:"id"`
	} `mapstructure:"instances"`
}

type DatabaseConfig struct {
	Postgres struct {
		Master   PostgresInstance   `mapstructure:"master"`
		Replicas []PostgresInstance `mapstructure:"replicas"`
	} `mapstructure:"postgres"`
}

type PostgresInstance struct {
	Host               string        `mapstructure:"host"`
	Port               int           `mapstructure:"port"`
	User               string        `mapstructure:"user"`
	Password           string        `mapstructure:"password"`
	Database           string        `mapstructure:"database"`
	SSLMode            string        `mapstructure:"ssl_mode"`
	MaxConnections     int           `mapstructure:"max_connections"`
	MinConnections     int           `mapstructure:"min_connections"`
	MaxIdleConnections int           `mapstructure:"max_idle_connections"`
	ConnMaxLifetime    time.Duration `mapstructure:"connection_max_lifetime"`
}

type RedisConfig struct {
	Mode         string        `mapstructure:"mode"`
	Addresses    []string      `mapstructure:"addresses"`
	Password     string        `mapstructure:"password"`
	PoolSize     int           `mapstructure:"pool_size"`
	MinIdleConns int           `mapstructure:"min_idle_conns"`
	DialTimeout  time.Duration `mapstructure:"dial_timeout"`
	ReadTimeout  time.Duration `mapstructure:"read_timeout"`
	WriteTimeout time.Duration `mapstructure:"write_timeout"`
}

type PythonProcessorConfig struct {
	Pool struct {
		MinSize             int           `mapstructure:"min_size"`
		MaxSize             int           `mapstructure:"max_size"`
		HealthCheckInterval time.Duration `mapstructure:"health_check_interval"`
		RequestTimeout      time.Duration `mapstructure:"request_timeout"`
	} `mapstructure:"pool"`
	ServiceDiscovery string   `mapstructure:"service_discovery"`
	ConsulAddress    string   `mapstructure:"consul_address"`
	StaticAddresses  []string `mapstructure:"static_addresses"`
}

type AuthConfig struct {
	JWTSecret            string        `mapstructure:"jwt_secret"`
	TokenDuration        time.Duration `mapstructure:"token_duration"`
	RefreshTokenDuration time.Duration `mapstructure:"refresh_token_duration"`
}

type WebSocketConfig struct {
	MaxConnectionsPerServer int           `mapstructure:"max_connections_per_server"`
	ReadBufferSize          int           `mapstructure:"read_buffer_size"`
	WriteBufferSize         int           `mapstructure:"write_buffer_size"`
	ReadTimeout             time.Duration `mapstructure:"read_timeout"`
	WriteTimeout            time.Duration `mapstructure:"write_timeout"`
	PingInterval            time.Duration `mapstructure:"ping_interval"`
	PongTimeout             time.Duration `mapstructure:"pong_timeout"`
	MaxMessageSize          int64         `mapstructure:"max_message_size"`
}

type LoadBalancerConfig struct {
	Type                string        `mapstructure:"type"`
	StickySessions      bool          `mapstructure:"sticky_sessions"`
	HealthCheckPath     string        `mapstructure:"health_check_path"`
	HealthCheckInterval time.Duration `mapstructure:"health_check_interval"`
}

type QueueConfig struct {
	FrameProcessing struct {
		Name           string        `mapstructure:"name"`
		MaxSize        int           `mapstructure:"max_size"`
		PriorityLevels int           `mapstructure:"priority_levels"`
		RetryCount     int           `mapstructure:"retry_count"`
		RetryDelay     time.Duration `mapstructure:"retry_delay"`
	} `mapstructure:"frame_processing"`
	StatsUpdate struct {
		Name    string `mapstructure:"name"`
		MaxSize int    `mapstructure:"max_size"`
		Workers int    `mapstructure:"workers"`
	} `mapstructure:"stats_update"`
}

type CacheConfig struct {
	ExerciseStateTTL time.Duration `mapstructure:"exercise_state_ttl"`
	UserSessionTTL   time.Duration `mapstructure:"user_session_ttl"`
	StatsTTL         time.Duration `mapstructure:"stats_ttl"`
	LeaderboardTTL   time.Duration `mapstructure:"leaderboard_ttl"`
}

type MonitoringConfig struct {
	Enabled        bool   `mapstructure:"enabled"`
	PrometheusPort int    `mapstructure:"prometheus_port"`
	MetricsPath    string `mapstructure:"metrics_path"`
	HealthCheck    struct {
		Path         string `mapstructure:"path"`
		DetailedPath string `mapstructure:"detailed_path"`
	} `mapstructure:"health_check"`
	Tracing struct {
		Enabled       bool    `mapstructure:"enabled"`
		Provider      string  `mapstructure:"provider"`
		JaegerAddress string  `mapstructure:"jaeger_address"`
		SampleRate    float64 `mapstructure:"sample_rate"`
	} `mapstructure:"tracing"`
}

type LoggingConfig struct {
	Level            string   `mapstructure:"level"`
	Format           string   `mapstructure:"format"`
	OutputPaths      []string `mapstructure:"output_paths"`
	ErrorOutputPaths []string `mapstructure:"error_output_paths"`
}

type RateLimitingConfig struct {
	Enabled           bool    `mapstructure:"enabled"`
	RequestsPerSecond float64 `mapstructure:"requests_per_second"`
	Burst             int     `mapstructure:"burst"`
	Type              string  `mapstructure:"type"`
}

type SecurityConfig struct {
	CORSAllowedOrigins []string `mapstructure:"cors_allowed_origins"`
	CORSAllowedMethods []string `mapstructure:"cors_allowed_methods"`
	EnableHTTPS        bool     `mapstructure:"enable_https"`
	SSLCertPath        string   `mapstructure:"ssl_cert_path"`
	SSLKeyPath         string   `mapstructure:"ssl_key_path"`
}

func LoadConfig() (*Config, error) {
	viper.SetConfigName("config")
	viper.SetConfigType("yaml")
	viper.AddConfigPath("./configs")
	viper.AddConfigPath("/etc/lfk/")
	viper.AddConfigPath(".") // Добавляем текущую директорию для разработки

	// Поддержка переменных окружения (опционально, но теперь необязательно)
	viper.AutomaticEnv()
	viper.SetEnvPrefix("LFK")
	viper.SetEnvKeyReplacer(strings.NewReplacer(".", "_"))

	if err := viper.ReadInConfig(); err != nil {
		// Если файл не найден, используем значения по умолчанию
		if _, ok := err.(viper.ConfigFileNotFoundError); ok {
			fmt.Println("No config file found, using defaults")
			return getDefaultConfig(), nil
		}
		return nil, fmt.Errorf("failed to read config: %w", err)
	}

	var config Config
	if err := viper.Unmarshal(&config); err != nil {
		return nil, fmt.Errorf("failed to unmarshal config: %w", err)
	}

	// Устанавливаем значения по умолчанию, если они не заданы
	if config.Server.HTTPPort == 0 {
		config.Server.HTTPPort = 9000
	}
	if config.WebSocket.PingInterval == 0 {
		config.WebSocket.PingInterval = 30 * time.Second
	}
	if config.Cache.ExerciseStateTTL == 0 {
		config.Cache.ExerciseStateTTL = 5 * time.Second
	}
	if config.Auth.JWTSecret == "" {
		config.Auth.JWTSecret = "default-dev-secret-key-change-in-production"
	}

	return &config, nil
}

// getDefaultConfig возвращает конфигурацию по умолчанию на случай отсутствия файла
func getDefaultConfig() *Config {
	return &Config{
		Server: ServerConfig{
			Name:            "lfk-backend",
			Environment:     "development",
			HTTPPort:        8080,
			GRPCPort:        50051,
			ShutdownTimeout: 30 * time.Second,
			Instances: struct {
				Min int    `mapstructure:"min"`
				Max int    `mapstructure:"max"`
				ID  string `mapstructure:"id"`
			}{
				Min: 1,
				Max: 10,
				ID:  "lfk-server-1",
			},
		},
		Database: DatabaseConfig{
			Postgres: struct {
				Master   PostgresInstance   `mapstructure:"master"`
				Replicas []PostgresInstance `mapstructure:"replicas"`
			}{
				Master: PostgresInstance{
					Host:               "localhost",
					Port:               5432,
					User:               "postgres",
					Password:           "123456",
					Database:           "lfkdb",
					SSLMode:            "disable",
					MaxConnections:     100,
					MinConnections:     10,
					MaxIdleConnections: 10,
					ConnMaxLifetime:    1 * time.Hour,
				},
				Replicas: []PostgresInstance{},
			},
		},
		Redis: RedisConfig{
			Mode:         "standalone",
			Addresses:    []string{"localhost:6379"},
			Password:     "",
			PoolSize:     100,
			MinIdleConns: 10,
			DialTimeout:  5 * time.Second,
			ReadTimeout:  3 * time.Second,
			WriteTimeout: 3 * time.Second,
		},
		PythonProcessor: PythonProcessorConfig{
			Pool: struct {
				MinSize             int           `mapstructure:"min_size"`
				MaxSize             int           `mapstructure:"max_size"`
				HealthCheckInterval time.Duration `mapstructure:"health_check_interval"`
				RequestTimeout      time.Duration `mapstructure:"request_timeout"`
			}{
				MinSize:             1,
				MaxSize:             5,
				HealthCheckInterval: 10 * time.Second,
				RequestTimeout:      5 * time.Second,
			},
			ServiceDiscovery: "static",
			ConsulAddress:    "consul:8500",
			StaticAddresses:  []string{"localhost:5001"},
		},
		Auth: AuthConfig{
			JWTSecret:            "your-super-secret-jwt-key-change-in-production-2024",
			TokenDuration:        24 * time.Hour,
			RefreshTokenDuration: 720 * time.Hour,
		},
		WebSocket: WebSocketConfig{
			MaxConnectionsPerServer: 500,
			ReadBufferSize:          1048576,
			WriteBufferSize:         1048576,
			ReadTimeout:             60 * time.Second,
			WriteTimeout:            60 * time.Second,
			PingInterval:            30 * time.Second,
			PongTimeout:             60 * time.Second,
			MaxMessageSize:          10485760,
		},
		// ... остальные поля с значениями по умолчанию
	}
}
