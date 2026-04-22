package com.qts.circuitbreaker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerConfig {
    @Builder.Default
    private int failureRateThreshold = 50;
    @Builder.Default
    private int slowCallDurationMs = 100;
    @Builder.Default
    private int slowCallRate = 50;
    @Builder.Default
    private int circuitOpenDuration = 30;
    @Builder.Default
    private int minimumNumberOfCalls = 10;
    @Builder.Default
    private int slidingWindowSize = 60;
}
