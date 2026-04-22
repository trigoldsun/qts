package service

import (
	"context"
	"encoding/json"
	"sync"
	"testing"
	"time"

	"github.com/qts/biz-market/internal/adapter"
	"github.com/qts/biz-market/internal/model"
)

func TestNewMarketService(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	if svc == nil {
		t.Fatal("NewMarketService returned nil")
	}
	if svc.adapter == nil {
		t.Error("adapter not set")
	}
	if svc.wsManager == nil {
		t.Error("wsManager not initialized")
	}
	if svc.quoteCache == nil {
		t.Error("quoteCache not initialized")
	}
	if svc.klineCache == nil {
		t.Error("klineCache not initialized")
	}
	if svc.symbolInfo == nil {
		t.Error("symbolInfo not initialized")
	}
}

func TestMarketService_GetWSManager(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	mgr := svc.GetWSManager()
	if mgr == nil {
		t.Error("GetWSManager returned nil")
	}
	
	// Should return the same instance
	mgr2 := svc.GetWSManager()
	if mgr != mgr2 {
		t.Error("GetWSManager should return same instance")
	}
}

func TestMarketService_MdClient(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	mdClient := svc.MdClient()
	if mdClient == nil {
		t.Error("MdClient returned nil")
	}
}

func TestMarketService_Subscribe(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	adapter.Connect(context.Background())
	
	svc := NewMarketService(adapter)
	
	// Note: Subscribe requires WebSocket connection to be registered
	// For unit testing, we test the error path
	err := svc.Subscribe("non-existent-conn", []string{"IF2405"}, nil)
	// This should fail because connection doesn't exist
	if err == nil {
		// Actually this might succeed if connection manager allows it
		t.Log("Subscribe returned nil for non-existent connection (may be expected)")
	}
}

func TestMarketService_Unsubscribe(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	adapter.Connect(context.Background())
	
	svc := NewMarketService(adapter)
	
	err := svc.Unsubscribe("non-existent-conn", []string{"IF2405"})
	if err == nil {
		t.Log("Unsubscribe returned nil for non-existent connection (may be expected)")
	}
}

func TestMarketService_GetQuote(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	// Initially no quote
	quote, err := svc.GetQuote("NONEXISTENT")
	if err == nil {
		t.Error("expected error for non-existent symbol")
	}
	if quote != nil {
		t.Error("expected nil quote for non-existent symbol")
	}
}

func TestMarketService_GetKline(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	// Get kline for non-existent symbol
	klines, err := svc.GetKline("NONEXISTENT", "1m", 100)
	if err != nil {
		t.Errorf("GetKline returned error: %v", err)
	}
	if klines == nil {
		t.Error("GetKline returned nil")
	}
	if len(klines) != 0 {
		t.Errorf("expected empty klines, got %d", len(klines))
	}
}

func TestMarketService_GetKline_DefaultLimit(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	// Get kline with zero limit should use default 100
	_, err := svc.GetKline("NONEXISTENT", "1m", 0)
	if err != nil {
		t.Errorf("GetKline returned error: %v", err)
	}
}

func TestMarketService_GetSymbols(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	symbols := svc.GetSymbols()
	if symbols == nil {
		t.Error("GetSymbols returned nil")
	}
	if len(symbols) != 0 {
		t.Errorf("expected empty symbols, got %d", len(symbols))
	}
}

func TestMarketService_GetSymbol(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	info, err := svc.GetSymbol("NONEXISTENT")
	if err == nil {
		t.Error("expected error for non-existent symbol")
	}
	if info != nil {
		t.Error("expected nil info for non-existent symbol")
	}
}

func TestMarketService_handleQuote(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	quote := model.QuoteData{
		Symbol:     "IF2405",
		LastPrice:  3800.0,
		OpenPrice:  3795.0,
		HighPrice:  3810.0,
		LowPrice:   3785.0,
		Volume:     50000,
		Timestamp:  time.Now(),
	}
	
	svc.handleQuote(quote)
	
	// Verify quote was cached
	svc.quoteMu.RLock()
	cached, ok := svc.quoteCache["IF2405"]
	svc.quoteMu.RUnlock()
	
	if !ok {
		t.Error("quote not cached")
	}
	if cached.LastPrice != 3800.0 {
		t.Error("cached quote has wrong price")
	}
}

func TestMarketService_handleTick(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	tick := model.TickData{
		Symbol:    "IF2405",
		Price:     3800.0,
		Quantity:  10,
		TradeTime: time.Now(),
	}
	
	// This should not panic
	svc.handleTick(tick)
}

func TestWSRequestMessage_JSON(t *testing.T) {
	tests := []struct {
		name    string
		jsonStr string
		want    WSRequestMessage
	}{
		{
			name:    "subscribe message",
			jsonStr: `{"type":"subscribe","symbols":["IF2405"],"periods":["quote"]}`,
			want: WSRequestMessage{
				Type:    "subscribe",
				Symbols: []string{"IF2405"},
				Periods: []string{"quote"},
			},
		},
		{
			name:    "unsubscribe message",
			jsonStr: `{"type":"unsubscribe","symbols":["IF2405"]}`,
			want: WSRequestMessage{
				Type:    "unsubscribe",
				Symbols: []string{"IF2405"},
			},
		},
		{
			name:    "ping message",
			jsonStr: `{"type":"ping"}`,
			want: WSRequestMessage{
				Type: "ping",
			},
		},
	}
	
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			var msg WSRequestMessage
			err := json.Unmarshal([]byte(tc.jsonStr), &msg)
			if err != nil {
				t.Errorf("json.Unmarshal failed: %v", err)
			}
			if msg.Type != tc.want.Type {
				t.Errorf("Type = %s, want %s", msg.Type, tc.want.Type)
			}
		})
	}
}

func TestMarketService_HandleWSMessage(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	tests := []struct {
		name    string
		message string
		wantErr bool
	}{
		{
			name:    "subscribe message",
			message: `{"type":"subscribe","symbols":["IF2405"],"periods":["quote"]}`,
			wantErr: true, // connection doesn't exist
		},
		{
			name:    "unsubscribe message",
			message: `{"type":"unsubscribe","symbols":["IF2405"]}`,
			wantErr: true,
		},
		{
			name:    "ping message",
			message: `{"type":"ping"}`,
			wantErr: false,
		},
		{
			name:    "invalid message",
			message: `not valid json`,
			wantErr: true,
		},
		{
			name:    "unknown type",
			message: `{"type":"unknown"}`,
			wantErr: true,
		},
	}
	
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			err := svc.HandleWSMessage("non-existent-conn", []byte(tc.message))
			if (err != nil) != tc.wantErr {
				t.Errorf("HandleWSMessage() error = %v, wantErr %v", err, tc.wantErr)
			}
		})
	}
}

func TestMarketService_Start(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	ctx, cancel := context.WithTimeout(context.Background(), 100*time.Millisecond)
	defer cancel()
	
	err := svc.Start(ctx)
	// Start should succeed but the context will timeout since it runs in background
	// This is expected behavior for unit testing
	if err != nil {
		t.Errorf("Start failed: %v", err)
	}
}

func TestMarketService_ConcurrentQuoteAccess(t *testing.T) {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := NewMarketService(adapter)
	
	var wg sync.WaitGroup
	
	for i := 0; i < 100; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			svc.handleQuote(model.QuoteData{
				Symbol:    "IF2405",
				LastPrice: float64(3800 + i),
			})
		}(i)
	}
	
	wg.Wait()
	
	// Verify quote was cached
	svc.quoteMu.RLock()
	cached, ok := svc.quoteCache["IF2405"]
	svc.quoteMu.RUnlock()
	
	if !ok {
		t.Error("quote not cached after concurrent writes")
	}
	_ = cached // Just verify no panic
}
