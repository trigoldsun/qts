package handler

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/qts/biz-market/internal/model"
	"github.com/qts/biz-market/internal/websocket"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// MockMarketService is a mock implementation for testing
type MockMarketService struct {
	quotes    map[string]*model.QuoteData
	klines    map[string][]*model.KlineData
	symbols   map[string]*model.SymbolInfo
	wsManager *websocket.ConnectionManager
}

func NewMockMarketService() *MockMarketService {
	return &MockMarketService{
		quotes:  make(map[string]*model.QuoteData),
		klines:  make(map[string][]*model.KlineData),
		symbols: make(map[string]*model.SymbolInfo),
	}
}

func (m *MockMarketService) GetQuote(symbol string) (*model.QuoteData, error) {
	if quote, ok := m.quotes[symbol]; ok {
		return quote, nil
	}
	return nil, errors.New("quote not found for symbol: " + symbol)
}

func (m *MockMarketService) GetKline(symbol, period string, limit int) ([]*model.KlineData, error) {
	if limit <= 0 {
		limit = 100
	}
	key := symbol + "_" + period
	if klines, ok := m.klines[key]; ok {
		if len(klines) <= limit {
			return klines, nil
		}
		return klines[len(klines)-limit:], nil
	}
	return []*model.KlineData{}, nil
}

func (m *MockMarketService) GetSymbols() []*model.SymbolInfo {
	result := make([]*model.SymbolInfo, 0, len(m.symbols))
	for _, info := range m.symbols {
		result = append(result, info)
	}
	return result
}

func (m *MockMarketService) GetSymbol(symbol string) (*model.SymbolInfo, error) {
	if info, ok := m.symbols[symbol]; ok {
		return info, nil
	}
	return nil, errors.New("symbol not found: " + symbol)
}

func (m *MockMarketService) GetWSManager() *websocket.ConnectionManager {
	if m.wsManager == nil {
		m.wsManager = websocket.NewConnectionManager()
	}
	return m.wsManager
}

func (m *MockMarketService) HandleWSMessage(connID string, message []byte) error {
	return nil
}

func (m *MockMarketService) AddQuote(quote *model.QuoteData) {
	m.quotes[quote.Symbol] = quote
}

func (m *MockMarketService) AddKline(symbol, period string, klines []*model.KlineData) {
	key := symbol + "_" + period
	m.klines[key] = klines
}

func (m *MockMarketService) AddSymbol(info *model.SymbolInfo) {
	m.symbols[info.Symbol] = info
}

// Ensure MockMarketService implements MarketServiceInterface
var _ MarketServiceInterface = (*MockMarketService)(nil)

// Tests for HealthCheck
func TestHealthCheck(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	handler := NewHTTPHandler(NewMockMarketService())
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/health", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, "healthy", resp["status"])
	assert.NotNil(t, resp["timestamp"])
}

// Tests for GetQuote
func TestGetQuote_Success(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	mockSvc.AddQuote(&model.QuoteData{
		Symbol:    "BTC-USDT",
		LastPrice: 50000.0,
		OpenPrice: 49000.0,
		HighPrice: 51000.0,
		LowPrice:  48500.0,
		Volume:    12345,
	})

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/quotes/BTC-USDT", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(0), resp["code"])
	assert.Equal(t, "success", resp["message"])

	data := resp["data"].(map[string]interface{})
	assert.Equal(t, "BTC-USDT", data["symbol"])
	assert.Equal(t, 50000.0, data["last_price"])
}

func TestGetQuote_NotFound(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	// Don't add any quotes

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/quotes/UNKNOWN", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusNotFound, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(404), resp["code"])
	assert.Contains(t, resp["message"], "quote not found")
}

// Tests for GetKline
func TestGetKline_Success(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	mockSvc.AddKline("BTC-USDT", "1m", []*model.KlineData{
		{Symbol: "BTC-USDT", Period: "1m", Open: 50000.0, High: 50100.0, Low: 49900.0, Close: 50050.0, Volume: 123},
		{Symbol: "BTC-USDT", Period: "1m", Open: 50050.0, High: 50200.0, Low: 50000.0, Close: 50100.0, Volume: 234},
	})

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=BTC-USDT&period=1m", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(0), resp["code"])

	data := resp["data"].(map[string]interface{})
	assert.Equal(t, "BTC-USDT", data["symbol"])
	assert.Equal(t, "1m", data["period"])
	assert.Equal(t, float64(2), data["count"])
}

func TestGetKline_MissingSymbol(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	handler := NewHTTPHandler(NewMockMarketService())
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/kline", nil) // no symbol query param
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(400), resp["code"])
	assert.Equal(t, "symbol is required", resp["message"])
}

func TestGetKline_EmptyResult(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	// No klines added

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=UNKNOWN&period=1m", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(0), resp["code"])

	data := resp["data"].(map[string]interface{})
	assert.Equal(t, float64(0), data["count"])
}

func TestGetKline_DefaultPeriod(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	mockSvc.AddKline("BTC-USDT", "1m", []*model.KlineData{
		{Symbol: "BTC-USDT", Period: "1m", Open: 50000.0, High: 50100.0, Low: 49900.0, Close: 50050.0, Volume: 123},
	})

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=BTC-USDT", nil) // no period, should default to 1m
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	data := resp["data"].(map[string]interface{})
	assert.Equal(t, "1m", data["period"]) // default period
}

func TestGetKline_HardcodedLimit(t *testing.T) {
	// This test verifies that the handler uses hardcoded limit=100
	// The handler doesn't parse limit query parameter
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	klines := make([]*model.KlineData, 0, 150)
	for i := 0; i < 150; i++ {
		klines = append(klines, &model.KlineData{
			Symbol: "BTC-USDT",
			Period: "1m",
			Open:   50000.0 + float64(i),
			High:   50100.0 + float64(i),
			Low:    49900.0 + float64(i),
			Close:  50050.0 + float64(i),
			Volume: int64(i),
		})
	}
	mockSvc.AddKline("BTC-USDT", "1m", klines)

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	// Request without limit - handler hardcodes limit=100
	req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=BTC-USDT&period=1m", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)

	data := resp["data"].(map[string]interface{})
	// Handler hardcodes limit=100
	assert.Equal(t, float64(100), data["count"])
}

// Tests for GetSymbols
func TestGetSymbols_Success(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	mockSvc.AddSymbol(&model.SymbolInfo{Symbol: "BTC-USDT", Name: "Bitcoin", Exchange: "BINANCE", ProductType: "SPOT"})
	mockSvc.AddSymbol(&model.SymbolInfo{Symbol: "ETH-USDT", Name: "Ethereum", Exchange: "BINANCE", ProductType: "SPOT"})

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/symbols", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(0), resp["code"])

	data := resp["data"].(map[string]interface{})
	assert.Equal(t, float64(2), data["count"])
}

func TestGetSymbols_Empty(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	handler := NewHTTPHandler(NewMockMarketService())
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/symbols", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(0), resp["code"])

	data := resp["data"].(map[string]interface{})
	assert.Equal(t, float64(0), data["count"])
}

// Tests for GetSymbol
func TestGetSymbol_Success(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	mockSvc.AddSymbol(&model.SymbolInfo{
		Symbol:      "BTC-USDT",
		Name:        "Bitcoin",
		Exchange:    "BINANCE",
		ProductType: "SPOT",
		Multiplier:  1.0,
		PriceTick:   0.01,
	})

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/symbols/BTC-USDT", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(0), resp["code"])

	data := resp["data"].(map[string]interface{})
	assert.Equal(t, "BTC-USDT", data["symbol"])
	assert.Equal(t, "Bitcoin", data["name"])
	assert.Equal(t, "BINANCE", data["exchange"])
}

func TestGetSymbol_NotFound(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	// Don't add any symbols

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/symbols/UNKNOWN", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusNotFound, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(404), resp["code"])
	assert.Contains(t, resp["message"], "symbol not found")
}

// Tests for route 404
func TestRouteNotFound(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	handler := NewHTTPHandler(NewMockMarketService())
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/invalid", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusNotFound, w.Code)
}

// Tests for JSON serialization of models
func TestQuoteData_JSON(t *testing.T) {
	quote := model.QuoteData{
		Symbol:          "ETH-USDT",
		LastPrice:       3000.0,
		OpenPrice:       2950.0,
		HighPrice:       3050.0,
		LowPrice:        2900.0,
		Volume:          98765,
		UpperLimitPrice: 3245.0,
		LowerLimitPrice: 2655.0,
	}

	data, err := json.Marshal(quote)
	require.NoError(t, err)

	var decoded model.QuoteData
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)

	assert.Equal(t, "ETH-USDT", decoded.Symbol)
	assert.Equal(t, 3000.0, decoded.LastPrice)
	assert.Equal(t, int64(98765), decoded.Volume)
}

func TestKlineData_JSON(t *testing.T) {
	kline := model.KlineData{
		Symbol: "BTC-USDT",
		Period: "5m",
		Open:   50000.0,
		High:   50500.0,
		Low:    49800.0,
		Close:  50200.0,
		Volume: 1234,
		Amount: 61788000.0,
	}

	data, err := json.Marshal(kline)
	require.NoError(t, err)

	var decoded model.KlineData
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)

	assert.Equal(t, "BTC-USDT", decoded.Symbol)
	assert.Equal(t, "5m", decoded.Period)
	assert.Equal(t, 50200.0, decoded.Close)
}

func TestSymbolInfo_ProductType(t *testing.T) {
	info := model.SymbolInfo{
		Symbol:      "BTC-USDT",
		Name:        "Bitcoin",
		Exchange:    "BINANCE",
		ProductType: "SPOT",
		Multiplier:  1.0,
		PriceTick:   0.01,
	}

	data, err := json.Marshal(info)
	require.NoError(t, err)

	var decoded model.SymbolInfo
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)

	assert.Equal(t, "SPOT", decoded.ProductType)
	assert.Equal(t, 1.0, decoded.Multiplier)
}

func TestWebSocketMessage_JSON(t *testing.T) {
	msg := model.WebSocketMessage{
		Type:   "quote",
		Symbol: "BTC-USDT",
		Data: model.QuoteData{
			Symbol:    "BTC-USDT",
			LastPrice: 50000.0,
		},
		Time: time.Now(),
	}

	data, err := json.Marshal(msg)
	require.NoError(t, err)

	var decoded model.WebSocketMessage
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)

	assert.Equal(t, "quote", decoded.Type)
	assert.Equal(t, "BTC-USDT", decoded.Symbol)
}

// Test context cancellation
func TestGetQuote_ContextCancellation(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	mockSvc.AddQuote(&model.QuoteData{
		Symbol:    "BTC-USDT",
		LastPrice: 50000.0,
	})

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	// Create request with context
	ctx, cancel := context.WithCancel(context.Background())
	req, _ := http.NewRequestWithContext(ctx, "GET", "/v1/market/quotes/BTC-USDT", nil)
	cancel() // Cancel immediately

	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	// Request should still complete since handler doesn't use context
	assert.Equal(t, http.StatusOK, w.Code)
}

// Test APIResponse structure
func TestAPIResponse_Structure(t *testing.T) {
	resp := APIResponse{
		Code:    0,
		Message: "success",
		Data:    map[string]string{"key": "value"},
	}

	data, err := json.Marshal(resp)
	require.NoError(t, err)

	var decoded APIResponse
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)

	assert.Equal(t, 0, decoded.Code)
	assert.Equal(t, "success", decoded.Message)
	assert.NotNil(t, decoded.Data)
}

// Test WebSocket upgrade failure with missing upgrade header
func TestWebSocketStream_InvalidRequest(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	// Request without proper WebSocket upgrade headers - should fail with 400
	req, _ := http.NewRequest("GET", "/v1/market/stream", nil)
	// Missing Upgrade header
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	// Should fail WebSocket upgrade due to missing Upgrade header
	assert.Equal(t, http.StatusBadRequest, w.Code)
}

// Test TickData JSON serialization
func TestTickData_JSON(t *testing.T) {
	tick := model.TickData{
		Symbol:    "BTC-USDT",
		TradeTime: time.Now(),
		Price:     50000.0,
		Quantity:  100,
		Turnover:  5000000.0,
		BSFlag:    "B",
		OrderKind: "limit",
		OrderID:   "ORDER123",
	}

	data, err := json.Marshal(tick)
	require.NoError(t, err)

	var decoded model.TickData
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)

	assert.Equal(t, "BTC-USDT", decoded.Symbol)
	assert.Equal(t, 50000.0, decoded.Price)
	assert.Equal(t, 100, decoded.Quantity)
	assert.Equal(t, "B", decoded.BSFlag)
}

// Test SubscribeRequest JSON serialization
func TestSubscribeRequest_JSON(t *testing.T) {
	req := model.SubscribeRequest{
		Symbols: []string{"BTC-USDT", "ETH-USDT"},
		Periods: []string{"tick", "quote", "1m"},
	}

	data, err := json.Marshal(req)
	require.NoError(t, err)

	var decoded model.SubscribeRequest
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)

	assert.Len(t, decoded.Symbols, 2)
	assert.Equal(t, "BTC-USDT", decoded.Symbols[0])
	assert.Len(t, decoded.Periods, 3)
}

// Test handler with GetKline service error
func TestGetKline_ServiceError(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	// Create a mock that returns an error
	mockSvc := &MockMarketServiceWithError{
		MockMarketService: *NewMockMarketService(),
		klineError: errors.New("database connection failed"),
	}

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=BTC-USDT&period=1m", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusInternalServerError, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(500), resp["code"])
}

// MockMarketServiceWithError wraps MockMarketService with configurable errors
type MockMarketServiceWithError struct {
	MockMarketService
	klineError error
}

func (m *MockMarketServiceWithError) GetKline(symbol, period string, limit int) ([]*model.KlineData, error) {
	if m.klineError != nil {
		return nil, m.klineError
	}
	return m.MockMarketService.GetKline(symbol, period, limit)
}

// Test multiple symbols in GetSymbols
func TestGetSymbols_MultipleProducts(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	mockSvc := NewMockMarketService()
	mockSvc.AddSymbol(&model.SymbolInfo{Symbol: "BTC-USDT", Name: "Bitcoin", Exchange: "BINANCE", ProductType: "SPOT"})
	mockSvc.AddSymbol(&model.SymbolInfo{Symbol: "ETH-USDT", Name: "Ethereum", Exchange: "BINANCE", ProductType: "SPOT"})
	mockSvc.AddSymbol(&model.SymbolInfo{Symbol: "IF2405", Name: "CSI 300 Index Future", Exchange: "CFFEX", ProductType: "FUTURE"})

	handler := NewHTTPHandler(mockSvc)
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/v1/market/symbols", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)

	data := resp["data"].(map[string]interface{})
	assert.Equal(t, float64(3), data["count"])
}

// Test QuoteData with all fields
func TestQuoteData_AllFields(t *testing.T) {
	quote := model.QuoteData{
		Symbol:          "IF2405",
		LastPrice:       3500.0,
		OpenPrice:       3480.0,
		HighPrice:       3510.0,
		LowPrice:        3470.0,
		ClosePrice:      3500.0,
		PreClosePrice:   3480.0,
		Volume:          100000,
		Turnover:        350000000.0,
		OpenInterest:    50000,
		UpperLimitPrice: 3800.0,
		LowerLimitPrice: 3200.0,
		BidPrice1:       3499.5,
		BidPrice2:       3499.0,
		BidPrice3:       3498.5,
		BidPrice4:       3498.0,
		BidPrice5:       3497.5,
		AskPrice1:       3500.5,
		AskPrice2:       3501.0,
		AskPrice3:       3501.5,
		AskPrice4:       3502.0,
		AskPrice5:       3502.5,
		BidVolume1:      100,
		BidVolume2:      200,
		BidVolume3:      300,
		BidVolume4:      400,
		BidVolume5:      500,
		AskVolume1:      150,
		AskVolume2:      250,
		AskVolume3:      350,
		AskVolume4:      450,
		AskVolume5:      550,
		Timestamp:       time.Now(),
	}

	data, err := json.Marshal(quote)
	require.NoError(t, err)

	var decoded model.QuoteData
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)

	assert.Equal(t, "IF2405", decoded.Symbol)
	assert.Equal(t, 3500.0, decoded.LastPrice)
	assert.Equal(t, int64(100000), decoded.Volume)
	assert.Equal(t, 3500.5, decoded.AskPrice1)
	assert.Equal(t, 150, decoded.AskVolume1)
	assert.Equal(t, 3800.0, decoded.UpperLimitPrice)
}

// Test KlineData with different periods
func TestKlineData_Periods(t *testing.T) {
	periods := []string{"1m", "5m", "15m", "30m", "1h", "4h", "1d", "1w"}

	for _, period := range periods {
		kline := model.KlineData{
			Symbol: "BTC-USDT",
			Period: period,
			Open:   50000.0,
			High:   50500.0,
			Low:    49800.0,
			Close:  50200.0,
			Volume: 1234,
			Amount: 61788000.0,
		}

		data, err := json.Marshal(kline)
		require.NoError(t, err)

		var decoded model.KlineData
		err = json.Unmarshal(data, &decoded)
		require.NoError(t, err)

		assert.Equal(t, period, decoded.Period)
	}
}

// Test SymbolInfo with all fields
func TestSymbolInfo_AllFields(t *testing.T) {
	info := model.SymbolInfo{
		Symbol:       "IF2405",
		Name:         "CSI 300 Index Future",
		Exchange:     "CFFEX",
		ProductType:  "FUTURE",
		Multiplier:   300.0,
		PriceTick:    0.2,
		ListedDate:   "2024-01-15",
		DelistedDate: "2024-05-20",
	}

	data, err := json.Marshal(info)
	require.NoError(t, err)

	var decoded model.SymbolInfo
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)

	assert.Equal(t, "IF2405", decoded.Symbol)
	assert.Equal(t, "CSI 300 Index Future", decoded.Name)
	assert.Equal(t, "CFFEX", decoded.Exchange)
	assert.Equal(t, "FUTURE", decoded.ProductType)
	assert.Equal(t, 300.0, decoded.Multiplier)
	assert.Equal(t, 0.2, decoded.PriceTick)
	assert.Equal(t, "2024-01-15", decoded.ListedDate)
	assert.Equal(t, "2024-05-20", decoded.DelistedDate)
}

// Test QuoteData serialization roundtrip
func TestQuoteData_Roundtrip(t *testing.T) {
	quote := &model.QuoteData{
		Symbol:         "ETH-USDT",
		LastPrice:      3000.0,
		OpenPrice:      2950.0,
		HighPrice:      3050.0,
		LowPrice:       2900.0,
		ClosePrice:     3000.0,
		PreClosePrice:  2950.0,
		Volume:         98765,
		Turnover:       296295000.0,
		OpenInterest:   123456,
		UpperLimitPrice: 3245.0,
		LowerLimitPrice: 2655.0,
		BidPrice1:      2999.5,
		AskPrice1:      3000.5,
		BidVolume1:     100,
		AskVolume1:     200,
	}

	// Marshal
	data, err := json.Marshal(quote)
	require.NoError(t, err)

	// Unmarshal to map
	var decodedMap map[string]interface{}
	err = json.Unmarshal(data, &decodedMap)
	require.NoError(t, err)

	assert.Equal(t, "ETH-USDT", decodedMap["symbol"])
	assert.Equal(t, 3000.0, decodedMap["last_price"])

	// Unmarshal to struct
	var decoded model.QuoteData
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)

	assert.Equal(t, quote.Symbol, decoded.Symbol)
	assert.Equal(t, quote.LastPrice, decoded.LastPrice)
}

// Test KlineData with timestamps
func TestKlineData_WithTimestamp(t *testing.T) {
	now := time.Now()
	kline := model.KlineData{
		Symbol:    "BTC-USDT",
		Period:   "1m",
		Open:     50000.0,
		High:     50500.0,
		Low:      49800.0,
		Close:    50200.0,
		Volume:   1234,
		Amount:   61788000.0,
		Timestamp: now,
	}

	data, err := json.Marshal(kline)
	require.NoError(t, err)

	var decoded model.KlineData
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)

	assert.Equal(t, "BTC-USDT", decoded.Symbol)
}

// Test WebSocketMessage with different types
func TestWebSocketMessage_Types(t *testing.T) {
	types := []string{"tick", "quote", "kline", "error"}

	for _, msgType := range types {
		msg := model.WebSocketMessage{
			Type:   msgType,
			Symbol: "BTC-USDT",
			Data:   nil,
			Time:   time.Now(),
		}

		data, err := json.Marshal(msg)
		require.NoError(t, err)

		var decoded model.WebSocketMessage
		err = json.Unmarshal(data, &decoded)
		require.NoError(t, err)

		assert.Equal(t, msgType, decoded.Type)
	}
}

// Test health check response has correct structure
func TestHealthCheck_ResponseStructure(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()

	handler := NewHTTPHandler(NewMockMarketService())
	handler.RegisterRoutes(r)

	req, _ := http.NewRequest("GET", "/health", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)

	// Check that response has required fields
	_, hasStatus := resp["status"]
	_, hasTimestamp := resp["timestamp"]
	assert.True(t, hasStatus, "response should have 'status' field")
	assert.True(t, hasTimestamp, "response should have 'timestamp' field")
}

// Test API response format consistency
func TestAPIResponse_Format(t *testing.T) {
	testCases := []struct {
		name       string
		code       int
		message    string
		hasData    bool
	}{
		{"success", 0, "success", true},
		{"not found", 404, "resource not found", false},
		{"bad request", 400, "invalid parameter", false},
		{"server error", 500, "internal error", false},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			var data interface{}
			if tc.hasData {
				data = map[string]string{"key": "value"}
			}

			resp := APIResponse{
				Code:    tc.code,
				Message: tc.message,
				Data:    data,
			}

			jsonData, err := json.Marshal(resp)
			require.NoError(t, err)

			var decoded APIResponse
			err = json.Unmarshal(jsonData, &decoded)
			require.NoError(t, err)

			assert.Equal(t, tc.code, decoded.Code)
			assert.Equal(t, tc.message, decoded.Message)
			if tc.hasData {
				assert.NotNil(t, decoded.Data)
			}
		})
	}
}

// Ensure imports are used
var _ = os.Getenv
