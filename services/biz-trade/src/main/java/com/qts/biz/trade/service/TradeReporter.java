package com.qts.biz.trade.service;

import com.qts.biz.trade.dto.OrderReport;
import com.qts.biz.trade.dto.TradeDTO;
import com.qts.biz.trade.dto.TradeReport;
import com.qts.biz.trade.entity.OrderEntity;
import com.qts.biz.trade.enums.OrderSide;
import com.qts.biz.trade.enums.OrderStatus;
import com.qts.biz.trade.event.TradeExecutedEvent;
import com.qts.biz.trade.repository.OrderRepository;
import com.qts.biz.trade.statemachine.OrderStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Trade Reporter Service
 * 处理成交回报：
 * 1. 解析回报数据
 * 2. 更新订单状态（调用OrderStateMachine.fill()）
 * 3. 更新持仓（调用PositionManager.addPosition/reducePosition）
 * 4. 更新资金（调用AssetManager.updateAssetFromTrade）
 * 5. 发布TradeExecutedEvent（Kafka）
 */
@Service
public class TradeReporter {

    private static final Logger logger = LoggerFactory.getLogger(TradeReporter.class);
    private static final String TOPIC_TRADE_EXECUTED = "trade-executed";

    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;
    private final PositionManager positionManager;
    private final AssetManager assetManager;
    private final KafkaTemplate<String, TradeExecutedEvent> kafkaTemplate;

    @Autowired
    public TradeReporter(OrderRepository orderRepository,
                        OrderStateMachine orderStateMachine,
                        PositionManager positionManager,
                        AssetManager assetManager,
                        KafkaTemplate<String, TradeExecutedEvent> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.orderStateMachine = orderStateMachine;
        this.positionManager = positionManager;
        this.assetManager = assetManager;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Process trade report from exchange
     */
    @Transactional
    public void processTradeReport(TradeReport tradeReport) {
        logger.info("Processing trade report: tradeId={}, orderId={}, symbol={}, side={}, qty={}, price={}",
                tradeReport.getTradeId(), tradeReport.getOrderId(), tradeReport.getSymbol(),
                tradeReport.getSide(), tradeReport.getQuantity(), tradeReport.getPrice());

        try {
            // 1. Parse and validate trade report
            if (!validateTradeReport(tradeReport)) {
                logger.error("Invalid trade report: {}", tradeReport);
                return;
            }

            // 2. Find the order
            Optional<OrderEntity> orderOpt = findOrderById(tradeReport.getOrderId());
            if (orderOpt.isEmpty()) {
                logger.error("Order not found for trade report: orderId={}", tradeReport.getOrderId());
                return;
            }

            OrderEntity order = orderOpt.get();

            // 3. Convert to TradeDTO for internal processing
            TradeDTO tradeDTO = convertToTradeDTO(tradeReport);

            // 4. Update order status via OrderStateMachine
            updateOrderStatus(order, tradeReport, tradeDTO);

            // 5. Update position
            updatePosition(tradeDTO);

            // 6. Update asset
            updateAsset(tradeDTO);

            // 7. Publish TradeExecutedEvent to Kafka
            publishTradeExecutedEvent(tradeDTO);

            logger.info("Trade report processed successfully: tradeId={}", tradeReport.getTradeId());
        } catch (Exception e) {
            logger.error("Failed to process trade report: tradeId={}, error={}",
                    tradeReport.getTradeId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Process order report from exchange
     */
    @Transactional
    public void processOrderReport(OrderReport orderReport) {
        logger.info("Processing order report: orderId={}, status={}, filledQty={}",
                orderReport.getOrderId(), orderReport.getStatus(), orderReport.getFilledQuantity());

        try {
            // Find the order
            Optional<OrderEntity> orderOpt = findOrderById(orderReport.getOrderId());
            if (orderOpt.isEmpty()) {
                logger.error("Order not found for order report: orderId={}", orderReport.getOrderId());
                return;
            }

            OrderEntity order = orderOpt.get();

            // Update order based on status
            switch (orderReport.getStatus()) {
                case SUBMITTED:
                    // Already submitted, nothing to do
                    break;
                case PARTIAL_FILLED:
                    orderStateMachine.partialFill(order, orderReport.getFilledQuantity());
                    break;
                case FILLED:
                    orderStateMachine.fill(order, orderReport.getFilledQuantity(), orderReport.getAvgPrice());
                    break;
                case CANCELLED:
                    orderStateMachine.cancel(order);
                    break;
                case REJECTED:
                    orderStateMachine.reject(order, orderReport.getErrorMessage());
                    break;
                default:
                    logger.warn("Unknown order status: {}", orderReport.getStatus());
            }

            logger.info("Order report processed successfully: orderId={}, status={}",
                    orderReport.getOrderId(), orderReport.getStatus());
        } catch (Exception e) {
            logger.error("Failed to process order report: orderId={}, error={}",
                    orderReport.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Validate trade report
     */
    private boolean validateTradeReport(TradeReport tradeReport) {
        if (tradeReport == null) {
            return false;
        }
        if (tradeReport.getTradeId() == null || tradeReport.getTradeId().isEmpty()) {
            logger.error("Trade report missing tradeId");
            return false;
        }
        if (tradeReport.getOrderId() == null || tradeReport.getOrderId().isEmpty()) {
            logger.error("Trade report missing orderId");
            return false;
        }
        if (tradeReport.getSymbol() == null || tradeReport.getSymbol().isEmpty()) {
            logger.error("Trade report missing symbol");
            return false;
        }
        if (tradeReport.getQuantity() == null || tradeReport.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Trade report invalid quantity");
            return false;
        }
        if (tradeReport.getPrice() == null || tradeReport.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Trade report invalid price");
            return false;
        }
        return true;
    }

    /**
     * Find order by orderId or clientOrderId
     */
    private Optional<OrderEntity> findOrderById(String orderId) {
        Optional<OrderEntity> orderOpt = orderRepository.findByOrderId(orderId);
        if (orderOpt.isEmpty()) {
            orderOpt = orderRepository.findByClientOrderId(orderId);
        }
        return orderOpt;
    }

    /**
     * Convert TradeReport to TradeDTO
     */
    private TradeDTO convertToTradeDTO(TradeReport tradeReport) {
        TradeDTO dto = new TradeDTO();
        dto.setTradeId(tradeReport.getTradeId());
        dto.setOrderId(tradeReport.getOrderId());
        
        // Convert accountId to Long if needed
        if (tradeReport.getAccountId() != null) {
            try {
                dto.setAccountId(Long.parseLong(tradeReport.getAccountId()));
            } catch (NumberFormatException e) {
                // Use as string if conversion fails
                dto.setAccountId(0L); // Default fallback
            }
        }
        
        dto.setSymbol(tradeReport.getSymbol());
        dto.setSide(tradeReport.getSide());
        dto.setPrice(tradeReport.getPrice());
        dto.setQuantity(tradeReport.getQuantity());
        dto.setAmount(tradeReport.getAmount());
        dto.setTradeTime(tradeReport.getTradeTime());
        return dto;
    }

    /**
     * Update order status via OrderStateMachine
     */
    private void updateOrderStatus(OrderEntity order, TradeReport tradeReport, TradeDTO tradeDTO) {
        OrderStatus currentStatus = order.getStatus();
        int currentFilledQty = order.getFilledQuantity() != null ? order.getFilledQuantity() : 0;
        int newlyFilledQty = tradeReport.getQuantity() != null ? tradeReport.getQuantity().intValue() : 0;
        int totalFilledQty = currentFilledQty + newlyFilledQty;

        if (totalFilledQty >= order.getQuantity()) {
            // Full fill
            double avgPrice = calculateAvgPrice(order, tradeReport);
            orderStateMachine.fill(order, totalFilledQty, avgPrice);
            logger.info("Order filled: orderId={}, totalFilledQty={}", order.getOrderId(), totalFilledQty);
        } else {
            // Partial fill
            orderStateMachine.partialFill(order, totalFilledQty);
            logger.info("Order partial filled: orderId={}, totalFilledQty={}", order.getOrderId(), totalFilledQty);
        }
    }

    /**
     * Calculate average price considering existing fills
     */
    private double calculateAvgPrice(OrderEntity order, TradeReport tradeReport) {
        BigDecimal tradePrice = tradeReport.getPrice();
        int tradeQty = tradeReport.getQuantity() != null ? tradeReport.getQuantity().intValue() : 0;
        
        if (order.getAvgPrice() == null || order.getFilledQuantity() == null || order.getFilledQuantity() == 0) {
            return tradePrice.doubleValue();
        }
        
        BigDecimal totalAmount = order.getAvgPrice()
                .multiply(BigDecimal.valueOf(order.getFilledQuantity()))
                .add(tradePrice.multiply(BigDecimal.valueOf(tradeQty)));
        int totalQty = order.getFilledQuantity() + tradeQty;
        
        return totalAmount.divide(BigDecimal.valueOf(totalQty), 4, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * Update position based on trade
     */
    private void updatePosition(TradeDTO trade) {
        try {
            if (trade.isBuy()) {
                positionManager.addPosition(trade);
                logger.info("Position added for trade: tradeId={}", trade.getTradeId());
            } else if (trade.isSell()) {
                positionManager.reducePosition(trade);
                logger.info("Position reduced for trade: tradeId={}", trade.getTradeId());
            }
        } catch (Exception e) {
            logger.error("Failed to update position: tradeId={}, error={}", trade.getTradeId(), e.getMessage());
            // Don't rethrow - position update failure shouldn't stop trade processing
        }
    }

    /**
     * Update asset based on trade
     */
    private void updateAsset(TradeDTO trade) {
        try {
            assetManager.updateAssetFromTrade(trade);
            logger.info("Asset updated for trade: tradeId={}", trade.getTradeId());
        } catch (Exception e) {
            logger.error("Failed to update asset: tradeId={}, error={}", trade.getTradeId(), e.getMessage());
            // Don't rethrow - asset update failure shouldn't stop trade processing
        }
    }

    /**
     * Publish TradeExecutedEvent to Kafka
     */
    private void publishTradeExecutedEvent(TradeDTO trade) {
        try {
            TradeExecutedEvent event = new TradeExecutedEvent(trade);
            kafkaTemplate.send(TOPIC_TRADE_EXECUTED, trade.getTradeId(), event);
            logger.info("TradeExecutedEvent published: tradeId={}", trade.getTradeId());
        } catch (Exception e) {
            logger.error("Failed to publish TradeExecutedEvent: tradeId={}, error={}",
                    trade.getTradeId(), e.getMessage());
            // Don't rethrow - kafka publish failure shouldn't stop trade processing
        }
    }
}
