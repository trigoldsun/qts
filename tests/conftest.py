"""
Pytest configuration and fixtures for QTS integration tests

This module provides common fixtures and setup for integration testing
across multiple services.
"""

import pytest
import os
from typing import Generator, Dict, Any


# Test environment configuration
@pytest.fixture(scope="session")
def test_config() -> Dict[str, Any]:
    """Provide test configuration settings"""
    return {
        "api_gateway_url": os.getenv("API_GATEWAY_URL", "http://localhost:8080"),
        "trade_service_url": os.getenv("TRADE_SERVICE_URL", "http://localhost:8081"),
        "auth_service_url": os.getenv("AUTH_SERVICE_URL", "http://localhost:8082"),
        "database_url": os.getenv("DATABASE_URL", "postgresql://test:test@localhost:5432/test_db"),
        "redis_url": os.getenv("REDIS_URL", "redis://localhost:6379/0"),
        "kafka_brokers": os.getenv("KAFKA_BROKERS", "localhost:9092"),
        "timeout": 30,
    }


@pytest.fixture(scope="session")
def auth_token() -> str:
    """Provide authentication token for API requests"""
    # In real tests, this would authenticate and return a real token
    return "test-token-placeholder"


@pytest.fixture
def api_headers(auth_token: str) -> Dict[str, str]:
    """Provide standard API headers with authentication"""
    return {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {auth_token}",
        "X-Request-ID": "test-request-id",
    }


@pytest.fixture
def sample_order_data() -> Dict[str, Any]:
    """Provide sample order data for testing"""
    return {
        "account_id": "ACC001",
        "symbol": "BTC-USDT",
        "side": "BUY",
        "price": 50000.0,
        "quantity": 1.5,
        "order_type": "LIMIT",
        "time_in_force": "GTC",
    }


@pytest.fixture
def sample_cancel_data() -> Dict[str, Any]:
    """Provide sample cancel request data"""
    return {
        "order_id": "ORD123",
        "reason": "User requested cancellation",
    }


# Database fixtures (for integration tests)
@pytest.fixture(scope="session")
def db_connection(test_config: Dict[str, Any]):
    """
    Provide database connection for integration tests
    Note: Requires postgres service to be running
    """
    # This would establish a real database connection in integration tests
    # For unit tests, this fixture can be mocked
    pass


@pytest.fixture
def clean_db(db_connection):
    """
    Clean database before each test
    Note: Only use in integration tests with actual database
    """
    # Setup: Clean relevant tables
    # db_connection.execute("TRUNCATE TABLE orders RESTART IDENTITY CASCADE")
    yield
    # Teardown: Clean up after test
    pass


# Kafka fixtures (for integration tests)
@pytest.fixture(scope="session")
def kafka_producer(test_config: Dict[str, Any]):
    """
    Provide Kafka producer for event publishing tests
    Note: Requires kafka service to be running
    """
    # This would create a real Kafka producer in integration tests
    pass


# Redis fixtures (for caching tests)
@pytest.fixture
def redis_client(test_config: Dict[str, Any]):
    """
    Provide Redis client for caching tests
    Note: Requires redis service to be running
    """
    # This would create a real Redis client in integration tests
    pass


# Mock fixtures for unit tests
@pytest.fixture
def mock_trade_service():
    """Provide mock trade service for controller tests"""
    from unittest.mock import Mock
    mock = Mock()
    mock.create_order.return_value = {
        "success": True,
        "order_id": "ORD123",
        "data": {},
    }
    mock.cancel_order.return_value = {"success": True}
    mock.get_order.return_value = {"order_id": "ORD123", "status": "FILLED"}
    mock.get_orders.return_value = {"orders": [], "pagination": {}}
    return mock


@pytest.fixture
def mock_risk_client():
    """Provide mock risk check client"""
    from unittest.mock import Mock
    mock = Mock()
    mock.check_risk.return_value = {
        "passed": True,
        "account_id": "ACC001",
        "message": "Risk check passed",
    }
    return mock


# Helper functions
def create_test_request(method: str, path: str, data: Dict[str, Any] = None) -> Dict[str, Any]:
    """Helper to create test request objects"""
    return {
        "method": method,
        "path": path,
        "body": data,
        "headers": {},
    }


def assert_response_success(response: Dict[str, Any]) -> None:
    """Assert that API response indicates success"""
    assert response.get("success") is True, f"Expected success response, got: {response}"


def assert_response_error(response: Dict[str, Any], expected_code: str = None) -> None:
    """Assert that API response indicates error"""
    assert response.get("success") is False, f"Expected error response, got: {response}"
    if expected_code:
        assert response.get("error", {}).get("code") == expected_code
