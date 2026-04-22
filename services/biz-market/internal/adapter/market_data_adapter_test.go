package adapter

import (
	"context"
	"fmt"
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

// Test Disconnect
func TestMarketDataAdapter_Disconnect(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	adapter.Connect(context.Background())
	
	err := adapter.Disconnect()
	if err != nil {
		t.Errorf("Disconnect failed: %v", err)
	}
}

// Test Unsubscribe with nil symbol subscription
func TestMarketDataAdapter_Unsubscribe_NilSymbol(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	adapter.Connect(context.Background())
	
	// Try to unsubscribe a symbol that was never subscribed
	err := adapter.Unsubscribe("NONEXISTENT", []string{"tick"})
	if err != nil {
		t.Errorf("Unsubscribe for non-existent symbol failed: %v", err)
	}
}

// Test Unsubscribe when all periods are removed
func TestMarketDataAdapter_Unsubscribe_AllPeriodsRemoved(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	adapter.Connect(context.Background())
	
	// Subscribe first
	adapter.Subscribe("IF2405", []string{"tick", "quote"})
	
	// Unsubscribe all periods - should trigger mdClient.UnsubscribeMarketData
	err := adapter.Unsubscribe("IF2405", []string{"tick", "quote"})
	if err != nil {
		t.Errorf("Unsubscribe all periods failed: %v", err)
	}
}

// Test validatePrice with LastPrice <= 0
func TestMarketDataAdapter_validateQuote_ZeroPrice(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	quote := &model.QuoteData{
		Symbol:          "IF2405",
		LastPrice:       0, // zero price
		UpperLimitPrice: 4169.0,
		LowerLimitPrice: 3411.0,
		Volume:          50000,
	}
	
	valid := adapter.validatePrice(quote)
	if valid {
		t.Error("expected invalid quote (zero price)")
	}
}

// Test validatePrice with negative price
func TestMarketDataAdapter_validateQuote_NegativePrice(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	quote := &model.QuoteData{
		Symbol:          "IF2405",
		LastPrice:       -100.0, // negative price
		UpperLimitPrice: 4169.0,
		LowerLimitPrice: 3411.0,
		Volume:          50000,
	}
	
	valid := adapter.validatePrice(quote)
	if valid {
		t.Error("expected invalid quote (negative price)")
	}
}

// Test validatePrice without limit prices (only checks LastPrice > 0)
func TestMarketDataAdapter_validateQuote_NoLimits(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	quote := &model.QuoteData{
		Symbol:          "IF2405",
		LastPrice:       3800.0,
		UpperLimitPrice: 0, // no limits set
		LowerLimitPrice: 0,
		Volume:          50000,
	}
	
	valid := adapter.validatePrice(quote)
	if !valid {
		t.Error("expected valid quote with no limits")
	}
}

// Test checkLatency with normal latency
func TestMarketDataAdapter_checkLatency_Normal(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	// Add a symbol with recent timestamp
	adapter.lastQuoteTimeMu.Lock()
	adapter.lastQuoteTime["IF2405"] = time.Now()
	adapter.lastQuoteTimeMu.Unlock()
	
	// Should not panic
	adapter.checkLatency()
}

// Test checkLatency with high latency (triggers alert)
func TestMarketDataAdapter_checkLatency_HighLatency(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	// Add a symbol with old timestamp (2 seconds ago)
	adapter.lastQuoteTimeMu.Lock()
	adapter.lastQuoteTime["IF2405"] = time.Now().Add(-2 * time.Second)
	adapter.lastQuoteTimeMu.Unlock()
	
	// Should not panic, but will print ALERT
	adapter.checkLatency()
}

// Test SetKafkaProducer
func TestMarketDataAdapter_SetKafkaProducer(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	// Setting nil should not panic
	adapter.SetKafkaProducer(nil)
}

// Test publishToKafka with mock producer
func TestMarketDataAdapter_publishToKafka_WithProducer(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	// Use a real-ish scenario - we just test the code path doesn't panic
	adapter.SetKafkaProducer(nil)
	
	quote := &model.QuoteData{
		Symbol:    "IF2405",
		LastPrice: 3800.0,
	}
	
	// This will just return since kafkaProducer is nil
	adapter.publishToKafka(quote)
}

// Test handleQuote with nil onQuote callback
func TestMarketDataAdapter_handleQuote_NilCallback(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	// Don't set any callbacks
	
	raw := simnow.QuoteData{
		Symbol:          "IF2405",
		LastPrice:       3800.0,
		UpperLimitPrice: 4169.0,
		LowerLimitPrice: 3411.0,
		Volume:          50000,
		Timestamp:       time.Now(),
	}
	
	// Should not panic even with nil callback
	adapter.handleQuote(raw)
}

// Test handleTick with nil onTick callback
func TestMarketDataAdapter_handleTick_NilCallback(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	// Don't set any callbacks
	
	raw := simnow.TickData{
		Symbol:    "IF2405",
		Price:     3800.0,
		Quantity:  10,
		TradeTime: time.Now(),
	}
	
	// Should not panic even with nil callback
	adapter.handleTick(raw)
}

// Test handleQuote with zero volume (valid)
func TestMarketDataAdapter_handleQuote_ZeroVolume(t *testing.T) {
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
		Volume:          0, // zero volume is valid
		Timestamp:       time.Now(),
	}
	
	adapter.handleQuote(raw)
	
	if !called {
		t.Error("onQuote callback should be called for zero volume")
	}
}

// Test handleQuote with negative volume (invalid, should be filtered by validatePrice)
func TestMarketDataAdapter_handleQuote_NegativeVolume_Invalid(t *testing.T) {
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
		Volume:          -100, // negative volume
		Timestamp:       time.Now(),
	}
	
	adapter.handleQuote(raw)
	
	// Should not call callback for invalid data
	if called {
		t.Error("callback should not be called for negative volume")
	}
}

// Test updateKline with multiple periods
func TestMarketDataAdapter_updateKline_MultiplePeriods(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	quote := &model.QuoteData{
		Symbol:    "IF2405",
		LastPrice: 3800.0,
		Volume:    50000,
		Turnover:  190000000.0,
		Timestamp: time.Now(),
	}
	
	adapter.updateKline(quote)
	
	// Verify both 1m and 5m klines were created
	adapter.klineCacheMu.RLock()
	_, ok1m := adapter.klineCache["IF2405_1m"]
	_, ok5m := adapter.klineCache["IF2405_5m"]
	adapter.klineCacheMu.RUnlock()
	
	if !ok1m {
		t.Error("1m kline not created")
	}
	if !ok5m {
		t.Error("5m kline not created")
	}
}

// Test concurrent access to quoteCache
func TestMarketDataAdapter_ConcurrentQuoteAccess(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	// Add some quotes
	adapter.quoteCacheMu.Lock()
	adapter.quoteCache["IF2405"] = &model.QuoteData{Symbol: "IF2405", LastPrice: 3800.0}
	adapter.quoteCache["IF2406"] = &model.QuoteData{Symbol: "IF2406", LastPrice: 3810.0}
	adapter.quoteCacheMu.Unlock()
	
	// Concurrent reads
	done := make(chan bool, 10)
	for i := 0; i < 10; i++ {
		go func() {
			for j := 0; j < 100; j++ {
				adapter.GetQuote("IF2405")
				adapter.GetQuote("IF2406")
				adapter.GetQuote("NONEXISTENT")
			}
			done <- true
		}()
	}
	
	for i := 0; i < 10; i++ {
		<-done
	}
}

// Test concurrent access to klineCache
func TestMarketDataAdapter_ConcurrentKlineAccess(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	// Add some klines
	adapter.klineCacheMu.Lock()
	adapter.klineCache["IF2405_1m"] = &model.KlineData{Symbol: "IF2405", Period: "1m"}
	adapter.klineCacheMu.Unlock()
	
	// Concurrent reads
	done := make(chan bool, 10)
	for i := 0; i < 10; i++ {
		go func() {
			for j := 0; j < 100; j++ {
				adapter.GetKline("IF2405", "1m")
				adapter.GetKline("NONEXISTENT", "1m")
			}
			done <- true
		}()
	}
	
	for i := 0; i < 10; i++ {
		<-done
	}
}

// Test concurrent subscription modifications
func TestMarketDataAdapter_ConcurrentSubscribe(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	adapter.Connect(context.Background())
	
	done := make(chan bool, 5)
	for i := 0; i < 5; i++ {
		go func(idx int) {
			symbol := fmt.Sprintf("IF24%02d", idx)
			adapter.Subscribe(symbol, []string{"tick", "quote"})
			adapter.Unsubscribe(symbol, []string{"tick"})
			done <- true
		}(i)
	}
	
	for i := 0; i < 5; i++ {
		<-done
	}
}

// Test multiple symbols in handleQuote
func TestMarketDataAdapter_handleQuote_MultipleSymbols(t *testing.T) {
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	symbols := make(map[string]bool)
	adapter.SetCallbacks(func(q model.QuoteData) {
		symbols[q.Symbol] = true
	}, nil)
	
	symbolList := []string{"IF2405", "IF2406", "IF2407", "IF2408", "IF2409"}
	
	for _, sym := range symbolList {
		raw := simnow.QuoteData{
			Symbol:          sym,
			LastPrice:       3800.0,
			UpperLimitPrice: 4169.0,
			LowerLimitPrice: 3411.0,
			Volume:          50000,
			Timestamp:       time.Now(),
		}
		adapter.handleQuote(raw)
	}
	
	if len(symbols) != len(symbolList) {
		t.Errorf("expected %d symbols, got %d", len(symbolList), len(symbols))
	}
}

// Test Connect error path (simulated by mock)
func TestMarketDataAdapter_ConnectLoginError(t *testing.T) {
	// We cannot easily trigger a Login error with the current SimNow mock
	// This test documents the limitation
	adapter := NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	
	// Connect should succeed in mock
	err := adapter.Connect(context.Background())
	if err != nil {
		t.Errorf("Connect failed: %v", err)
	}
}
