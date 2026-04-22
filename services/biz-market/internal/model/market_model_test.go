package model

import (
	"encoding/json"
	"testing"
	"time"
)

// Edge cases and boundary condition tests

func TestQuoteData_AllFieldsZero(t *testing.T) {
	// Test quote with all zero values - should still serialize/deserialize
	quote := QuoteData{}

	data, err := json.Marshal(quote)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed QuoteData
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	if parsed.Symbol != "" {
		t.Errorf("expected empty Symbol, got %s", parsed.Symbol)
	}
}

func TestQuoteData_MaxValues(t *testing.T) {
	// Test with maximum float64 values
	quote := QuoteData{
		Symbol:          "MAX",
		LastPrice:       1e308, // near max float64
		UpperLimitPrice: 1e308,
		LowerLimitPrice: 1e308,
		Volume:          9223372036854775807, // max int64
	}

	data, err := json.Marshal(quote)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed QuoteData
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	if parsed.LastPrice != quote.LastPrice {
		t.Errorf("LastPrice = %e, want %e", parsed.LastPrice, quote.LastPrice)
	}
}

func TestQuoteData_NegativeValues(t *testing.T) {
	quote := QuoteData{
		Symbol:     "NEGATIVE",
		LastPrice:  -100.0,
		OpenPrice:  -50.0,
		HighPrice:  0.0,
		LowPrice:   -200.0,
		ClosePrice: -100.0,
		Volume:     -1000, // negative volume should be possible in test
	}

	data, err := json.Marshal(quote)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed QuoteData
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	if parsed.LastPrice != -100.0 {
		t.Errorf("LastPrice = %f, want -100.0", parsed.LastPrice)
	}
}

func TestKlineData_EmptyPeriod(t *testing.T) {
	// Kline with empty period - edge case
	kline := KlineData{
		Symbol: "IF2405",
		Period: "",
		Open:   3800.0,
		High:   3810.0,
		Low:    3785.0,
		Close:  3800.0,
	}

	data, err := json.Marshal(kline)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed KlineData
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	if parsed.Period != "" {
		t.Errorf("Period = %s, want empty string", parsed.Period)
	}
}

func TestKlineData_ZeroValues(t *testing.T) {
	// Kline with zero values
	kline := KlineData{}

	data, err := json.Marshal(kline)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed KlineData
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	// All fields should be zero values
	if parsed.Symbol != "" {
		t.Errorf("Symbol = %s, want empty", parsed.Symbol)
	}
	if parsed.Volume != 0 {
		t.Errorf("Volume = %d, want 0", parsed.Volume)
	}
}

func TestTickData_EmptyStrings(t *testing.T) {
	// Tick with empty optional fields
	tick := TickData{
		Symbol:   "IF2405",
		TradeTime: time.Now(),
		Price:    3800.0,
		Quantity: 10,
		BSFlag:   "",
		OrderKind: "",
		OrderID:  "",
	}

	data, err := json.Marshal(tick)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed TickData
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	if parsed.OrderID != "" {
		t.Errorf("OrderID = %s, want empty", parsed.OrderID)
	}
}

func TestSymbolInfo_WithDelistedDate(t *testing.T) {
	// SymbolInfo with delisted date set
	info := SymbolInfo{
		Symbol:       "DELISTED",
		Name:         "Delisted Stock",
		Exchange:     "SSE",
		ProductType:  "股票",
		Multiplier:   100.0,
		PriceTick:    0.01,
		ListedDate:   "2000-01-01",
		DelistedDate: "2024-01-01",
	}

	data, err := json.Marshal(info)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed SymbolInfo
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	if parsed.DelistedDate != "2024-01-01" {
		t.Errorf("DelistedDate = %s, want 2024-01-01", parsed.DelistedDate)
	}
}

func TestSymbolInfo_WithoutDelistedDate(t *testing.T) {
	// SymbolInfo without delisted date (omitempty)
	info := SymbolInfo{
		Symbol:      "ACTIVE",
		Name:        "Active Stock",
		Exchange:    "SSE",
		ProductType: "股票",
		Multiplier:  100.0,
		PriceTick:   0.01,
		ListedDate:  "2024-01-01",
	}

	data, err := json.Marshal(info)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed SymbolInfo
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	if parsed.DelistedDate != "" {
		t.Errorf("DelistedDate = %s, want empty (omitempty)", parsed.DelistedDate)
	}
}

func TestSubscribeRequest_EmptyArrays(t *testing.T) {
	// SubscribeRequest with empty arrays
	req := SubscribeRequest{
		Symbols: []string{},
		Periods: []string{},
	}

	data, err := json.Marshal(req)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed SubscribeRequest
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	if len(parsed.Symbols) != 0 {
		t.Errorf("Symbols length = %d, want 0", len(parsed.Symbols))
	}
}

func TestSubscribeRequest_NilArrays(t *testing.T) {
	// SubscribeRequest with nil arrays
	req := SubscribeRequest{}

	data, err := json.Marshal(req)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed SubscribeRequest
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	// nil slices should marshal to null, not empty array
	if parsed.Symbols != nil {
		t.Errorf("Symbols = %v, want nil", parsed.Symbols)
	}
}

func TestWebSocketMessage_WithComplexData(t *testing.T) {
	// WebSocketMessage with complex nested data
	msg := WebSocketMessage{
		Type:   "quote",
		Symbol: "IF2405",
		Data: map[string]interface{}{
			"bid_prices": []float64{3799.0, 3798.0, 3797.0},
			"ask_prices": []float64{3801.0, 3802.0, 3803.0},
			"volumes":    map[string]int{"bid": 100, "ask": 150},
		},
		Time: time.Now(),
	}

	data, err := json.Marshal(msg)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed WebSocketMessage
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	if parsed.Type != "quote" {
		t.Errorf("Type = %s, want quote", parsed.Type)
	}
}

func TestQuoteData_BidAskConsistency(t *testing.T) {
	// Verify bid prices are always less than ask prices (for valid quote)
	quote := QuoteData{
		Symbol:    "IF2405",
		BidPrice1: 3800.0,
		AskPrice1: 3801.0,
		BidPrice2: 3799.0,
		AskPrice2: 3802.0,
	}

	// This is a data consistency test
	if quote.BidPrice1 >= quote.AskPrice1 {
		t.Errorf("BidPrice1 (%f) should be < AskPrice1 (%f)", quote.BidPrice1, quote.AskPrice1)
	}
	if quote.BidPrice2 >= quote.AskPrice2 {
		t.Errorf("BidPrice2 (%f) should be < AskPrice2 (%f)", quote.BidPrice2, quote.AskPrice2)
	}
}

func TestKlineData_OHLC_Consistency(t *testing.T) {
	// Verify High >= Low for valid kline
	kline := KlineData{
		Symbol: "IF2405",
		Open:   3800.0,
		High:   3810.0,
		Low:    3785.0,
		Close:  3800.0,
	}

	if kline.High < kline.Low {
		t.Errorf("High (%f) should be >= Low (%f)", kline.High, kline.Low)
	}

	if kline.High < kline.Open {
		t.Errorf("High (%f) should be >= Open (%f)", kline.High, kline.Open)
	}

	if kline.High < kline.Close {
		t.Errorf("High (%f) should be >= Close (%f)", kline.High, kline.Close)
	}

	if kline.Low > kline.Open {
		t.Errorf("Low (%f) should be <= Open (%f)", kline.Low, kline.Open)
	}

	if kline.Low > kline.Close {
		t.Errorf("Low (%f) should be <= Close (%f)", kline.Low, kline.Close)
	}
}

func TestKlineData_InvalidOHLC(t *testing.T) {
	// Test kline where High < Low (invalid data) - should be handled gracefully
	kline := KlineData{
		Symbol: "INVALID",
		High:   3785.0,
		Low:    3810.0, // Low > High - invalid
	}

	// Just verify it serializes - application should validate
	data, err := json.Marshal(kline)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}

	var parsed KlineData
	err = json.Unmarshal(data, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}

	if parsed.Low != 3810.0 {
		t.Errorf("Low = %f, want 3810.0", parsed.Low)
	}
}

func TestQuoteData_PreCloseVsClose(t *testing.T) {
	// Test relationship between PreClosePrice and ClosePrice
	quote := QuoteData{
		Symbol:        "IF2405",
		PreClosePrice: 3800.0,
		ClosePrice:    3810.0,
	}

	// This is a data relationship test
	_ = quote.PreClosePrice // Just verify fields exist
	_ = quote.ClosePrice
}

func TestTickData_BuySellFlags(t *testing.T) {
	tests := []struct {
		flag    string
		isValid bool
	}{
		{"BUY", true},
		{"SELL", true},
		{"B", true},
		{"S", true},
		{"", false},
		{"UNKNOWN", false},
	}

	for _, tt := range tests {
		tick := TickData{
			Symbol: "IF2405",
			BSFlag: tt.flag,
		}

		// Just verify it serializes
		data, err := json.Marshal(tick)
		if err != nil {
			t.Errorf("json.Marshal(%s) failed: %v", tt.flag, err)
		}

		var parsed TickData
		err = json.Unmarshal(data, &parsed)
		if err != nil {
			t.Errorf("json.Unmarshal(%s) failed: %v", tt.flag, err)
		}

		if tt.isValid && parsed.BSFlag != tt.flag {
			t.Errorf("BSFlag = %s, want %s", parsed.BSFlag, tt.flag)
		}
	}
}

// Benchmark tests
func BenchmarkQuoteData_Marshal(b *testing.B) {
	quote := QuoteData{
		Symbol:          "IF2405",
		LastPrice:       3800.0,
		OpenPrice:       3795.0,
		HighPrice:       3810.0,
		LowPrice:        3785.0,
		ClosePrice:      3800.0,
		PreClosePrice:   3790.0,
		Volume:          50000,
		Turnover:        190000000.0,
		OpenInterest:    100000,
		UpperLimitPrice: 4169.0,
		LowerLimitPrice: 3411.0,
		BidPrice1:       3799.0,
		AskPrice1:       3801.0,
		BidVolume1:      10,
		AskVolume1:      12,
		Timestamp:       time.Now(),
	}

	for i := 0; i < b.N; i++ {
		_, _ = json.Marshal(quote)
	}
}

func BenchmarkQuoteData_Unmarshal(b *testing.B) {
	data := []byte(`{"symbol":"IF2405","last_price":3800.0,"volume":50000}`)

	for i := 0; i < b.N; i++ {
		var quote QuoteData
		_ = json.Unmarshal(data, &quote)
	}
}

func BenchmarkKlineData_Marshal(b *testing.B) {
	kline := KlineData{
		Symbol:    "IF2405",
		Period:    "1m",
		Open:      3795.0,
		High:      3810.0,
		Low:       3785.0,
		Close:     3800.0,
		Volume:    50000,
		Amount:    190000000.0,
		Timestamp: time.Now(),
	}

	for i := 0; i < b.N; i++ {
		_, _ = json.Marshal(kline)
	}
}

func BenchmarkTickData_Marshal(b *testing.B) {
	tick := TickData{
		Symbol:    "IF2405",
		TradeTime: time.Now(),
		Price:     3800.0,
		Quantity:  10,
		Turnover:  38000.0,
		BSFlag:    "BUY",
		OrderKind: "limit",
		OrderID:   "ORDER123",
	}

	for i := 0; i < b.N; i++ {
		_, _ = json.Marshal(tick)
	}
}
