package com.qts.biz.trade.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * gRPC Client Configuration
 * Centralized configuration for all gRPC clients used by biz-trade service
 */
@Configuration
public class GrpcClientConfig {

    @Value("${qts.grpc.shutdown-timeout-seconds:10}")
    private int shutdownTimeoutSeconds;

    @Bean
    public ManagedChannel uamChannel(UamConfig uamConfig) {
        return ManagedChannelBuilder
                .forTarget(uamConfig.getTarget())
                .usePlaintext()
                .build();
    }

    @Bean
    public ManagedChannel smsChannel(SmsConfig smsConfig) {
        return ManagedChannelBuilder
                .forTarget(smsConfig.getTarget())
                .usePlaintext()
                .build();
    }

    @Bean
    public ManagedChannel riskChannel(RiskConfig riskConfig) {
        return ManagedChannelBuilder
                .forTarget(riskConfig.getTarget())
                .usePlaintext()
                .build();
    }

    @Bean
    public ManagedChannel accountChannel(AccountConfig accountConfig) {
        return ManagedChannelBuilder
                .forTarget(accountConfig.getTarget())
                .usePlaintext()
                .build();
    }

    @Bean
    public ManagedChannel assetChannel(AssetConfig assetConfig) {
        return ManagedChannelBuilder
                .forTarget(assetConfig.getTarget())
                .usePlaintext()
                .build();
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }
}
