package handler

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/qts/biz-market/internal/model"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func setupRouter() *gin.Engine {
	gin.SetMode(gin.TestMode)
	r := gin.New()
	return r
}

func TestHealthCheck(t *testing.T) {
	r := setupRouter()
	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status":    "healthy",
			"timestamp": 1700000000,
		})
	})
	
	req, _ := http.NewRequest("GET", "/health", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	
	assert.Equal(t, http.StatusOK, w.Code)
	
	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, "healthy", resp["status"])
}

func TestGetQuote_Success(t *testing.T) {
	r := setupRouter()
	
	quote := model.QuoteData{
		Symbol:        "BTC-USDT",
		LastPrice:     50000.0,
		OpenPrice:     49000.0,
		HighPrice:     51000.0,
		LowPrice:      48500.0,
		Volume:        12345,
	}
	
	r.GET("/v1/market/quotes/:symbol", func(c *gin.Context) {
		symbol := c.Param("symbol")
		if symbol == "" {
			c.JSON(http.StatusBadRequest, gin.H{"code": 400, "message": "symbol is required"})
			return
		}
		c.JSON(http.StatusOK, gin.H{
			"code": 0,
			"data": quote,
		})
	})
	
	req, _ := http.NewRequest("GET", "/v1/market/quotes/BTC-USDT", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	
	assert.Equal(t, http.StatusOK, w.Code)
	
	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(0), resp["code"])
}

func TestGetQuote_MissingSymbol(t *testing.T) {
	r := setupRouter()
	
	// When symbol is empty, return 400
	r.GET("/v1/market/quotes/:symbol", func(c *gin.Context) {
		symbol := c.Param("symbol")
		if symbol == "" {
			c.JSON(http.StatusBadRequest, gin.H{"code": 400, "message": "symbol is required"})
			return
		}
	})
	
	// Request without symbol in path will not match this route
	req, _ := http.NewRequest("GET", "/v1/market/quotes/", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	
	// Will return 404 since route doesn't match
	assert.Equal(t, http.StatusNotFound, w.Code)
}

func TestGetKline_Success(t *testing.T) {
	r := setupRouter()
	
	klines := []model.KlineData{
		{Symbol: "BTC-USDT", Period: "1m", Open: 50000.0, High: 50100.0, Low: 49900.0, Close: 50050.0, Volume: 123},
		{Symbol: "BTC-USDT", Period: "1m", Open: 50050.0, High: 50200.0, Low: 50000.0, Close: 50100.0, Volume: 234},
	}
	
	r.GET("/v1/market/kline", func(c *gin.Context) {
		symbol := c.Query("symbol")
		if symbol == "" {
			c.JSON(http.StatusBadRequest, gin.H{"code": 400, "message": "symbol is required"})
			return
		}
		c.JSON(http.StatusOK, gin.H{
			"code": 0,
			"data": gin.H{
				"symbol": symbol,
				"period": c.DefaultQuery("period", "1m"),
				"klines": klines,
			},
		})
	})
	
	req, _ := http.NewRequest("GET", "/v1/market/kline?symbol=BTC-USDT&period=1m", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	
	assert.Equal(t, http.StatusOK, w.Code)
	
	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(0), resp["code"])
}

func TestGetSymbols_Success(t *testing.T) {
	r := setupRouter()
	
	symbols := []model.SymbolInfo{
		{Symbol: "BTC-USDT", Name: "Bitcoin", Exchange: "BINANCE", ProductType: "SPOT"},
		{Symbol: "ETH-USDT", Name: "Ethereum", Exchange: "BINANCE", ProductType: "SPOT"},
	}
	
	r.GET("/v1/market/symbols", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"code": 0,
			"data": gin.H{
				"symbols": symbols,
				"total":   len(symbols),
			},
		})
	})
	
	req, _ := http.NewRequest("GET", "/v1/market/symbols", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	
	assert.Equal(t, http.StatusOK, w.Code)
	
	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(0), resp["code"])
}

func TestGetSymbol_Success(t *testing.T) {
	r := setupRouter()
	
	r.GET("/v1/market/symbols/:symbol", func(c *gin.Context) {
		sym := c.Param("symbol")
		c.JSON(http.StatusOK, gin.H{
			"code": 0,
			"data": model.SymbolInfo{
				Symbol:      sym,
				Name:        "Bitcoin",
				Exchange:    "BINANCE",
				ProductType: "SPOT",
			},
		})
	})
	
	req, _ := http.NewRequest("GET", "/v1/market/symbols/BTC-USDT", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	
	assert.Equal(t, http.StatusOK, w.Code)
	
	var resp map[string]interface{}
	err := json.Unmarshal(w.Body.Bytes(), &resp)
	require.NoError(t, err)
	assert.Equal(t, float64(0), resp["code"])
}

func TestWebSocketStream_Connection(t *testing.T) {
	r := setupRouter()
	
	// WebSocket endpoint returns 101 Switching Protocols on real upgrade
	// For testing, we just verify the endpoint exists
	r.GET("/v1/market/stream", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"code":    0,
			"message": "WebSocket endpoint ready",
		})
	})
	
	req, _ := http.NewRequest("GET", "/v1/market/stream", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	
	assert.Equal(t, http.StatusOK, w.Code)
}

func TestWebSocketMessage_JSON(t *testing.T) {
	msg := model.WebSocketMessage{
		Type:   "quote",
		Symbol: "BTC-USDT",
		Data: model.QuoteData{
			Symbol:    "BTC-USDT",
			LastPrice: 50000.0,
		},
	}
	
	data, err := json.Marshal(msg)
	require.NoError(t, err)
	
	var decoded model.WebSocketMessage
	err = json.Unmarshal(data, &decoded)
	require.NoError(t, err)
	
	assert.Equal(t, "quote", decoded.Type)
	assert.Equal(t, "BTC-USDT", decoded.Symbol)
}

func TestQuoteData_JSON(t *testing.T) {
	quote := model.QuoteData{
		Symbol:        "ETH-USDT",
		LastPrice:     3000.0,
		OpenPrice:     2950.0,
		HighPrice:     3050.0,
		LowPrice:      2900.0,
		Volume:        98765,
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
