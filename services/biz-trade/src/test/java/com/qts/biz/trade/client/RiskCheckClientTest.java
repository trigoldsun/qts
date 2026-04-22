package com.qts.biz.trade.client;

import com.qts.biz.trade.BaseTest;
import com.qts.biz.trade.config.RiskConfig;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RiskCheckClient
 * Tests risk checking functionality with mocked gRPC channel
 */
public class RiskCheckClientTest extends BaseTest {

    @Mock
    private ManagedChannel mockChannel;

    @Mock
    private RiskConfig mockRiskConfig;

    private RiskCheckClient riskCheckClient;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        when(mockRiskConfig.getTimeoutMs()).thenReturn(5000L);
        when(mockRiskConfig.getTarget()).thenReturn("risk-service:50051");
        riskCheckClient = new RiskCheckClient(mockChannel, mockRiskConfig);
    }

    @Test
    @DisplayName("Risk check should pass for valid request")
    void testCheckRisk_Pass() {
        // Given
        Long accountId = 1001L;
        String symbol = "BTC-USDT";
        String side = "BUY";
        Double price = 50000.0;
        Double quantity = 1.5;

        // When
        RiskCheckClient.RiskCheckResult result = riskCheckClient.checkRisk(
            accountId, symbol, side, price, quantity);

        // Then
        assertTrue(result.isPassed(), "Risk check should pass for valid request");
        assertEquals(accountId, result.getAccountId());
        assertEquals("Risk check passed", result.getMessage());
        assertNull(result.getRejectCode());
    }

    @Test
    @DisplayName("Risk check should fail for invalid account")
    void testCheckRisk_FailOnException() {
        // Given
        Long accountId = -1L;
        String symbol = "BTC-USDT";
        String side = "BUY";
        Double price = 50000.0;
        Double quantity = 1.5;

        // When
        RiskCheckClient.RiskCheckResult result = riskCheckClient.checkRisk(
            accountId, symbol, side, price, quantity);

        // Then
        assertFalse(result.isPassed(), "Risk check should fail for invalid account");
        assertNotNull(result.getMessage());
    }

    @Test
    @DisplayName("Shutdown should close channel gracefully")
    void testShutdown() throws InterruptedException {
        // Given
        when(mockChannel.shutdown()).thenReturn(mock(io.grpc.ManagedChannel.class));
        when(mockChannel.shutdown().awaitTermination(anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

        // When
        riskCheckClient.shutdown();

        // Then
        verify(mockChannel, times(1)).shutdown();
    }

    @Test
    @DisplayName("RiskCheckResult getters and setters should work correctly")
    void testRiskCheckResult() {
        // Given
        RiskCheckClient.RiskCheckResult result = new RiskCheckClient.RiskCheckResult();

        // When
        result.setPassed(true);
        result.setAccountId(12345L);
        result.setMessage("Test message");
        result.setRejectCode("REJECT001");

        // Then
        assertTrue(result.isPassed());
        assertEquals(12345L, result.getAccountId());
        assertEquals("Test message", result.getMessage());
        assertEquals("REJECT001", result.getRejectCode());
    }
}
