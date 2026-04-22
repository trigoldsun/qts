package handler

/*
HTTP Handler - 行情API

API清单：
- F-DATA-001: WS /v1/market/stream - 实时行情订阅
- F-DATA-002: GET /v1/market/quotes/{symbol} - 行情快照
- F-DATA-003: GET /v1/market/kline - K线查询
- F-DATA-004: GET /v1/market/symbols - 标的信息
*/

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/qts/biz-market/internal/service"
	ws "github.com/qts/biz-market/internal/websocket"

	"github.com/gin-gonic/gin"
	wsutil "github.com/gorilla/websocket"
)

var upgrader = wsutil.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true // 允许所有来源（生产环境应限制）
	},
}

// HTTPHandler HTTP处理器
type HTTPHandler struct {
	marketSvc *service.MarketService
}

// NewHTTPHandler 创建HTTP处理器
func NewHTTPHandler(marketSvc *service.MarketService) *HTTPHandler {
	return &HTTPHandler{
		marketSvc: marketSvc,
	}
}

// RegisterRoutes 注册路由
func (h *HTTPHandler) RegisterRoutes(r *gin.Engine) {
	// 健康检查
	r.GET("/health", h.HealthCheck)

	// 行情API
	v1 := r.Group("/v1/market")
	{
		// 行情快照
		v1.GET("/quotes/:symbol", h.GetQuote)

		// K线查询
		v1.GET("/kline", h.GetKline)

		// 标的信息
		v1.GET("/symbols", h.GetSymbols)
		v1.GET("/symbols/:symbol", h.GetSymbol)
	}

	// WebSocket端点
	r.GET("/v1/market/stream", h.WebSocketStream)
}

// HealthCheck 健康检查
func (h *HTTPHandler) HealthCheck(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"status":    "healthy",
		"timestamp": time.Now().Unix(),
	})
}

// GetQuote 获取行情快照
// GET /v1/market/quotes/{symbol}
func (h *HTTPHandler) GetQuote(c *gin.Context) {
	symbol := c.Param("symbol")
	if symbol == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "symbol is required",
		})
		return
	}

	quote, err := h.marketSvc.GetQuote(symbol)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": fmt.Sprintf("quote not found for symbol: %s", symbol),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    quote,
	})
}

// GetKline 获取K线
// GET /v1/market/kline?symbol=IF2405&period=1m&limit=100
func (h *HTTPHandler) GetKline(c *gin.Context) {
	symbol := c.Query("symbol")
	period := c.DefaultQuery("period", "1m")
	limit := 100

	if symbol == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "symbol is required",
		})
		return
	}

	klines, err := h.marketSvc.GetKline(symbol, period, limit)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"symbol":    symbol,
			"period":    period,
			"klines":    klines,
			"count":     len(klines),
		},
	})
}

// GetSymbols 获取标的信息列表
// GET /v1/market/symbols
func (h *HTTPHandler) GetSymbols(c *gin.Context) {
	symbols := h.marketSvc.GetSymbols()

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"symbols": symbols,
			"count":   len(symbols),
		},
	})
}

// GetSymbol 获取单个标的信息
// GET /v1/market/symbols/{symbol}
func (h *HTTPHandler) GetSymbol(c *gin.Context) {
	symbol := c.Param("symbol")
	if symbol == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "symbol is required",
		})
		return
	}

	info, err := h.marketSvc.GetSymbol(symbol)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": fmt.Sprintf("symbol not found: %s", symbol),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    info,
	})
}

// WebSocketStream 实时行情WebSocket
// WS /v1/market/stream
func (h *HTTPHandler) WebSocketStream(c *gin.Context) {
	// 升级为WebSocket
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		fmt.Printf("WebSocket upgrade failed: %v\n", err)
		return
	}

	// 生成连接ID
	connID := fmt.Sprintf("ws-%d", time.Now().UnixNano())

	// 添加到连接管理器
	wsMgr := h.marketSvc.GetWSManager()
	wsConn, err := wsMgr.AcceptConnection(conn, connID)
	if err != nil {
		conn.Close()
		return
	}

	fmt.Printf("WebSocket connection established: %s\n", connID)

	// 处理连接
	go h.handleWSConnection(wsConn)
}

func (h *HTTPHandler) handleWSConnection(conn *ws.Connection) {
	defer func() {
		h.marketSvc.GetWSManager().RemoveConnection(conn.ID)
		conn.Session.Close()
		fmt.Printf("WebSocket connection closed: %s\n", conn.ID)
	}()

	// 设置读取超时
	conn.Session.SetReadDeadline(time.Now().Add(60 * time.Second))

	for {
		_, message, err := conn.Session.ReadMessage()
		if err != nil {
			if strings.Contains(err.Error(), "websocket: close") {
				return
			}
			fmt.Printf("WebSocket read error: %v\n", err)
			return
		}

		// 重置读取超时
		conn.Session.SetReadDeadline(time.Now().Add(60 * time.Second))
		conn.UpdateLastActive()

		// 处理消息
		if err := h.marketSvc.HandleWSMessage(conn.ID, message); err != nil {
			// 发送错误响应
			errMsg, _ := json.Marshal(gin.H{
				"code":    1,
				"message": err.Error(),
			})
			conn.Session.WriteMessage(wsutil.TextMessage, errMsg)
		}
	}
}

// API响应结构
type APIResponse struct {
	Code    int         `json:"code"`
	Message string      `json:"message"`
	Data    interface{} `json:"data,omitempty"`
}
