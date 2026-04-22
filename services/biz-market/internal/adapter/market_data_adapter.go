package adapter

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"
	"time"

	"github.com/qts/biz-market/internal/model"
	"github.com/qts/biz-market/pkg/simnow"
	"github.com/IBM/sarama"
)

/*
MarketDataAdapter - 行情数据适配器

职责：
1. 封装SimNow MD-API客户端
2. 数据清洗与标准化
3. 数据质量校验
4. 行情中断监控
*/

// MarketDataAdapter 行情数据适配器
type MarketDataAdapter struct {
	// SimNow客户端
	mdClient *simnow.MDClient

	// Kafka producer
	kafkaProducer sarama.AsyncProducer

	// 订阅管理
	subscriptions map[string]map[string]bool // symbol -> period -> subscribed
	subscriptionsMu sync.RWMutex

	// 行情缓存
	quoteCache map[string]*model.QuoteData
	quoteCacheMu sync.RWMutex

	// K线数据缓存
	klineCache map[string]*model.KlineData // symbol_period -> kline
	klineCacheMu sync.RWMutex

	// 延迟监控
	lastQuoteTime map[string]time.Time
	lastQuoteTimeMu sync.RWMutex

	// 回调
	onQuote func(model.QuoteData)
	onTick  func(model.TickData)
}

// NewMarketDataAdapter 创建行情适配器
func NewMarketDataAdapter(frontAddr, brokerID, userID, password string) *MarketDataAdapter {
	adapter := &MarketDataAdapter{
		mdClient:     simnow.NewMDClient(frontAddr, brokerID, userID, password),
		subscriptions: make(map[string]map[string]bool),
		quoteCache:    make(map[string]*model.QuoteData),
		klineCache:   make(map[string]*model.KlineData),
		lastQuoteTime: make(map[string]time.Time),
	}

	// 设置SimNow回调
	adapter.mdClient.SetCallbacks(simnow.MDCallbacks{
		OnQuote:  adapter.handleQuote,
		OnTick:   adapter.handleTick,
		OnError:  adapter.handleError,
		OnStatus: adapter.handleStatus,
	})

	return adapter
}

// Connect 连接
func (a *MarketDataAdapter) Connect(ctx context.Context) error {
	if err := a.mdClient.Connect(); err != nil {
		return err
	}
	// Login after connect
	return a.mdClient.Login()
}

// Disconnect 断开连接
func (a *MarketDataAdapter) Disconnect() error {
	return a.mdClient.Logout()
}

// Subscribe 订阅行情
func (a *MarketDataAdapter) Subscribe(symbol string, periods []string) error {
	a.subscriptionsMu.Lock()
	defer a.subscriptionsMu.Unlock()

	if a.subscriptions[symbol] == nil {
		a.subscriptions[symbol] = make(map[string]bool)
	}

	for _, period := range periods {
		a.subscriptions[symbol][period] = true
	}

	// 调用底层订阅
	return a.mdClient.SubscribeMarketData([]string{symbol})
}

// Unsubscribe 取消订阅
func (a *MarketDataAdapter) Unsubscribe(symbol string, periods []string) error {
	a.subscriptionsMu.Lock()
	defer a.subscriptionsMu.Unlock()

	if a.subscriptions[symbol] == nil {
		return nil
	}

	for _, period := range periods {
		delete(a.subscriptions[symbol], period)
	}

	// 如果所有周期都退订了，则取消symbol订阅
	hasPeriods := false
	for _, subscribed := range a.subscriptions[symbol] {
		if subscribed {
			hasPeriods = true
			break
		}
	}
	if !hasPeriods {
		delete(a.subscriptions, symbol)
		return a.mdClient.UnsubscribeMarketData([]string{symbol})
	}

	return nil
}

// SetCallbacks 设置回调
func (a *MarketDataAdapter) SetCallbacks(onQuote func(model.QuoteData), onTick func(model.TickData)) {
	a.onQuote = onQuote
	a.onTick = onTick
}

// handleQuote 处理行情数据
func (a *MarketDataAdapter) handleQuote(quote simnow.QuoteData) {
	// 转换为model.QuoteData
	quoteData := &model.QuoteData{
		Symbol:          quote.Symbol,
		LastPrice:      quote.LastPrice,
		OpenPrice:      quote.OpenPrice,
		HighPrice:      quote.HighPrice,
		LowPrice:       quote.LowPrice,
		ClosePrice:     quote.ClosePrice,
		UpperLimitPrice: quote.UpperLimitPrice,
		LowerLimitPrice: quote.LowerLimitPrice,
		Volume:         quote.Volume,
		Turnover:       quote.Turnover,
		Timestamp:      time.Now(),
	}

	// 数据清洗：价格合理值检查
	if !a.validatePrice(quoteData) {
		fmt.Printf("WARN: Price %.6f out of limit [%f, %f] for %s\n",
			quoteData.LastPrice, quoteData.LowPrice, quoteData.HighPrice, quoteData.Symbol)
		return
	}

	// 数据清洗：成交量非负检查
	if quoteData.Volume < 0 {
		fmt.Printf("WARN: Negative volume for %s\n", quoteData.Symbol)
		return
	}

	// 更新缓存
	a.quoteCacheMu.Lock()
	a.quoteCache[quote.Symbol] = quoteData
	a.quoteCacheMu.Unlock()

	// 更新延迟监控
	a.lastQuoteTimeMu.Lock()
	a.lastQuoteTime[quote.Symbol] = time.Now()
	a.lastQuoteTimeMu.Unlock()

	// 更新K线
	a.updateKline(quoteData)

	// 回调
	if a.onQuote != nil {
		a.onQuote(*quoteData)
	}

	// 发布到Kafka
	a.publishToKafka(quoteData)
}

// handleTick 处理 tick 数据
func (a *MarketDataAdapter) handleTick(tick simnow.TickData) {
	tickData := model.TickData{
		Symbol:    tick.Symbol,
		TradeTime: tick.TradeTime,
		Price:     tick.Price,
		Quantity:  tick.Quantity,
		Turnover:  tick.Turnover,
	}

	// 回调
	if a.onTick != nil {
		a.onTick(tickData)
	}
}

// handleError 处理错误
func (a *MarketDataAdapter) handleError(err error) {
	fmt.Printf("SimNow error: %v\n", err)
}

// handleStatus 处理状态变化
func (a *MarketDataAdapter) handleStatus(status simnow.StatusType) {
	fmt.Printf("SimNow status: %s\n", status)
}

// validatePrice 检查价格合理性
func (a *MarketDataAdapter) validatePrice(quote *model.QuoteData) bool {
	// 对于期货，价格不能为0
	if quote.LastPrice <= 0 {
		return false
	}

	// 如果有UpperLimitPrice/LowerLimitPrice限制，价格必须在范围内
	if quote.UpperLimitPrice > 0 && quote.LowerLimitPrice > 0 {
		if quote.LastPrice < quote.LowerLimitPrice || quote.LastPrice > quote.UpperLimitPrice {
			return false
		}
	}

	// 成交量不能为负数
	if quote.Volume < 0 {
		return false
	}

	return true
}

// GetQuote 获取行情数据
func (a *MarketDataAdapter) GetQuote(symbol string) (*model.QuoteData, bool) {
	a.quoteCacheMu.RLock()
	defer a.quoteCacheMu.RUnlock()

	quote, ok := a.quoteCache[symbol]
	return quote, ok
}

// GetKline 获取K线数据
func (a *MarketDataAdapter) GetKline(symbol, period string) (*model.KlineData, bool) {
	a.klineCacheMu.RLock()
	defer a.klineCacheMu.RUnlock()

	key := symbol + "_" + period
	kline, ok := a.klineCache[key]
	return kline, ok
}

// StartLatencyMonitor 启动延迟监控
func (a *MarketDataAdapter) StartLatencyMonitor(ctx context.Context) {
	go func() {
		ticker := time.NewTicker(5 * time.Second)
		defer ticker.Stop()

		for {
			select {
			case <-ctx.Done():
				return
			case <-ticker.C:
				a.checkLatency()
			}
		}
	}()
}

// checkLatency 检查行情延迟
func (a *MarketDataAdapter) checkLatency() {
	a.lastQuoteTimeMu.Lock()
	defer a.lastQuoteTimeMu.Unlock()

	now := time.Now()
	for symbol, lastTime := range a.lastQuoteTime {
		latency := now.Sub(lastTime)
		if latency > 1*time.Second {
			// 延迟超过1秒，触发P1告警
			fmt.Printf("ALERT [P1]: Market data latency for %s is %v (>1s)\n", symbol, latency)
		}
	}
}

// updateKline 更新K线数据
func (a *MarketDataAdapter) updateKline(quote *model.QuoteData) {
	periods := []string{"1m", "5m"}
	for _, period := range periods {
		key := quote.Symbol + "_" + period
		a.klineCacheMu.Lock()
		kline, ok := a.klineCache[key]
		if !ok {
			kline = &model.KlineData{
				Symbol:    quote.Symbol,
				Period:    period,
				Timestamp: quote.Timestamp.Truncate(time.Minute),
			}
			a.klineCache[key] = kline
		}

		// 更新K线数据 - 简化处理：始终重置K线数据
		kline.Open = quote.LastPrice
		kline.High = quote.LastPrice
		kline.Low = quote.LastPrice
		kline.Close = quote.LastPrice
		kline.Volume = quote.Volume
		kline.Amount = quote.Turnover
		kline.Timestamp = quote.Timestamp.Truncate(time.Minute)
		a.klineCacheMu.Unlock()
	}
}

// publishToKafka 发布到Kafka
func (a *MarketDataAdapter) publishToKafka(quote *model.QuoteData) {
	if a.kafkaProducer == nil {
		return
	}

	msg := &sarama.ProducerMessage{
		Topic: "qts.market.quotes",
		Key:   sarama.StringEncoder(quote.Symbol),
	}

	data, err := json.Marshal(quote)
	if err != nil {
		return
	}
	msg.Value = sarama.ByteEncoder(data)

	a.kafkaProducer.Input() <- msg
}

// SetKafkaProducer 设置Kafka生产者
func (a *MarketDataAdapter) SetKafkaProducer(producer sarama.AsyncProducer) {
	a.kafkaProducer = producer
}