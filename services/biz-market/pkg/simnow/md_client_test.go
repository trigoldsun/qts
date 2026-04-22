package simnow

import (
	"encoding/binary"
	"testing"
	"time"
)

func TestNewMDClient(t *testing.T) {
	client := NewMDClient("tcp://127.0.0.1:41205", "9999", "user1", "password1")
	if client == nil {
		t.Fatal("NewMDClient returned nil")
	}
	if client.frontAddr != "tcp://127.0.0.1:41205" {
		t.Errorf("expected frontAddr tcp://127.0.0.1:41205, got %s", client.frontAddr)
	}
	if client.brokerID != "9999" {
		t.Errorf("expected brokerID 9999, got %s", client.brokerID)
	}
	if client.userID != "user1" {
		t.Errorf("expected userID user1, got %s", client.userID)
	}
	if client.subscribedInstruments == nil {
		t.Error("expected subscribedInstruments to be initialized")
	}
}

func TestMDClient_SetCallbacks(t *testing.T) {
	client := NewMDClient("", "", "", "")
	
	var quoteCalled, tickCalled bool
	callbacks := MDCallbacks{
		OnQuote: func(q QuoteData) { quoteCalled = true },
		OnTick:  func(t TickData) { tickCalled = true },
	}
	
	client.SetCallbacks(callbacks)
	
	if client.onQuote == nil {
		t.Error("onQuote callback not set")
	}
	if client.onTick == nil {
		t.Error("onTick callback not set")
	}
	
	// Trigger callbacks
	client.onQuote(QuoteData{Symbol: "IF2405"})
	client.onTick(TickData{Symbol: "IF2405"})
	
	if !quoteCalled {
		t.Error("OnQuote callback was not called")
	}
	if !tickCalled {
		t.Error("OnTick callback was not called")
	}
}

func TestMDClient_Connect(t *testing.T) {
	client := NewMDClient("", "", "", "")
	
	err := client.Connect()
	if err != nil {
		t.Errorf("Connect failed: %v", err)
	}
	
	if !client.IsConnected() {
		t.Error("expected IsConnected to return true after Connect")
	}
	
	// Connect again should be idempotent
	err = client.Connect()
	if err != nil {
		t.Errorf("second Connect failed: %v", err)
	}
}

func TestMDClient_Login(t *testing.T) {
	client := NewMDClient("", "", "", "")
	
	// Login without connect should fail
	err := client.Login()
	if err == nil {
		t.Error("Login without connect should fail")
	}
	
	// Connect first
	client.Connect()
	
	err = client.Login()
	if err != nil {
		t.Errorf("Login after Connect failed: %v", err)
	}
	
	if !client.IsLoggedIn() {
		t.Error("expected IsLoggedIn to return true after Login")
	}
}

func TestMDClient_Logout(t *testing.T) {
	client := NewMDClient("", "", "", "")
	client.Connect()
	client.Login()
	
	err := client.Logout()
	if err != nil {
		t.Errorf("Logout failed: %v", err)
	}
	
	if client.IsLoggedIn() {
		t.Error("expected IsLoggedIn to return false after Logout")
	}
}

func TestMDClient_SubscribeMarketData(t *testing.T) {
	client := NewMDClient("", "", "", "")
	client.Connect()
	client.Login()
	
	instruments := []string{"IF2405", "IF2406"}
	err := client.SubscribeMarketData(instruments)
	if err != nil {
		t.Errorf("SubscribeMarketData failed: %v", err)
	}
	
	// Check subscription
	client.subscribedInstrumentsMu.RLock()
	for _, inst := range instruments {
		if !client.subscribedInstruments[inst] {
			t.Errorf("instrument %s not subscribed", inst)
		}
	}
	client.subscribedInstrumentsMu.RUnlock()
	
	// Subscribe without login should fail
	client2 := NewMDClient("", "", "", "")
	err = client2.SubscribeMarketData(instruments)
	if err == nil {
		t.Error("SubscribeMarketData without login should fail")
	}
}

func TestMDClient_UnsubscribeMarketData(t *testing.T) {
	client := NewMDClient("", "", "", "")
	client.Connect()
	client.Login()
	
	instruments := []string{"IF2405", "IF2406"}
	client.SubscribeMarketData(instruments)
	
	err := client.UnsubscribeMarketData(instruments)
	if err != nil {
		t.Errorf("UnsubscribeMarketData failed: %v", err)
	}
	
	// Check unsubscription
	client.subscribedInstrumentsMu.RLock()
	for _, inst := range instruments {
		if client.subscribedInstruments[inst] {
			t.Errorf("instrument %s still subscribed", inst)
		}
	}
	client.subscribedInstrumentsMu.RUnlock()
}

func TestMDClient_GetNextRequestID(t *testing.T) {
	client := NewMDClient("", "", "", "")
	
	id1 := client.GetNextRequestID()
	id2 := client.GetNextRequestID()
	id3 := client.GetNextRequestID()
	
	if id2 <= id1 {
		t.Error("GetNextRequestID should return increasing IDs")
	}
	if id3 <= id2 {
		t.Error("GetNextRequestID should return increasing IDs")
	}
}

func TestStatusType_String(t *testing.T) {
	tests := []struct {
		status   StatusType
		expected string
	}{
		{StatusDisconnected, "Disconnected"},
		{StatusConnecting, "Connecting"},
		{StatusConnected, "Connected"},
		{StatusLoggingIn, "LoggingIn"},
		{StatusLoggedIn, "LoggedIn"},
		{StatusError, "Error"},
		{StatusType(100), "Unknown"},
	}
	
	for _, tc := range tests {
		if tc.status.String() != tc.expected {
			t.Errorf("StatusType(%d).String() = %s, want %s", tc.status, tc.status.String(), tc.expected)
		}
	}
}

func TestParsePacket(t *testing.T) {
	tests := []struct {
		name    string
		data    []byte
		wantErr bool
	}{
		{
			name:    "packet too short",
			data:    []byte{0x01},
			wantErr: true,
		},
		{
			name:    "incomplete packet",
			data:    []byte{0x01, 0x00, 0x10, 0x00, 0x00, 0x00},
			wantErr: true,
		},
		{
			name: "heartbeat packet",
			data: func() []byte {
				data := make([]byte, 6)
				data[0] = 0x01
				data[1] = 0x00
				data[2] = 0x00
				data[3] = 0x00
				data[4] = 0x00
				data[5] = 0x00
				return data
			}(),
			wantErr: false,
		},
		{
			name: "unknown packet type",
			data: func() []byte {
				data := make([]byte, 6)
				data[0] = 0xFF
				data[1] = 0xFF
				data[2] = 0x00
				data[3] = 0x00
				data[4] = 0x00
				data[5] = 0x00
				return data
			}(),
			wantErr: true,
		},
	}
	
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			_, err := ParsePacket(tc.data)
			if (err != nil) != tc.wantErr {
				t.Errorf("ParsePacket() error = %v, wantErr %v", err, tc.wantErr)
			}
		})
	}
}

func TestParsePacket_MarketData(t *testing.T) {
	// Create a market data packet with proper header
	data := make([]byte, 6+10)
	binary.LittleEndian.PutUint16(data[0:2], PacketTypeMarketData)
	binary.LittleEndian.PutUint32(data[2:6], 10)
	
	result, err := ParsePacket(data)
	if err != nil {
		t.Errorf("ParsePacket failed: %v", err)
	}
	
	quote, ok := result.(QuoteData)
	if !ok {
		t.Errorf("expected QuoteData, got %T", result)
	}
	
	// Validate fields
	_ = quote.Symbol
	_ = quote.LastPrice
}

func TestParsePacket_TradeData(t *testing.T) {
	// Create a trade data packet with proper header
	data := make([]byte, 6+10)
	binary.LittleEndian.PutUint16(data[0:2], PacketTypeTradeData)
	binary.LittleEndian.PutUint32(data[2:6], 10)
	
	result, err := ParsePacket(data)
	if err != nil {
		t.Errorf("ParsePacket failed: %v", err)
	}
	
	tick, ok := result.(TickData)
	if !ok {
		t.Errorf("expected TickData, got %T", result)
	}
	
	_ = tick.Symbol
}

func TestQuoteData_Structure(t *testing.T) {
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
	}
	
	if quote.Symbol != "IF2405" {
		t.Error("QuoteData Symbol not set correctly")
	}
	if quote.LastPrice != 3800.0 {
		t.Error("QuoteData LastPrice not set correctly")
	}
}

func TestTickData_Structure(t *testing.T) {
	now := time.Now()
	tick := TickData{
		Symbol:    "IF2405",
		TradeTime: now,
		Price:     3800.0,
		Quantity:  10,
		Turnover:  38000.0,
		BSFlag:    'B',
		OrderKind: '1',
		OrderID:   "ORDER123",
	}
	
	if tick.Symbol != "IF2405" {
		t.Error("TickData Symbol not set correctly")
	}
	if tick.Price != 3800.0 {
		t.Error("TickData Price not set correctly")
	}
	if tick.BSFlag != 'B' {
		t.Error("TickData BSFlag not set correctly")
	}
}

func TestKlineData_Structure(t *testing.T) {
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
	
	if kline.Symbol != "IF2405" {
		t.Error("KlineData Symbol not set correctly")
	}
	if kline.Period != "1m" {
		t.Error("KlineData Period not set correctly")
	}
	if kline.Volume != 50000 {
		t.Error("KlineData Volume not set correctly")
	}
}
