#!/bin/bash
# QTS Docker Compose Grouped Start/Stop Script
# Resource-constrained environment (1.3GB RAM)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yaml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

show_help() {
    cat << EOF
QTS Docker Compose Management Script
Usage: $0 <command> [group]

Commands:
    start-infra     Start infrastructure (postgres, redis)
    start-core      Start core services (api-gateway, auth, bff-web)
    start-trading   Start trading services (biz-trade, biz-risk, order)
    start-market    Start market services (market-service)
    start-all       Start all services
    stop-infra      Stop infrastructure
    stop-core       Stop core services
    stop-trading    Stop trading services
    stop-market     Stop market services
    stop-all        Stop all services
    restart <group> Restart a specific group
    status          Show service status
    logs <service>  Show logs for a service

Examples:
    $0 start-infra
    $0 start-core
    $0 start-trading
    $0 start-market
    $0 stop-all
EOF
}

# Start infrastructure
start_infra() {
    log_info "Starting infrastructure services..."
    docker compose -f "$COMPOSE_FILE" up -d postgres redis
    log_info "Waiting for infrastructure to be ready..."
    sleep 10
    docker compose -f "$COMPOSE_FILE" ps
}

# Start core group
start_core() {
    log_info "Starting core services..."
    docker compose -f "$COMPOSE_FILE" up -d api-gateway auth-service bff-web
    log_info "Waiting for core services to be ready..."
    sleep 15
    docker compose -f "$COMPOSE_FILE" ps
}

# Start trading group
start_trading() {
    log_info "Starting trading services..."
    docker compose -f "$COMPOSE_FILE" up -d biz-trade biz-risk order-service
    log_info "Waiting for trading services to be ready..."
    sleep 20
    docker compose -f "$COMPOSE_FILE" ps
}

# Start market group
start_market() {
    log_info "Starting market services..."
    docker compose -f "$COMPOSE_FILE" up -d market-service
    log_info "Waiting for market services to be ready..."
    sleep 10
    docker compose -f "$COMPOSE_FILE" ps
}

# Stop infrastructure
stop_infra() {
    log_warn "Stopping infrastructure (will also stop dependent services)..."
    docker compose -f "$COMPOSE_FILE" stop postgres redis
}

# Stop core group
stop_core() {
    log_info "Stopping core services..."
    docker compose -f "$COMPOSE_FILE" stop api-gateway auth-service bff-web
}

# Stop trading group
stop_trading() {
    log_info "Stopping trading services..."
    docker compose -f "$COMPOSE_FILE" stop biz-trade biz-risk order-service
}

# Stop market group
stop_market() {
    log_info "Stopping market services..."
    docker compose -f "$COMPOSE_FILE" stop market-service
}

# Start all
start_all() {
    log_info "Starting all services..."
    start_infra
    start_core
    start_trading
    start_market
    log_info "All services started"
    docker compose -f "$COMPOSE_FILE" ps
}

# Stop all
stop_all() {
    log_warn "Stopping all services..."
    docker compose -f "$COMPOSE_FILE" down
}

# Restart group
restart_group() {
    local group="$1"
    case "$group" in
        infra) stop_infra && start_infra ;;
        core) stop_core && start_core ;;
        trading) stop_trading && start_trading ;;
        market) stop_market && start_market ;;
        all) stop_all && start_all ;;
        *) log_error "Unknown group: $group" && show_help && exit 1 ;;
    esac
}

# Show status
show_status() {
    docker compose -f "$COMPOSE_FILE" ps
}

# Show logs
show_logs() {
    local service="$1"
    if [ -z "$service" ]; then
        log_error "Please specify a service name"
        exit 1
    fi
    docker compose -f "$COMPOSE_FILE" logs -f --tail=100 "$service"
}

# Main
COMMAND="${1:-help}"
GROUP="${2:-}"

case "$COMMAND" in
    start-infra) start_infra ;;
    start-core) start_core ;;
    start-trading) start_trading ;;
    start-market) start_market ;;
    start-all) start_all ;;
    stop-infra) stop_infra ;;
    stop-core) stop_core ;;
    stop-trading) stop_trading ;;
    stop-market) stop_market ;;
    stop-all) stop_all ;;
    restart) restart_group "$GROUP" ;;
    status) show_status ;;
    logs) show_logs "$GROUP" ;;
    help|--help|-h) show_help ;;
    *) log_error "Unknown command: $COMMAND" && show_help && exit 1 ;;
esac
