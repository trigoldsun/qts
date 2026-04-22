package config

import (
	"os"
	"path/filepath"
	"testing"
)

func TestLoad(t *testing.T) {
	cfg := Load()
	
	if cfg == nil {
		t.Fatal("Load returned nil")
	}
	if cfg.Server.HTTPPort == 0 {
		t.Error("Server.HTTPPort not set")
	}
	if cfg.Server.WSPort == 0 {
		t.Error("Server.WSPort not set")
	}
	if cfg.SimNow.FrontAddr == "" {
		t.Error("SimNow.FrontAddr not set")
	}
	if cfg.Redis.Host == "" {
		t.Error("Redis.Host not set")
	}
	if cfg.Kafka.Topic == "" {
		t.Error("Kafka.Topic not set")
	}
}

func TestLoad_WithConfigFile(t *testing.T) {
	// Create a temporary config file
	tmpDir := t.TempDir()
	configPath := filepath.Join(tmpDir, "config.yaml")
	
	configContent := `
server:
  http_port: 9090
  ws_port: 9091
simnow:
  front_addr: "tcp://127.0.0.1:41206"
  broker_id: "8888"
  user_id: "test_user"
  password: "test_pass"
redis:
  host: "redis.example.com"
  port: 6380
  password: "redis_pass"
  db: 1
kafka:
  brokers:
    - "kafka1:9092"
    - "kafka2:9092"
  topic: "test.market.quotes"
`
	err := os.WriteFile(configPath, []byte(configContent), 0644)
	if err != nil {
		t.Fatalf("Failed to write config file: %v", err)
	}
	
	// Set the config file environment variable
	os.Setenv("CONFIG_FILE", configPath)
	defer os.Unsetenv("CONFIG_FILE")
	
	cfg := Load()
	
	if cfg.Server.HTTPPort != 9090 {
		t.Errorf("Server.HTTPPort = %d, want 9090", cfg.Server.HTTPPort)
	}
	if cfg.Server.WSPort != 9091 {
		t.Errorf("Server.WSPort = %d, want 9091", cfg.Server.WSPort)
	}
	if cfg.SimNow.FrontAddr != "tcp://127.0.0.1:41206" {
		t.Errorf("SimNow.FrontAddr = %s, want tcp://127.0.0.1:41206", cfg.SimNow.FrontAddr)
	}
	if cfg.SimNow.BrokerID != "8888" {
		t.Errorf("SimNow.BrokerID = %s, want 8888", cfg.SimNow.BrokerID)
	}
	if cfg.Redis.Host != "redis.example.com" {
		t.Errorf("Redis.Host = %s, want redis.example.com", cfg.Redis.Host)
	}
	if cfg.Redis.Port != 6380 {
		t.Errorf("Redis.Port = %d, want 6380", cfg.Redis.Port)
	}
	if len(cfg.Kafka.Brokers) != 2 {
		t.Errorf("Kafka.Brokers length = %d, want 2", len(cfg.Kafka.Brokers))
	}
}

func TestConfig_Defaults(t *testing.T) {
	cfg := Load()
	
	// Default values from Load()
	if cfg.Server.HTTPPort != 8086 {
		t.Errorf("default HTTPPort = %d, want 8086", cfg.Server.HTTPPort)
	}
	if cfg.Server.WSPort != 8087 {
		t.Errorf("default WSPort = %d, want 8087", cfg.Server.WSPort)
	}
	if cfg.SimNow.FrontAddr != "tcp://127.0.0.1:41205" {
		t.Errorf("default FrontAddr = %s, want tcp://127.0.0.1:41205", cfg.SimNow.FrontAddr)
	}
	if cfg.SimNow.BrokerID != "9999" {
		t.Errorf("default BrokerID = %s, want 9999", cfg.SimNow.BrokerID)
	}
	if cfg.Redis.Port != 6379 {
		t.Errorf("default Redis.Port = %d, want 6379", cfg.Redis.Port)
	}
	if cfg.Redis.DB != 0 {
		t.Errorf("default Redis.DB = %d, want 0", cfg.Redis.DB)
	}
}

func TestServerConfig(t *testing.T) {
	cfg := ServerConfig{
		HTTPPort: 8080,
		WSPort:   8081,
	}
	
	if cfg.HTTPPort != 8080 {
		t.Error("HTTPPort not set correctly")
	}
	if cfg.WSPort != 8081 {
		t.Error("WSPort not set correctly")
	}
}

func TestSimNowConfig(t *testing.T) {
	cfg := SimNowConfig{
		FrontAddr: "tcp://127.0.0.1:41205",
		BrokerID:  "9999",
		UserID:    "user1",
		Password:  "password1",
	}
	
	if cfg.FrontAddr != "tcp://127.0.0.1:41205" {
		t.Error("FrontAddr not set correctly")
	}
	if cfg.BrokerID != "9999" {
		t.Error("BrokerID not set correctly")
	}
}

func TestRedisConfig(t *testing.T) {
	cfg := RedisConfig{
		Host:     "localhost",
		Port:     6379,
		Password: "secret",
		DB:       1,
	}
	
	if cfg.Host != "localhost" {
		t.Error("Host not set correctly")
	}
	if cfg.Port != 6379 {
		t.Error("Port not set correctly")
	}
	if cfg.Password != "secret" {
		t.Error("Password not set correctly")
	}
	if cfg.DB != 1 {
		t.Error("DB not set correctly")
	}
}

func TestKafkaConfig(t *testing.T) {
	cfg := KafkaConfig{
		Brokers: []string{"localhost:9092"},
		Topic:   "qts.market.quotes",
	}
	
	if len(cfg.Brokers) != 1 {
		t.Error("Brokers not set correctly")
	}
	if cfg.Topic != "qts.market.quotes" {
		t.Error("Topic not set correctly")
	}
}
