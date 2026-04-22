package service

/*
MarketService - 行情服务

职责：
1. 行情订阅路由
2. 实时行情推送
3. 行情快照API
4. K线查询
*/

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"

	"github.com/qts/biz-market/internal/adapter"
	"github.com/qts/biz-market/internal/model"
	"github.com/qts/biz-market/internal/websocket"
)

// MarketService 行情服务
type MarketService struct {
	// SimNow适配器
	adapter *adapter.MarketDataAdapter

	// WebSocket管理器
	wsManager *websocket.ConnectionManager

	// 行情缓存
	quoteCache map[string]*model.QuoteData
	quoteMu    sync.RWMutex

	// K线仓库
	klineCache map[string][]*model.KlineData // symbol_period -> klines
	klineMu    sync.RWMutex

	// 标的信息缓存
	symbolInfo map[string]*model.SymbolInfo
	symbolMu   sync.RWMutex
}

// NewMarketService 创建行情服务
func NewMarketService(adapter *adapter.MarketDataAdapter) *MarketService {
	svc := &MarketService{
		adapter:    adapter,
		wsManager:  websocket.NewConnectionManager(),
		quoteCache: make(map[string]*model.QuoteData),
		klineCache: make(map[string][]*model.KlineData),
		symbolInfo: make(map[string]*model.SymbolInfo),
	}

	// 设置行情回调
	adapter.SetCallbacks(svc.handleQuote, svc.handleTick)

	return svc
}

// Start 启动服务
func (s *MarketService) Start(ctx context.Context) error {
	// 启动延迟监控
	s.adapter.StartLatencyMonitor(ctx)

	return nil
}

// MdClient 获取MD客户端
func (s *MarketService) MdClient() *adapter.MarketDataAdapter {
	return s.adapter
}

// GetWSManager 获取WebSocket管理器
func (s *MarketService) GetWSManager() *websocket.ConnectionManager {
	return s.wsManager
}

// Subscribe 订阅行情
func (s *MarketService) Subscribe(connID string, symbols []string, periods []string) error {
	if len(periods) == 0 {
		periods = []string{"tick", "quote"}
	}

	// 先订阅到SimNow
	for _, symbol := range symbols {
		if err := s.adapter.Subscribe(symbol, periods); err != nil {
			return err
		}
	}

	// 添加WebSocket订阅
	for _, symbol := range symbols {
		if err := s.wsManager.Subscribe(connID, symbol); err != nil {
			return err
		}
	}

	return nil
}

// Unsubscribe 取消订阅
func (s *MarketService) Unsubscribe(connID string, symbols []string) error {
	for _, symbol := range symbols {
		if err := s.adapter.Unsubscribe(symbol, nil); err != nil {
			return err
		}
	}

	for _, symbol := range symbols {
		if err := s.wsManager.Unsubscribe(connID, symbol); err != nil {
			return err
		}
	}

	return nil
}

// GetQuote 获取行情快照
func (s *MarketService) GetQuote(symbol string) (*model.QuoteData, error) {
	s.quoteMu.RLock()
	defer s.quoteMu.RUnlock()

	if quote, ok := s.quoteCache[symbol]; ok {
		return quote, nil
	}

	// 尝试从适配器获取
	quote, ok := s.adapter.GetQuote(symbol)
	if !ok {
		return nil, fmt.Errorf("quote not found for symbol: %s", symbol)
	}
	return quote, nil
}

// GetKline 获取K线
func (s *MarketService) GetKline(symbol, period string, limit int) ([]*model.KlineData, error) {
	if limit <= 0 {
		limit = 100
	}

	key := symbol + "_" + period
	s.klineMu.RLock()
	klines, ok := s.klineCache[key]
	s.klineMu.RUnlock()

	if !ok {
		// 返回空或从数据库查询
		return []*model.KlineData{}, nil
	}

	if len(klines) <= limit {
		return klines, nil
	}

	return klines[len(klines)-limit:], nil
}

// GetSymbols 获取标的信息
func (s *MarketService) GetSymbols() []*model.SymbolInfo {
	s.symbolMu.RLock()
	defer s.symbolMu.RUnlock()

	symbols := make([]*model.SymbolInfo, 0, len(s.symbolInfo))
	for _, info := range s.symbolInfo {
		symbols = append(symbols, info)
	}

	return symbols
}

// GetSymbol 获取单个标的信息
func (s *MarketService) GetSymbol(symbol string) (*model.SymbolInfo, error) {
	s.symbolMu.RLock()
	defer s.symbolMu.RUnlock()

	if info, ok := s.symbolInfo[symbol]; ok {
		return info, nil
	}

	return nil, fmt.Errorf("symbol not found: %s", symbol)
}

// 内部方法

func (s *MarketService) handleQuote(quote model.QuoteData) {
	// 更新缓存
	s.quoteMu.Lock()
	s.quoteCache[quote.Symbol] = &quote
	s.quoteMu.Unlock()

	// 广播到WebSocket订阅者
	s.wsManager.BroadcastToSymbol(quote.Symbol, "quote", quote)
}

func (s *MarketService) handleTick(tick model.TickData) {
	// 广播到WebSocket订阅者
	s.wsManager.BroadcastToSymbol(tick.Symbol, "tick", tick)
}

// WebSocket请求消息
type WSRequestMessage struct {
	Type     string          `json:"type"` // subscribe, unsubscribe, ping
	Symbols  []string        `json:"symbols,omitempty"`
	Periods  []string        `json:"periods,omitempty"`
	Symbol   string          `json:"symbol,omitempty"`
	Data     json.RawMessage `json:"data,omitempty"`
}

// HandleWSMessage 处理WebSocket消息
func (s *MarketService) HandleWSMessage(connID string, message []byte) error {
	var req WSRequestMessage
	if err := json.Unmarshal(message, &req); err != nil {
		return fmt.Errorf("invalid message format: %w", err)
	}

	switch req.Type {
	case "subscribe":
		return s.Subscribe(connID, req.Symbols, req.Periods)
	case "unsubscribe":
		return s.Unsubscribe(connID, req.Symbols)
	case "ping":
		// 心跳，已由WebSocket库处理
	default:
		return fmt.Errorf("unknown message type: %s", req.Type)
	}

	return nil
}
