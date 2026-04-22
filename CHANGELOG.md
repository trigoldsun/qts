# Changelog - QTS (Quantitative Trading System)

## [1.0.0-alpha.1] - 2026-04-22

### Added
- **Sprint 1: Foundation & Core Framework**
  - Repository structure initialization with CI/CD pipeline
  - Docker templates for all services
  - Unit test framework (JUnit5+Mockito for Java, Jest for Node.js, pytest for Python)

- **BIZ-TRADE Module** (Trade Core)
  - OrderCommand and OrderStateMachine implementation
  - AssetManager for fund management
  - PositionManager for position tracking
  - ExchangeAdapter and TradeReporter integration

- **BIZ-RISK Module** (Risk Engine)
  - Risk rule engine with Micrometer metrics
  - Real-time risk monitoring
  - Circuit breaker mechanism
  - Test compatibility fixes

- **BFF-WEB Module** (Web Frontend Backend)
  - Trade API endpoints implementation
  - Risk status panel with WebSocket alerts

### Services Implemented
- api-gateway (Node.js)
- auth-service (Go)
- user-service (Go)
- product-service (Go)
- order-service (Go)
- payment-service (Go)
- notification-service (Go)
- search-service (Go)
- analytics-service (Node.js)
- file-service (Node.js)
- admin-service (Java/Spring Boot)
- biz-trade (Go)
- biz-risk (Java/Spring Boot)

### Infrastructure
- Kubernetes manifests directory (pending)
- Logging configuration (pending)
- Monitoring setup (pending)
- Terraform scripts (pending)

### Known Issues
- BIZ-MARKET (Market Data) not yet implemented - deferred to Beta
- Infrastructure deployment manifests are placeholder - require completion
