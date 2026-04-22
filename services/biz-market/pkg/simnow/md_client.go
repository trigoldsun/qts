package simnow

import (
	"encoding/binary"
	"fmt"
	"sync"
	"time"
)

/*
SimNow MD-API (Market Data API) Client

SimNow is a simulation platform for CTP (Comprehensive Transaction Protocol) futures trading.
The MD-API provides real-time market data via a socket connection.

Connection Flow:
1. Connect to SimNow Front (tcp://127.0.0.1:41205)
2. Login with BrokerID, UserID, Password
3. Subscribe to market data (Instruments)
4. Receive real-time data via callback

Data Format:
- SimNow uses binary protocol similar to CTP
- Each packet has a header with type and length
- Messages are compressed for market data
*/

// MDClient SimNow Market Data Client
type MDClient struct {
	frontAddr string
	brokerID  string
	userID    string
	password  string

	// Connection state
	connected   bool
	loggedIn    bool
	connectedMu sync.RWMutex

	// Subscription management
	subscribedInstruments map[string]bool
	subscribedInstrumentsMu sync.RWMutex

	// Callbacks
	onTick     func(TickData)
	onQuote    func(QuoteData)
	onKline    func(KlineData)
	onError    func(error)
	onStatus   func(StatusType)

	// Heartbeat
	heartbeatInterval time.Duration
	lastHeartbeat     time.Time

	// Request ID generator
	requestID int
	requestIDMu sync.Mutex
}

// StatusType 连接状态
type StatusType int

const (
	StatusDisconnected StatusType = iota
	StatusConnecting
	StatusConnected
	StatusLoggingIn
	StatusLoggedIn
	StatusError
)

func (s StatusType) String() string {
	switch s {
	case StatusDisconnected:
		return "Disconnected"
	case StatusConnecting:
		return "Connecting"
	case StatusConnected:
		return "Connected"
	case StatusLoggingIn:
		return "LoggingIn"
	case StatusLoggedIn:
		return "LoggedIn"
	case StatusError:
		return "Error"
	default:
		return "Unknown"
	}
}

// TickData 成交数据
type TickData struct {
	Symbol      string
	TradeTime   time.Time
	Price       float64
	Quantity    int
	Turnover    float64
	BSFlag      byte // B:Buy S:Sell
	OrderKind   byte
	OrderID     string
}

// QuoteData 行情快照
type QuoteData struct {
	Symbol          string
	LastPrice      float64
	OpenPrice      float64
	HighPrice      float64
	LowPrice       float64
	ClosePrice     float64
	PreClosePrice  float64
	Volume         int64
	Turnover       float64
	OpenInterest   int64
	UpperLimitPrice float64
	LowerLimitPrice float64
	BidPrices      [5]float64
	AskPrices      [5]float64
	BidVolumes     [5]int
	AskVolumes     [5]int
	Timestamp      time.Time
}

// KlineData K线数据
type KlineData struct {
	Symbol    string
	Period    string
	Open      float64
	High      float64
	Low       float64
	Close     float64
	Volume    int64
	Amount    float64
	Timestamp time.Time
}

// NewMDClient 创建SimNow行情客户端
func NewMDClient(frontAddr, brokerID, userID, password string) *MDClient {
	return &MDClient{
		frontAddr:            frontAddr,
		brokerID:             brokerID,
		userID:               userID,
		password:             password,
		subscribedInstruments: make(map[string]bool),
		heartbeatInterval:    30 * time.Second,
	}
}

// SetCallbacks 设置回调函数
func (c *MDClient) SetCallbacks(callbacks MDCallbacks) {
	c.onTick = callbacks.OnTick
	c.onQuote = callbacks.OnQuote
	c.onKline = callbacks.OnKline
	c.onError = callbacks.OnError
	c.onStatus = callbacks.OnStatus
}

// MDCallbacks 回调函数集合
type MDCallbacks struct {
	OnTick   func(TickData)
	OnQuote  func(QuoteData)
	OnKline  func(KlineData)
	OnError  func(error)
	OnStatus func(StatusType)
}

// Connect 连接到SimNow前置
func (c *MDClient) Connect() error {
	c.connectedMu.Lock()
	defer c.connectedMu.Unlock()

	if c.connected {
		return nil
	}

	c.setStatus(StatusConnecting)

	// In real implementation:
	// 1. Establish TCP connection to frontAddr
	// 2. Start receiving goroutine
	// 3. Wait for OnFrontConnected callback
	
	// Simulate connection
	c.connected = true
	c.setStatus(StatusConnected)
	c.lastHeartbeat = time.Now()

	return nil
}

// Login 登录
func (c *MDClient) Login() error {
	c.connectedMu.RLock()
	if !c.connected {
		c.connectedMu.RUnlock()
		return fmt.Errorf("not connected")
	}
	c.connectedMu.RUnlock()

	c.setStatus(StatusLoggingIn)

	// In real implementation:
	// Build and send CThostFtdcReqUserLoginField
	// Wait for OnRspUserLogin callback
	
	// Simulate login
	c.loggedIn = true
	c.setStatus(StatusLoggedIn)
	
	return nil
}

// Logout 登出
func (c *MDClient) Logout() error {
	c.connectedMu.Lock()
	defer c.connectedMu.Unlock()

	if !c.connected {
		return nil
	}

	// In real implementation:
	// Build and send CThostFtdcReqUserLogoutField
	// Wait for OnRspUserLogout callback
	
	c.loggedIn = false
	c.setStatus(StatusDisconnected)
	c.connected = false

	return nil
}

// SubscribeMarketData 订阅行情
func (c *MDClient) SubscribeMarketData(instruments []string) error {
	c.connectedMu.RLock()
	if !c.loggedIn {
		c.connectedMu.RUnlock()
		return fmt.Errorf("not logged in")
	}
	c.connectedMu.RUnlock()

	c.subscribedInstrumentsMu.Lock()
	defer c.subscribedInstrumentsMu.Unlock()

	for _, instrument := range instruments {
		c.subscribedInstruments[instrument] = true
	}

	// In real implementation:
	// Build CThostFtdcReqSubscribeMarketDataField with instrument IDs
	// Call trading api to subscribe
	
	return nil
}

// UnsubscribeMarketData 取消订阅
func (c *MDClient) UnsubscribeMarketData(instruments []string) error {
	c.connectedMu.RLock()
	if !c.loggedIn {
		c.connectedMu.RUnlock()
		return fmt.Errorf("not logged in")
	}
	c.connectedMu.RUnlock()

	c.subscribedInstrumentsMu.Lock()
	defer c.subscribedInstrumentsMu.Unlock()

	for _, instrument := range instruments {
		delete(c.subscribedInstruments, instrument)
	}

	// In real implementation:
	// Build CThostFtdcReqUnSubscribeMarketDataField
	// Call trading api to unsubscribe
	
	return nil
}

// IsConnected 检查连接状态
func (c *MDClient) IsConnected() bool {
	c.connectedMu.RLock()
	defer c.connectedMu.RUnlock()
	return c.connected
}

// IsLoggedIn 检查登录状态
func (c *MDClient) IsLoggedIn() bool {
	c.connectedMu.RLock()
	defer c.connectedMu.RUnlock()
	return c.loggedIn
}

// GetNextRequestID 获取下一个请求ID
func (c *MDClient) GetNextRequestID() int {
	c.requestIDMu.Lock()
	defer c.requestIDMu.Unlock()
	c.requestID++
	return c.requestID
}

func (c *MDClient) setStatus(status StatusType) {
	if c.onStatus != nil {
		c.onStatus(status)
	}
}

// SimNowPacketHeader SimNow数据包头
type SimNowPacketHeader struct {
	PacketType uint16
	Length     uint32
}

// SimNow packet types (based on CTP protocol)
const (
	PacketTypeHeartbeat       = 0x0001
	PacketTypeMarketData      = 0x0002
	PacketTypeTradeData       = 0x0003
	PacketTypeSubscribe       = 0x0010
	PacketTypeUnsubscribe     = 0x0011
)

// SimulateReceiveData 模拟接收数据（用于开发测试）
// 实际运行时，这个函数会被真实的网络接收循环替代
func (c *MDClient) SimulateReceiveData() {
	if c.onTick == nil || c.onQuote == nil {
		return
	}

	// 模拟发送一些市场数据
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()

	for range ticker.C {
		c.connectedMu.RLock()
		if !c.loggedIn {
			c.connectedMu.RUnlock()
			break
		}
		c.connectedMu.RUnlock()

		// 模拟行情数据
		quote := QuoteData{
			Symbol:         "IF2405",
			LastPrice:      3800.0 + float64(time.Now().Unix()%100)/10,
			OpenPrice:      3795.0,
			HighPrice:      3810.0,
			LowPrice:       3785.0,
			ClosePrice:     3800.0,
			PreClosePrice:  3790.0,
			Volume:         50000 + int64(time.Now().Unix()%1000),
			Turnover:       190000000.0,
			OpenInterest:   100000,
			UpperLimitPrice: 4169.0,
			LowerLimitPrice: 3411.0,
			Timestamp:      time.Now(),
		}
		for i := 0; i < 5; i++ {
			quote.BidPrices[i] = quote.LastPrice - float64(5-i)*0.2
			quote.AskPrices[i] = quote.LastPrice + float64(i+1)*0.2
			quote.BidVolumes[i] = 10 + i*2
			quote.AskVolumes[i] = 8 + i*2
		}

		c.onQuote(quote)
	}
}

// ParsePacket 解析数据包（实际协议解析）
func ParsePacket(data []byte) (interface{}, error) {
	if len(data) < 6 {
		return nil, fmt.Errorf("packet too short")
	}

	packetType := binary.LittleEndian.Uint16(data[0:2])
	length := binary.LittleEndian.Uint32(data[2:6])

	if uint32(len(data)) < length+6 {
		return nil, fmt.Errorf("incomplete packet")
	}

	payload := data[6 : 6+length]

	switch packetType {
	case PacketTypeHeartbeat:
		return nil, nil // Heartbeat, no payload
	case PacketTypeMarketData:
		return parseMarketDataPayload(payload)
	case PacketTypeTradeData:
		return parseTradeDataPayload(payload)
	default:
		return nil, fmt.Errorf("unknown packet type: %d", packetType)
	}
}

func parseMarketDataPayload(payload []byte) (QuoteData, error) {
	// In real implementation, parse CTP market data binary format
	// This is a simplified structure
	
	quote := QuoteData{}
	// Parse binary payload according to CTP structure
	// ... (actual parsing code)
	
	return quote, nil
}

func parseTradeDataPayload(payload []byte) (TickData, error) {
	// In real implementation, parse CTP trade data binary format
	
	tick := TickData{}
	// Parse binary payload according to CTP structure
	// ... (actual parsing code)
	
	return tick, nil
}
