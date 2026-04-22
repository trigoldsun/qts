package model

import (
	"encoding/json"
	"testing"
	"time"
)

func TestTickData_JSON(t *testing.T) {
	now := time.Now()
	tick := TickData{
		Symbol:    "IF2405",
		TradeTime: now,
		Price:     3800.0,
		Quantity:  10,
		Turnover:  38000.0,
		BSFlag:    "BUY",
		OrderKind: "limit",
		OrderID:   "ORDER123",
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
	
	if parsed.Symbol != "IF2405" {
		t.Errorf("Symbol = %s, want IF2405", parsed.Symbol)
	}
	if parsed.Price != 3800.0 {
		t.Errorf("Price = %f, want 3800.0", parsed.Price)
	}
}

func TestQuoteData_JSON(t *testing.T) {
	now := time.Now()
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
		BidPrice2:       3798.0,
		BidPrice3:       3797.0,
		BidPrice4:       3796.0,
		BidPrice5:       3795.0,
		AskPrice1:       3801.0,
		AskPrice2:       3802.0,
		AskPrice3:       3803.0,
		AskPrice4:       3804.0,
		AskPrice5:       3805.0,
		BidVolume1:      10,
		BidVolume2:      8,
		BidVolume3:      6,
		BidVolume4:      4,
		BidVolume5:      2,
		AskVolume1:      12,
		AskVolume2:      9,
		AskVolume3:      7,
		AskVolume4:      5,
		AskVolume5:      3,
		Timestamp:       now,
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
	
	if parsed.Symbol != "IF2405" {
		t.Errorf("Symbol = %s, want IF2405", parsed.Symbol)
	}
	if parsed.BidPrice1 != 3799.0 {
		t.Errorf("BidPrice1 = %f, want 3799.0", parsed.BidPrice1)
	}
	if parsed.AskVolume1 != 12 {
		t.Errorf("AskVolume1 = %d, want 12", parsed.AskVolume1)
	}
}

func TestKlineData_JSON(t *testing.T) {
	now := time.Now()
	kline := KlineData{
		Symbol:    "IF2405",
		Period:    "1m",
		Open:      3795.0,
		High:      3810.0,
		Low:       3785.0,
		Close:     3800.0,
		Volume:    50000,
		Amount:    190000000.0,
		Timestamp: now,
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
	
	if parsed.Symbol != "IF2405" {
		t.Errorf("Symbol = %s, want IF2405", parsed.Symbol)
	}
	if parsed.Period != "1m" {
		t.Errorf("Period = %s, want 1m", parsed.Period)
	}
	if parsed.Volume != 50000 {
		t.Errorf("Volume = %d, want 50000", parsed.Volume)
	}
}

func TestSymbolInfo_JSON(t *testing.T) {
	info := SymbolInfo{
		Symbol:      "IF2405",
		Name:        "沪深300期货2405",
		Exchange:    "CFFEX",
		ProductType: "期货",
		Multiplier:  300.0,
		PriceTick:   0.2,
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
	
	if parsed.Symbol != "IF2405" {
		t.Errorf("Symbol = %s, want IF2405", parsed.Symbol)
	}
	if parsed.Exchange != "CFFEX" {
		t.Errorf("Exchange = %s, want CFFEX", parsed.Exchange)
	}
	if parsed.Multiplier != 300.0 {
		t.Errorf("Multiplier = %f, want 300.0", parsed.Multiplier)
	}
}

func TestSubscribeRequest_JSON(t *testing.T) {
	req := SubscribeRequest{
		Symbols: []string{"IF2405", "IF2406"},
		Periods: []string{"tick", "quote", "1m"},
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
	
	if len(parsed.Symbols) != 2 {
		t.Errorf("Symbols length = %d, want 2", len(parsed.Symbols))
	}
	if parsed.Periods[0] != "tick" {
		t.Errorf("Periods[0] = %s, want tick", parsed.Periods[0])
	}
}

func TestWebSocketMessage_JSON(t *testing.T) {
	now := time.Now()
	msg := WebSocketMessage{
		Type:   "quote",
		Symbol: "IF2405",
		Data: map[string]interface{}{
			"price": 3800.0,
		},
		Time: now,
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
	if parsed.Symbol != "IF2405" {
		t.Errorf("Symbol = %s, want IF2405", parsed.Symbol)
	}
}

func TestQuoteData_StructFields(t *testing.T) {
	quote := QuoteData{
		Symbol:   "IF2405",
		Volume:   50000,
		Turnover: 190000000.0,
	}
	
	if quote.Symbol != "IF2405" {
		t.Error("Symbol not set")
	}
	if quote.Volume != 50000 {
		t.Error("Volume not set")
	}
	if quote.Turnover != 190000000.0 {
		t.Error("Turnover not set")
	}
}

func TestKlineData_Periods(t *testing.T) {
	periods := []string{"1m", "5m", "15m", "1h", "1d"}
	
	for _, period := range periods {
		kline := KlineData{
			Symbol: "IF2405",
			Period: period,
		}
		if kline.Period != period {
			t.Errorf("Period = %s, want %s", kline.Period, period)
		}
	}
}
