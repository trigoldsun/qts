package adapter

import (
	"context"
	"testing"
	"time"

	"github.com/qts/biz-market/internal/model"
	"github.com/qts/biz-market/pkg/simnow"
)

func TestNewMarketDataAdapter(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	if adapter == nil {
		t.Fatal("NewMarketDataAdapter returned nil")
	}
	if adapter.mdClient == nil {
		t.Error("mdClient not initialized")
	}
	if adapter.subscriptions == nil {
		t.Error("subscriptions not initialized")
	}
	if adapter.quoteCache == nil {
		t.Error("quoteCache not initialized")
	}
	if adapter.klineCache == nil {
		t.Error("klineCache not initialized")
	}
	if adapter.lastQuoteTime == nil {
		t.Error("lastQuoteTime not initialized")
	}
}

func TestMarketDataAdapter_Connect(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	err := adapter.Connect(context.Background())
	if err != nil {
		t.Errorf("Connect failed: %v", err)
	}
}

func TestMarketDataAdapter_Login(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	_ = adapter.Connect(context.Background())
	// Login is handled internally by Connect
}

func TestMarketDataAdapter_Subscribe(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	adapter.Connect(context.Background())

	symbol := "IF2405"
	periods := []string{"tick", "quote"}

	err := adapter.Subscribe(symbol, periods)
	if err != nil {
		t.Errorf("Subscribe failed: %v", err)
	}

	// Verify subscription
	adapter.subscriptionsMu.RLock()
	if adapter.subscriptions[symbol] == nil {
		t.Errorf("subscription for %s not found", symbol)
	}
	adapter.subscriptionsMu.RUnlock()
}

func TestMarketDataAdapter_Subscribe_DefaultPeriods(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	adapter.Connect(context.Background())

	err := adapter.Subscribe("IF2405", nil)
	if err != nil {
		t.Errorf("Subscribe with nil periods failed: %v", err)
	}
}

func TestMarketDataAdapter_Unsubscribe(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	adapter.Connect(context.Background())

	adapter.Subscribe("IF2405", []string{"tick", "quote"})

	err := adapter.Unsubscribe("IF2405", []string{"tick"})
	if err != nil {
		t.Errorf("Unsubscribe failed: %v", err)
	}
}

func TestMarketDataAdapter_GetQuote(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")

	quote, ok := adapter.GetQuote("NONEXISTENT")
	if ok {
		t.Error("expected not found for non-existent symbol")
	}
	if quote != nil {
		t.Error("expected nil quote for non-existent symbol")
	}
}

func TestMarketDataAdapter_GetKline(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")

	kline, ok := adapter.GetKline("NONEXISTENT", "1m")
	if ok {
		t.Error("expected not found for non-existent kline")
	}
	if kline != nil {
		t.Error("expected nil kline for non-existent")
	}
}

func TestMarketDataAdapter_SetCallbacks(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")

	var calledQuote bool
	var calledTick bool
	adapter.SetCallbacks(func(q model.QuoteData) {
		calledQuote = true
	}, func(t model.TickData) {
		calledTick = true
	})

	if adapter.onQuote == nil {
		t.Error("onQuote callback not set")
	}
	if adapter.onTick == nil {
		t.Error("onTick callback not set")
	}

	// Simulate callbacks
	adapter.onQuote(model.QuoteData{Symbol: "IF2405"})
	adapter.onTick(model.TickData{Symbol: "IF2405"})

	if !calledQuote {
		t.Error("quote callback not called")
	}
	if !calledTick {
		t.Error("tick callback not called")
	}
}

func TestMarketDataAdapter_validateQuote_Valid(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")

	quote := &model.QuoteData{
		Symbol:          "IF2405",
		LastPrice:       3800.0,
		UpperLimitPrice: 4169.0,
		LowerLimitPrice: 3411.0,
		Volume:          50000,
	}

	valid := adapter.validatePrice(quote)
	if !valid {
		t.Error("expected valid quote")
	}
}

func TestMarketDataAdapter_validateQuote_AboveLimit(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")

	quote := &model.QuoteData{
		Symbol:          "IF2405",
		LastPrice:       5000.0, // above upper limit
		UpperLimitPrice: 4169.0,
		LowerLimitPrice: 3411.0,
		Volume:          50000,
	}

	valid := adapter.validatePrice(quote)
	if valid {
		t.Error("expected invalid quote (above limit)")
	}
}

func TestMarketDataAdapter_validateQuote_BelowLimit(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")

	quote := &model.QuoteData{
		Symbol:          "IF2405",
		LastPrice:       3000.0, // below lower limit
		UpperLimitPrice: 4169.0,
		LowerLimitPrice: 3411.0,
		Volume:          50000,
	}

	valid := adapter.validatePrice(quote)
	if valid {
		t.Error("expected invalid quote (below limit)")
	}
}

func TestMarketDataAdapter_validateQuote_NegativeVolume(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")

	quote := &model.QuoteData{
		Symbol:          "IF2405",
		LastPrice:       3800.0,
		UpperLimitPrice: 4169.0,
		LowerLimitPrice: 3411.0,
		Volume:          -1,
	}

	valid := adapter.validatePrice(quote)
	if valid {
		t.Error("expected invalid quote (negative volume)")
	}
}

func TestMarketDataAdapter_updateKline(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	quote := &model.QuoteData{
		Symbol:    "IF2405",
		LastPrice: 3800.0,
		Volume:    50000,
		Turnover:  190000000.0,
		Timestamp: time.Now(),
	}
	
	adapter.updateKline(quote)
	
	// Verify kline was created in cache
	key := "IF2405_1m"
	adapter.klineCacheMu.RLock()
	kline, ok := adapter.klineCache[key]
	adapter.klineCacheMu.RUnlock()
	
	if !ok {
		t.Error("kline not created in cache")
	}
	if kline.Open != 3800.0 {
		t.Errorf("kline.Open = %f, want 3800.0", kline.Open)
	}
}

func TestMarketDataAdapter_updateKline_ExistingKline(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	// First quote
	quote1 := &model.QuoteData{
		Symbol:    "IF2405",
		LastPrice: 3800.0,
		Volume:    50000,
		Turnover:  190000000.0,
		Timestamp: time.Now(),
	}
	adapter.updateKline(quote1)
	
	// Second quote with higher price
	quote2 := &model.QuoteData{
		Symbol:    "IF2405",
		LastPrice: 3810.0,
		Volume:    50010,
		Turnover:  190038000.0,
		Timestamp: quote1.Timestamp.Add(time.Second), // same minute
	}
	adapter.updateKline(quote2)
	
	// Verify kline was updated
	key := "IF2405_1m"
	adapter.klineCacheMu.RLock()
	kline := adapter.klineCache[key]
	adapter.klineCacheMu.RUnlock()
	
	if kline.High != 3810.0 {
		t.Errorf("kline.High = %f, want 3810.0", kline.High)
	}
	if kline.Close != 3810.0 {
		t.Errorf("kline.Close = %f, want 3810.0", kline.Close)
	}
}

func TestMarketDataAdapter_publishToKafka_NoProducer(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	quote := &model.QuoteData{
		Symbol:    "IF2405",
		LastPrice: 3800.0,
	}
	
	// Should not panic even with nil kafka producer
	adapter.publishToKafka(quote)
}

func TestMarketDataAdapter_handleQuote(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	var called bool
	adapter.SetCallbacks(func(q model.QuoteData) {
		called = true
	}, nil)
	
	raw := simnow.QuoteData{
		Symbol:          "IF2405",
		LastPrice:       3800.0,
		UpperLimitPrice: 4169.0,
		LowerLimitPrice: 3411.0,
		Volume:          50000,
		Timestamp:       time.Now(),
	}
	
	adapter.handleQuote(raw)
	
	if !called {
		t.Error("onQuote callback not called")
	}
	
	// Verify quote was cached
	adapter.quoteCacheMu.RLock()
	cached, ok := adapter.quoteCache["IF2405"]
	adapter.quoteCacheMu.RUnlock()
	
	if !ok {
		t.Error("quote not cached")
	}
	if cached.LastPrice != 3800.0 {
		t.Error("cached quote has wrong price")
	}
}

func TestMarketDataAdapter_handleQuote_InvalidData(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	var called bool
	adapter.SetCallbacks(func(q model.QuoteData) {
		called = true
	}, nil)
	
	// Invalid quote (above limit)
	raw := simnow.QuoteData{
		Symbol:          "IF2405",
		LastPrice:       5000.0,
		UpperLimitPrice: 4169.0,
		LowerLimitPrice: 3411.0,
		Volume:          50000,
	}
	
	adapter.handleQuote(raw)
	
	// Invalid quotes should not trigger callback
	if called {
		t.Error("callback should not be called for invalid quote")
	}
}

func TestMarketDataAdapter_handleTick(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	var called bool
	adapter.SetCallbacks(nil, func(t model.TickData) {
		called = true
	})
	
	raw := simnow.TickData{
		Symbol:    "IF2405",
		Price:     3800.0,
		Quantity:  10,
		TradeTime: time.Now(),
	}
	
	adapter.handleTick(raw)
	
	if !called {
		t.Error("onTick callback not called")
	}
}

func TestMarketDataAdapter_handleError(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	// Should not panic
	adapter.handleError(nil)
}

func TestMarketDataAdapter_handleStatus(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	// Should not panic
	adapter.handleStatus(simnow.StatusConnected)
}
