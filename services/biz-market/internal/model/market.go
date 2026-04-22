package model

import (
	"time"
)

// TickData Tick数据（逐笔成交）
type TickData struct {
	Symbol      string    `json:"symbol"`
	TradeTime   time.Time `json:"trade_time"`
	Price       float64   `json:"price"`
	Quantity    int       `json:"quantity"`
	Turnover    float64   `json:"turnover"`
	BSFlag      string    `json:"bs_flag"` // B:买 S:卖
	OrderKind   string    `json:"order_kind"` // 订单类型
	OrderID     string    `json:"order_id"`
}

// QuoteData 行情快照
type QuoteData struct {
	Symbol         string    `json:"symbol"`
	LastPrice      float64   `json:"last_price"`
	OpenPrice      float64   `json:"open_price"`
	HighPrice      float64   `json:"high_price"`
	LowPrice       float64   `json:"low_price"`
	ClosePrice     float64   `json:"close_price"`
	PreClosePrice  float64   `json:"pre_close_price"`
	Volume         int64     `json:"volume"`
	Turnover       float64   `json:"turnover"`
	OpenInterest   int64     `json:"open_interest"`
	UpperLimitPrice float64  `json:"upper_limit_price"`
	LowerLimitPrice float64  `json:"lower_limit_price"`
	BidPrice1      float64   `json:"bid_price1"`
	BidPrice2      float64   `json:"bid_price2"`
	BidPrice3      float64   `json:"bid_price3"`
	BidPrice4      float64   `json:"bid_price4"`
	BidPrice5      float64   `json:"bid_price5"`
	AskPrice1      float64   `json:"ask_price1"`
	AskPrice2      float64   `json:"ask_price2"`
	AskPrice3      float64   `json:"ask_price3"`
	AskPrice4      float64   `json:"ask_price4"`
	AskPrice5      float64   `json:"ask_price5"`
	BidVolume1     int       `json:"bid_volume1"`
	BidVolume2     int       `json:"bid_volume2"`
	BidVolume3     int       `json:"bid_volume3"`
	BidVolume4     int       `json:"bid_volume4"`
	BidVolume5     int       `json:"bid_volume5"`
	AskVolume1     int       `json:"ask_volume1"`
	AskVolume2     int       `json:"ask_volume2"`
	AskVolume3     int       `json:"ask_volume3"`
	AskVolume4     int       `json:"ask_volume4"`
	AskVolume5     int       `json:"ask_volume5"`
	Timestamp      time.Time `json:"timestamp"`
}

// KlineData K线数据
type KlineData struct {
	Symbol    string    `json:"symbol"`
	Period    string    `json:"period"` // 1m, 5m, 15m, 1h, 1d
	Open      float64   `json:"open"`
	High      float64   `json:"high"`
	Low       float64   `json:"low"`
	Close     float64   `json:"close"`
	Volume    int64     `json:"volume"`
	Amount    float64   `json:"amount"`
	Timestamp time.Time `json:"timestamp"`
}

// SymbolInfo 标的信息
type SymbolInfo struct {
	Symbol       string `json:"symbol"`
	Name         string `json:"name"`
	Exchange     string `json:"exchange"`
	ProductType  string `json:"product_type"` // 股票/期货/期权
	Multiplier   float64 `json:"multiplier"`
	PriceTick    float64 `json:"price_tick"`
	ListedDate   string `json:"listed_date"`
	DelistedDate string `json:"delisted_date,omitempty"`
}

// SubscribeRequest 订阅请求
type SubscribeRequest struct {
	Symbols []string `json:"symbols"`
	Periods []string `json:"periods"` // tick, quote, 1m, 5m, 15m, 1h, 1d
}

// WebSocketMessage WebSocket消息
type WebSocketMessage struct {
	Type    string      `json:"type"` // tick, quote, kline, error
	Symbol  string      `json:"symbol,omitempty"`
	Data    interface{} `json:"data,omitempty"`
	Time    time.Time   `json:"time"`
}
