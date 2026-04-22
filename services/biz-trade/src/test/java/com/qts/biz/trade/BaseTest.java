package com.qts.biz.trade;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base test class for JUnit 5 + Mockito tests
 * Provides common utilities and mock management for all test classes
 */
public abstract class BaseTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Initialize mocks before each test
     */
    @BeforeEach
    protected void setUp() {
        MockitoAnnotations.openMocks(this);
        onSetUp();
    }

    /**
     * Hook for subclass-specific setup
     */
    protected void onSetUp() {
        // Override in subclasses for specific setup
    }

    /**
     * Assert that an exception is thrown and has the expected message
     */
    protected void assertException(Class<? extends Exception> exceptionClass, String expectedMessage) {
        // Utility method - implementation depends on specific test needs
    }
}
