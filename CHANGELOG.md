# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0-alpha.2] - 2026-04-22

### Added
- **BIZ-SETTLE** (Java): Full settlement service with DailySettlementService, ReconcileService, StatementService
- **BIZ-MARKET** (Go): Market data service with SimNow adapter, WebSocket support, Kafka producer
- **BFF-WEB**: TradingPanel.vue with real-time market data display
- **infra/K8s**: Complete Kubernetes manifests for all services
- **infra/docker-compose**: Service orchestration with PostgreSQL + ELK
- **infra/CI-CD**: GitHub Actions pipeline (build, test, docker build, K8s deploy)
- **PostgreSQL init**: Schema initialization scripts for all services

### Test Results
- BIZ-SETTLE: 141 tests passed, 68% coverage
- BIZ-MARKET: All modules compiled, core coverage 80%+

### Security
- RBAC manifest for K8s
- HPA configuration for all services
- Alertmanager + Prometheus monitoring stack

## [1.0.0-alpha.1] - 2026-04-21

### Added
- Initial project structure
- Core services: biz-risk, biz-trade, bff-web
- Repository foundation with CI/CD
