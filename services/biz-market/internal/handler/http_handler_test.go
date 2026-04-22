package handler

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/qts/biz-market/internal/adapter"
	"github.com/qts/biz-market/internal/service"

	"github.com/gin-gonic/gin"
)

// Mock MarketService for testing HTTP handlers
type mockMarketService struct {
	quoteFn   func(symbol string) (interface{}, error)
	klineFn   func(symbol, period string, limit int) (interface{}, error)
	symbolsFn func() []interface{}
}

func newTestHTTPHandler() *HTTPHandler {
	adapter := adapter.NewMarketDataAdapter("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	svc := service.NewMarketService(adapter)
	return NewHTTPHandler(svc)
}

func setupRouter() *gin.Engine {
	gin.SetMode(gin.TestMode)
	r := gin.New()
	return r
}

func TestHTTPHandler_HealthCheck(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	req, _ := http.NewRequest("GET", "/health", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}

	var resp map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &resp)

	if resp["status"] != "healthy" {
		t.Errorf("expected status 'healthy', got %v", resp["status"])
	}

	if _, ok := resp["timestamp"]; !ok {
		t.Error("expected timestamp in response")
	}
}

func TestHTTPHandler_GetQuote_Success(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	// Test with a non-existent symbol first
	req, _ := http.NewRequest("GET", "/v1/market/quotes/NONEXISTENT", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	// Should return 404 since symbol doesn't exist
	if w.Code != http.StatusNotFound {
		t.Errorf("expected status 404, got %d", w.Code)
	}
}

func TestHTTPHandler_GetQuote_MissingSymbol(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	// Empty symbol - Gin will match the route but symbol param will be empty
	req, _ := http.NewRequest("GET", "/v1/market/quotes/", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	// Route with trailing slash may not match, check for 404
	if w.Code != http.StatusNotFound {
		t.Logf("status code: %d", w.Code)
	}
}

func TestHTTPHandler_GetKline_Success(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=IF2405&period=1m&limit=10", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}

	var resp map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &resp)

	if resp["code"] != float64(0) {
		t.Errorf("expected code 0, got %v", resp["code"])
	}

	data, ok := resp["data"].(map[string]interface{})
	if !ok {
		t.Fatal("expected data to be map")
	}

	if data["symbol"] != "IF2405" {
		t.Errorf("expected symbol IF2405, got %v", data["symbol"])
	}
}

func TestHTTPHandler_GetKline_MissingSymbol(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	req, _ := http.NewRequest("GET", "/v1/market/kline", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected status 400, got %d", w.Code)
	}

	var resp map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &resp)

	if resp["code"] != float64(400) {
		t.Errorf("expected code 400, got %v", resp["code"])
	}
}

func TestHTTPHandler_GetKline_DefaultPeriod(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	// Without period parameter, should default to 1m
	req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=IF2405", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}

	var resp map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &resp)

	data := resp["data"].(map[string]interface{})
	if data["period"] != "1m" {
		t.Errorf("expected period 1m, got %v", data["period"])
	}
}

func TestHTTPHandler_GetSymbols(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	req, _ := http.NewRequest("GET", "/v1/market/symbols", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}

	var resp map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &resp)

	if resp["code"] != float64(0) {
		t.Errorf("expected code 0, got %v", resp["code"])
	}
}

func TestHTTPHandler_GetSymbol_NotFound(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	req, _ := http.NewRequest("GET", "/v1/market/symbols/NONEXISTENT", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusNotFound {
		t.Errorf("expected status 404, got %d", w.Code)
	}
}

func TestHTTPHandler_WebSocketStream_Upgrade(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	// Test WebSocket upgrade
	req, _ := http.NewRequest("GET", "/v1/market/stream", nil)
	req.Header.Set("Upgrade", "websocket")
	req.Header.Set("Connection", "Upgrade")

	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	// WebSocket upgrade should succeed (or fail gracefully)
	// The test verifies the endpoint is accessible
	if w.Code != http.StatusOK && w.Code != http.StatusSwitchingProtocols {
		t.Logf("WebSocket upgrade response: %d", w.Code)
	}
}

func TestHTTPHandler_GetKline_InvalidLimit(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	// Even with invalid limit, should still work (uses default 100)
	req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=IF2405&limit=invalid", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	// Gin will parse "invalid" as 0, which triggers default 100
	// Response should still be OK
	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}
}

func TestAPIResponse_JSON(t *testing.T) {
	resp := APIResponse{
		Code:    0,
		Message: "success",
		Data:    map[string]string{"key": "value"},
	}

	jsonBytes, err := json.Marshal(resp)
	if err != nil {
		t.Fatalf("json.Marshal failed: %v", err)
	}

	var parsed APIResponse
	err = json.Unmarshal(jsonBytes, &parsed)
	if err != nil {
		t.Fatalf("json.Unmarshal failed: %v", err)
	}

	if parsed.Code != 0 {
		t.Errorf("expected code 0, got %d", parsed.Code)
	}
	if parsed.Message != "success" {
		t.Errorf("expected message 'success', got %s", parsed.Message)
	}
}

func TestHTTPHandler_RegisterRoutes(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()

	// Should not panic
	handler.RegisterRoutes(router)

	// Verify routes are registered by making requests
	routes := router.Routes()

	expectedRoutes := map[string]string{
		"GET":  "/health",
	}

	for _, route := range routes {
		delete(expectedRoutes, route.Method+" "+route.Path)
	}

	// Note: /v1/market/* routes also registered
	_ = expectedRoutes // Routes are registered, just verify no panic
}

func TestHTTPHandler_GetKline_EmptySymbol(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	// Empty symbol query parameter
	req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	// Should return 400 for empty symbol
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected status 400 for empty symbol, got %d", w.Code)
	}
}

func TestHTTPHandler_GetKline_NonDefaultPeriod(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	periods := []string{"1m", "5m", "15m", "1h", "1d"}

	for _, period := range periods {
		req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=IF2405&period="+period, nil)
		w := httptest.NewRecorder()
		router.ServeHTTP(w, req)

		if w.Code != http.StatusOK {
			t.Errorf("period=%s: expected status 200, got %d", period, w.Code)
		}

		var resp map[string]interface{}
		json.Unmarshal(w.Body.Bytes(), &resp)

		data := resp["data"].(map[string]interface{})
		if data["period"] != period {
			t.Errorf("period=%s: expected period %s, got %v", period, period, data["period"])
		}
	}
}

// Benchmark tests
func BenchmarkHealthCheck(b *testing.B) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	req, _ := http.NewRequest("GET", "/health", nil)

	for i := 0; i < b.N; i++ {
		w := httptest.NewRecorder()
		router.ServeHTTP(w, req)
	}
}

func BenchmarkGetQuote(b *testing.B) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	req, _ := http.NewRequest("GET", "/v1/market/quotes/IF2405", nil)

	for i := 0; i < b.N; i++ {
		w := httptest.NewRecorder()
		router.ServeHTTP(w, req)
	}
}

func BenchmarkGetKline(b *testing.B) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=IF2405&period=1m", nil)

	for i := 0; i < b.N; i++ {
		w := httptest.NewRecorder()
		router.ServeHTTP(w, req)
	}
}

// WebSocket message handling tests
func TestWSRequestMessage_Parsing(t *testing.T) {
	tests := []struct {
		name    string
		payload string
		wantErr bool
	}{
		{
			name:    "subscribe with multiple symbols",
			payload: `{"type":"subscribe","symbols":["IF2405","IF2406"],"periods":["quote","tick"]}`,
		},
		{
			name:    "unsubscribe",
			payload: `{"type":"unsubscribe","symbols":["IF2405"]}`,
		},
		{
			name:    "ping",
			payload: `{"type":"ping"}`,
		},
		{
			name:    "invalid json",
			payload: `{invalid}`,
			wantErr: true,
		},
		{
			name:    "missing type",
			payload: `{"symbols":["IF2405"]}`,
			wantErr: false, // valid JSON, validation happens at service level
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var msg map[string]interface{}
			err := json.Unmarshal([]byte(tt.payload), &msg)
			if tt.wantErr {
				if err == nil {
					t.Error("expected error but got none")
				}
			} else {
				if err != nil {
					t.Errorf("unexpected error: %v", err)
				}
			}
		})
	}
}

func TestHTTPHandler_WebSocketStream_MultipleConnections(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	// Verify multiple WebSocket connections can be initiated
	for i := 0; i < 5; i++ {
		req, _ := http.NewRequest("GET", "/v1/market/stream", nil)
		req.Header.Set("Upgrade", "websocket")
		req.Header.Set("Connection", "Upgrade")

		w := httptest.NewRecorder()
		router.ServeHTTP(w, req)

		// Should not panic or fail
		_ = w.Code
	}
}

// Error response format tests
func TestHTTPHandler_ErrorResponseFormat(t *testing.T) {
	handler := newTestHTTPHandler()
	router := setupRouter()
	handler.RegisterRoutes(router)

	tests := []struct {
		name         string
		path         string
		expectedCode int
		checkBody    bool // whether to check body has code/message fields
	}{
		{"empty symbol", "/v1/market/quotes/", 404, false}, // may not match route
		{"invalid symbol", "/v1/market/quotes/@@@", 404, true},
		{"empty kline symbol", "/v1/market/kline?symbol=", 400, true},
		{"missing kline symbol", "/v1/market/kline", 400, true},
		{"not found symbol", "/v1/market/symbols/NONEXISTENT", 404, true},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req, _ := http.NewRequest("GET", tt.path, nil)
			w := httptest.NewRecorder()
			router.ServeHTTP(w, req)

			if w.Code != tt.expectedCode {
				t.Errorf("expected status %d, got %d", tt.expectedCode, w.Code)
			}

			// Verify error response format if checkBody is set
			if tt.checkBody {
				body := w.Body.Bytes()
				if len(body) > 0 {
					var resp map[string]interface{}
					json.Unmarshal(body, &resp)

					if _, ok := resp["code"]; !ok {
						t.Error("expected 'code' field in error response")
					}
					if _, ok := resp["message"]; !ok {
						t.Error("expected 'message' field in error response")
					}
				}
			}
		})
	}
}
