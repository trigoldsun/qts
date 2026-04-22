package config

import (
	"os"
	"gopkg.in/yaml.v3"
)

type Config struct {
	Server   ServerConfig   `yaml:"server"`
	SimNow   SimNowConfig   `yaml:"simnow"`
	Redis    RedisConfig    `yaml:"redis"`
	Kafka    KafkaConfig    `yaml:"kafka"`
}

type ServerConfig struct {
	HTTPPort int `yaml:"http_port"`
	WSPort   int `yaml:"ws_port"`
}

type SimNowConfig struct {
	FrontAddr string `yaml:"front_addr"`
	BrokerID  string `yaml:"broker_id"`
	UserID    string `yaml:"user_id"`
	Password  string `yaml:"password"`
}

type RedisConfig struct {
	Host     string `yaml:"host"`
	Port     int    `yaml:"port"`
	Password string `yaml:"password"`
	DB       int    `yaml:"db"`
}

type KafkaConfig struct {
	Brokers []string `yaml:"brokers"`
	Topic   string   `yaml:"topic"`
}

func Load() *Config {
	cfg := &Config{
		Server: ServerConfig{
			HTTPPort: 8086,
			WSPort:   8087,
		},
		SimNow: SimNowConfig{
			FrontAddr: "tcp://127.0.0.1:41205",
			BrokerID:  "9999",
			UserID:    "",
			Password:  "",
		},
		Redis: RedisConfig{
			Host: "localhost",
			Port: 6379,
			DB:   0,
		},
		Kafka: KafkaConfig{
			Brokers: []string{"localhost:9092"},
			Topic:   "qts.market.quotes",
		},
	}

	// 从配置文件加载（如果存在）
	if configFile := os.Getenv("CONFIG_FILE"); configFile != "" {
		if data, err := os.ReadFile(configFile); err == nil {
			yaml.Unmarshal(data, cfg)
		}
	}

	return cfg
}
