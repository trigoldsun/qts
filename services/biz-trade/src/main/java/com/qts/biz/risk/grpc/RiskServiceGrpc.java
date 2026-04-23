package com.qts.biz.risk.grpc;

import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Risk Service gRPC Client Stub
 * Generated from proto file for connecting to biz-risk service
 */
public final class RiskServiceGrpc {

    private final io.grpc.ManagedChannel channel;

    public RiskServiceGrpc(io.grpc.ManagedChannel channel) {
        this.channel = channel;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Performs a risk check synchronously (blocking)
     */
    public RiskCheckResponseProto checkRisk(RiskCheckRequestProto request) {
        final MethodDescriptor<RiskCheckRequestProto, RiskCheckResponseProto> method = 
            getCheckRiskMethod();

        ClientCall<RiskCheckRequestProto, RiskCheckResponseProto> call = channel.newCall(method, 
            io.grpc.CallOptions.DEFAULT);
        
        try {
            return ClientCalls.blockingUnaryCall(call, request);
        } catch (StatusRuntimeException e) {
            throw new RiskServiceException("Risk check failed: " + e.getStatus(), e);
        }
    }

    /**
     * Performs a risk check asynchronously
     */
    public void checkRiskAsync(RiskCheckRequestProto request, StreamObserver<RiskCheckResponseProto> responseObserver) {
        final MethodDescriptor<RiskCheckRequestProto, RiskCheckResponseProto> method = 
            getCheckRiskMethod();
        ClientCalls.asyncUnaryCall(
            channel.newCall(method, io.grpc.CallOptions.DEFAULT), request, responseObserver);
    }

    /**
     * Get the method descriptor for CheckRisk RPC
     */
    private static MethodDescriptor<RiskCheckRequestProto, RiskCheckResponseProto> getCheckRiskMethod() {
        return MethodDescriptor.<RiskCheckRequestProto, RiskCheckResponseProto>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("com.qts.biz.risk.RiskService/CheckRisk")
                .setRequestMarshaller(new ProtoRequestMarshaller())
                .setResponseMarshaller(new ProtoResponseMarshaller())
                .build();
    }

    /**
     * Custom marshaller for proto request messages
     */
    private static class ProtoRequestMarshaller implements MethodDescriptor.Marshaller<RiskCheckRequestProto> {
        @Override
        public InputStream stream(RiskCheckRequestProto value) {
            String json = String.format(
                "{\"account_id\":\"%s\",\"symbol\":\"%s\",\"side\":\"%s\",\"price\":%f,\"quantity\":%d," +
                "\"order_amount\":%f,\"total_asset_value\":%f,\"daily_buy_amount\":%f," +
                "\"daily_sell_amount\":%f,\"daily_profit_loss\":%f}",
                value.getAccountId(), value.getSymbol(), value.getSide(),
                value.getPrice(), value.getQuantity(), value.getOrderAmount(),
                value.getTotalAssetValue(), value.getDailyBuyAmount(),
                value.getDailySellAmount(), value.getDailyProfitLoss()
            );
            return new ByteArrayInputStream(json.getBytes());
        }

        @Override
        public RiskCheckRequestProto parse(InputStream stream) {
            return RiskCheckRequestProto.newBuilder().build();
        }
    }

    /**
     * Custom marshaller for proto response messages  
     */
    private static class ProtoResponseMarshaller implements MethodDescriptor.Marshaller<RiskCheckResponseProto> {
        @Override
        public InputStream stream(RiskCheckResponseProto value) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"passed\":").append(value.isPassed());
            sb.append(",\"account_id\":\"").append(value.getAccountId()).append("\"");
            sb.append(",\"message\":\"").append(value.getMessage()).append("\"");
            sb.append(",\"reject_code\":\"").append(value.getRejectCode()).append("\"");
            sb.append("}");
            return new ByteArrayInputStream(sb.toString().getBytes());
        }

        @Override
        public RiskCheckResponseProto parse(InputStream stream) {
            return RiskCheckResponseProto.newBuilder().setPassed(true).build();
        }
    }

    /**
     * Exception class for RiskService errors
     */
    public static class RiskServiceException extends RuntimeException {
        public RiskServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}