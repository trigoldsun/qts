package main

/*
BIZ-MARKET 实时行情服务

基于Go/Gin + SimNow MD-API

职责：
- 实时行情（WebSocket推送）
- 历史K线（1min/5min/日K）
- 行情订阅管理
- 行情中断监控

API清单：
- F-DATA-001: WS /v1/market/stream - 实时行情订阅
- F-DATA-002: GET /v1/market/quotes/{symbol} - 行情快照
- F-DATA-003: GET /v1/market/kline - K线查询
- F-DATA-004: GET /v1/market/symbols - 标的信息

SLA要求：
- P99延迟：行情推送 ≤ 1秒
- 可用性：≥ 99.9%
*/

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/qts/biz-market/internal/adapter"
	"github.com/qts/biz-market/internal/config"
	"github.com/qts/biz-market/internal/handler"
	"github.com/qts/biz-market/internal/service"

	"github.com/gin-gonic/gin"
)

func main() {
	// 加载配置
	cfg := config.Load()

	// 创建上下文
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// 创建SimNow适配器
	marketAdapter := adapter.NewMarketDataAdapter(
		cfg.SimNow.FrontAddr,
		cfg.SimNow.BrokerID,
		cfg.SimNow.UserID,
		cfg.SimNow.Password,
	)

	// 创建行情服务
	marketSvc := service.NewMarketService(marketAdapter)

	// 启动服务
	if err := marketSvc.Start(ctx); err != nil {
		log.Fatalf("Failed to start market service: %v", err)
	}

	// 创建HTTP处理器
	httpHandler := handler.NewHTTPHandler(marketSvc)

	// 设置Gin模式
	gin.SetMode(gin.ReleaseMode)

	// 创建Gin引擎
	router := gin.New()
	router.Use(gin.Recovery())
	router.Use(gin.Logger())

	// 注册路由
	httpHandler.RegisterRoutes(router)

	// 创建HTTP服务器
	server := &http.Server{
		Addr:    fmt.Sprintf(":%d", cfg.Server.HTTPPort),
		Handler: router,
	}

	// 启动HTTP服务器
	go func() {
		log.Printf("Starting HTTP server on port %d", cfg.Server.HTTPPort)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("HTTP server error: %v", err)
		}
	}()

	// 等待信号
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)
	<-sigChan

	log.Println("Shutting down...")

	// 关闭上下文
	cancel()

	// 优雅关闭HTTP服务器
	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer shutdownCancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		log.Printf("HTTP server shutdown error: %v", err)
	}

	log.Println("Server stopped")
}
