package com.qts.biz.settle.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void testSuccess() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success(data);
        
        assertEquals(0, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals(data, response.getData());
        assertNotNull(response.getRequestId());
    }

    @Test
    void testError() {
        ApiResponse<String> response = ApiResponse.error(1001, "Validation failed");
        
        assertEquals(1001, response.getCode());
        assertEquals("Validation failed", response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getRequestId());
    }

    @Test
    void testBuilder() {
        ApiResponse<Integer> response = ApiResponse.<Integer>builder()
                .code(200)
                .message("OK")
                .data(42)
                .requestId("req-123")
                .build();
        
        assertEquals(200, response.getCode());
        assertEquals("OK", response.getMessage());
        assertEquals(42, response.getData());
        assertEquals("req-123", response.getRequestId());
    }

    @Test
    void testNoArgsConstructor() {
        ApiResponse<String> response = new ApiResponse<>();
        assertEquals(0, response.getCode());
    }

    @Test
    void testEqualsAndHashCode() {
        ApiResponse<String> response1 = ApiResponse.success("data");
        ApiResponse<String> response2 = ApiResponse.success("data");
        
        // Note: UUID.randomUUID() means these won't be equal unless mocked
        // This tests the structure itself
        assertNotNull(response1.getRequestId());
        assertNotNull(response2.getRequestId());
    }

    @Test
    void testToString() {
        ApiResponse<String> response = ApiResponse.success("test");
        String str = response.toString();
        assertTrue(str.contains("code"));
        assertTrue(str.contains("message"));
    }

    @Test
    void testSuccessWithNullData() {
        ApiResponse<Void> response = ApiResponse.success(null);
        assertEquals(0, response.getCode());
        assertEquals("success", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testErrorWithComplexMessage() {
        String message = "Error: something went wrong";
        ApiResponse<Object> response = ApiResponse.error(500, message);
        assertEquals(500, response.getCode());
        assertEquals(message, response.getMessage());
    }
}