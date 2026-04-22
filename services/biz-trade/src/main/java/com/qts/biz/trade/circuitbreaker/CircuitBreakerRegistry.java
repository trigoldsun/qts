package com.qts.biz.trade.circuitbreaker;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Registry for managing CircuitBreaker instances.
 * Provides centralized access to all circuit breakers in the application.
 */
@Component
public class CircuitBreakerRegistry {
    
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    /**
     * Get or create a circuit breaker with the given name and config
     */
    public CircuitBreaker getOrCreate(String name, CircuitBreakerConfig config) {
        return circuitBreakers.computeIfAbsent(name, 
            key -> new CircuitBreaker(key, config));
    }
    
    /**
     * Get or create a circuit breaker with default config
     */
    public CircuitBreaker getOrCreate(String name) {
        return getOrCreate(name, CircuitBreakerConfig.builder().build());
    }
    
    /**
     * Get a circuit breaker by name, throws if not found
     */
    public CircuitBreaker get(String name) {
        CircuitBreaker cb = circuitBreakers.get(name);
        if (cb == null) {
            throw new IllegalStateException("CircuitBreaker '" + name + "' not found");
        }
        return cb;
    }
    
    /**
     * Get all registered circuit breakers
     */
    public Map<String, CircuitBreaker> getAllCircuitBreakers() {
        return new ConcurrentHashMap<>(circuitBreakers);
    }
    
    /**
     * Remove a circuit breaker from the registry
     */
    public void remove(String name) {
        circuitBreakers.remove(name);
    }
    
    /**
     * Clear all circuit breakers
     */
    public void clear() {
        circuitBreakers.clear();
    }
    
    /**
     * Get the count of registered circuit breakers
     */
    public int size() {
        return circuitBreakers.size();
    }
    
    /**
     * Check if a circuit breaker exists
     */
    public boolean exists(String name) {
        return circuitBreakers.containsKey(name);
    }
}
