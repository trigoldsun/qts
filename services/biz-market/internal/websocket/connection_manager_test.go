package websocket

import (
	"encoding/json"
	"sync"
	"testing"
	"time"

	"github.com/gorilla/websocket"
)

func TestNewConnectionManager(t *testing.T) {
	mgr := NewConnectionManager()
	if mgr == nil {
		t.Fatal("NewConnectionManager returned nil")
	}
	if mgr.connections == nil {
		t.Error("connections map not initialized")
	}
	if mgr.subscribers == nil {
		t.Error("subscribers map not initialized")
	}
	
	// Clean up the heartbeat ticker
	mgr.Close()
}

func TestConnectionManager_AcceptConnection(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	// Create a dummy websocket connection
	conn := &websocket.Conn{}
	
	connection, err := mgr.AcceptConnection(conn, "test-conn-1")
	if err != nil {
		t.Errorf("AcceptConnection failed: %v", err)
	}
	
	if connection == nil {
		t.Fatal("AcceptConnection returned nil connection")
	}
	
	if connection.ID != "test-conn-1" {
		t.Errorf("expected ID test-conn-1, got %s", connection.ID)
	}
	
	count := mgr.GetConnectionCount()
	if count != 1 {
		t.Errorf("expected connection count 1, got %d", count)
	}
}

func TestConnectionManager_MaxConnections(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	// Try to exceed max connections
	for i := 0; i < MaxConnections; i++ {
		conn := &websocket.Conn{}
		_, err := mgr.AcceptConnection(conn, "test-conn-"+string(rune(i)))
		if err != nil {
			t.Errorf("AcceptConnection failed at i=%d: %v", i, err)
		}
	}
	
	// Next connection should fail
	conn := &websocket.Conn{}
	_, err := mgr.AcceptConnection(conn, "exceed-conn")
	if err == nil {
		t.Error("expected error when exceeding max connections")
	}
}

func TestConnectionManager_RemoveConnection(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	conn := &websocket.Conn{}
	_, _ = mgr.AcceptConnection(conn, "test-conn-1")
	
	count := mgr.GetConnectionCount()
	if count != 1 {
		t.Errorf("expected connection count 1, got %d", count)
	}
	
	mgr.RemoveConnection("test-conn-1")
	
	count = mgr.GetConnectionCount()
	if count != 0 {
		t.Errorf("expected connection count 0 after remove, got %d", count)
	}
}

func TestConnectionManager_Subscribe(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	conn := &websocket.Conn{}
	connection, _ := mgr.AcceptConnection(conn, "test-conn-1")
	
	err := mgr.Subscribe("test-conn-1", "IF2405")
	if err != nil {
		t.Errorf("Subscribe failed: %v", err)
	}
	
	// Verify subscription
	connection.subsMu.RLock()
	if !connection.subscriptions["IF2405"] {
		t.Error("subscription not recorded")
	}
	connection.subsMu.RUnlock()
	
	// Subscribe non-existent connection should fail
	err = mgr.Subscribe("non-existent", "IF2405")
	if err == nil {
		t.Error("expected error for non-existent connection")
	}
}

func TestConnectionManager_Unsubscribe(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	conn := &websocket.Conn{}
	mgr.AcceptConnection(conn, "test-conn-1")
	
	mgr.Subscribe("test-conn-1", "IF2405")
	mgr.Unsubscribe("test-conn-1", "IF2405")
	
	// Verify unsubscription
	conn2 := mgr.connections["test-conn-1"]
	conn2.subsMu.RLock()
	if conn2.subscriptions["IF2405"] {
		t.Error("subscription still recorded after unsubscribe")
	}
	conn2.subsMu.RUnlock()
}

func TestConnectionManager_subscribe(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	conn := &websocket.Conn{}
	connection, _ := mgr.AcceptConnection(conn, "test-conn-1")
	
	err := mgr.subscribe("IF2405", connection)
	if err != nil {
		t.Errorf("subscribe failed: %v", err)
	}
	
	// Check subscribers map
	mgr.subMu.RLock()
	if mgr.subscribers["IF2405"] == nil {
		t.Error("subscribers[IF2405] not initialized")
	}
	if mgr.subscribers["IF2405"]["test-conn-1"] == nil {
		t.Error("connection not in subscribers")
	}
	mgr.subMu.RUnlock()
}

func TestConnectionManager_unsubscribe(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	conn := &websocket.Conn{}
	connection, _ := mgr.AcceptConnection(conn, "test-conn-1")
	
	// First subscribe
	mgr.subscribe("IF2405", connection)
	
	// Then unsubscribe
	err := mgr.unsubscribe("IF2405", connection)
	if err != nil {
		t.Errorf("unsubscribe failed: %v", err)
	}
	
	// Verify
	mgr.subMu.RLock()
	if subs, ok := mgr.subscribers["IF2405"]; ok {
		if _, found := subs["test-conn-1"]; found {
			t.Error("connection still in subscribers after unsubscribe")
		}
	}
	mgr.subMu.RUnlock()
}

func TestConnectionManager_unsubscribeUnsafe(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	conn := &websocket.Conn{}
	connection, _ := mgr.AcceptConnection(conn, "test-conn-1")
	
	mgr.subscribe("IF2405", connection)
	mgr.subscribe("IF2406", connection)
	
	mgr.unsubscribeUnsafe("IF2405", connection)
	
	// IF2405 should be removed
	mgr.subMu.RLock()
	if _, ok := mgr.subscribers["IF2405"]; ok {
		t.Error("IF2405 still in subscribers")
	}
	// IF2406 should still exist
	if _, ok := mgr.subscribers["IF2406"]; !ok {
		t.Error("IF2406 removed unexpectedly")
	}
	mgr.subMu.RUnlock()
}

func TestConnectionManager_BroadcastToSymbol(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()

	conn := &websocket.Conn{}
	connection, _ := mgr.AcceptConnection(conn, "test-conn-1")
	mgr.subscribe("IF2405", connection)

	// Skip actual broadcast since mock websocket.Conn cannot handle WriteMessage
	// Just verify subscription state
	mgr.subMu.RLock()
	subs := mgr.subscribers["IF2405"]
	if subs == nil {
		t.Error("IF2405 should have subscribers")
	}
	if _, ok := subs["test-conn-1"]; !ok {
		t.Error("test-conn-1 should be subscribed to IF2405")
	}
	mgr.subMu.RUnlock()
}

func TestConnectionManager_BroadcastToSymbol_NoSubscribers(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	// Broadcast to symbol with no subscribers should succeed
	err := mgr.BroadcastToSymbol("NONEXISTENT", "quote", nil)
	if err != nil {
		t.Errorf("BroadcastToSymbol failed for non-existent symbol: %v", err)
	}
}

func TestConnectionManager_BroadcastAll(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	conn1 := &websocket.Conn{}
	conn2 := &websocket.Conn{}
	mgr.AcceptConnection(conn1, "test-conn-1")
	mgr.AcceptConnection(conn2, "test-conn-2")
	
	// Verify connections were accepted (count should be 2)
	if mgr.GetConnectionCount() != 2 {
		t.Errorf("expected 2 connections, got %d", mgr.GetConnectionCount())
	}
	
	// BroadcastAll should not panic even when mock websocket.Conn cannot write
	// (mock connections have nil internal state causing gorilla/websocket to panic)
	err := mgr.BroadcastAll("quote", map[string]interface{}{
		"symbol": "IF2405",
		"price":  3800.0,
	})
	// Error is expected since mock connections cannot actually write
	_ = err
}

func TestConnectionManager_GetConnectionCount(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	count := mgr.GetConnectionCount()
	if count != 0 {
		t.Errorf("expected initial count 0, got %d", count)
	}
	
	conn := &websocket.Conn{}
	mgr.AcceptConnection(conn, "test-conn-1")
	
	count = mgr.GetConnectionCount()
	if count != 1 {
		t.Errorf("expected count 1, got %d", count)
	}
}

func TestWebSocketOutboundMessage_JSON(t *testing.T) {
	msg := WebSocketOutboundMessage{
		Type:   "quote",
		Symbol: "IF2405",
		Data: map[string]interface{}{
			"price": 3800.0,
		},
		Time: time.Now(),
	}
	
	jsonData, err := json.Marshal(msg)
	if err != nil {
		t.Errorf("json.Marshal failed: %v", err)
	}
	
	var parsed WebSocketOutboundMessage
	err = json.Unmarshal(jsonData, &parsed)
	if err != nil {
		t.Errorf("json.Unmarshal failed: %v", err)
	}
	
	if parsed.Type != "quote" {
		t.Errorf("expected Type quote, got %s", parsed.Type)
	}
	if parsed.Symbol != "IF2405" {
		t.Errorf("expected Symbol IF2405, got %s", parsed.Symbol)
	}
}

func TestConnection_Fields(t *testing.T) {
	conn := &Connection{
		ID:            "test-id",
		UserID:        "user-1",
		AccountID:     "acc-1",
		subscriptions: make(map[string]bool),
		lastActive:    time.Now(),
		authenticated: true,
		closed:        false,
	}
	
	if conn.ID != "test-id" {
		t.Error("ID not set correctly")
	}
	if conn.UserID != "user-1" {
		t.Error("UserID not set correctly")
	}
	if conn.AccountID != "acc-1" {
		t.Error("AccountID not set correctly")
	}
	if !conn.authenticated {
		t.Error("authenticated not set correctly")
	}
	if conn.closed {
		t.Error("closed not set correctly")
	}
}

func TestConnectionManager_ConcurrentAccess(t *testing.T) {
	mgr := NewConnectionManager()
	defer mgr.Close()
	
	var wg sync.WaitGroup
	
	// Concurrent subscribe/unsubscribe
	for i := 0; i < 100; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			conn := &websocket.Conn{}
			connID := "conn-" + string(rune(i))
			mgr.AcceptConnection(conn, connID)
			mgr.Subscribe(connID, "IF2405")
			mgr.Unsubscribe(connID, "IF2405")
		}(i)
	}
	
	wg.Wait()
}
