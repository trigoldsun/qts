package com.qts.biz.trade.service;

import com.qts.biz.account.grpc.AccountServiceClient;
import com.qts.biz.asset.grpc.AssetServiceClient;
import com.qts.biz.trade.client.RiskCheckClient;
import com.qts.biz.trade.dto.*;
import com.qts.biz.trade.entity.OrderEntity;
import com.qts.biz.trade.enums.OrderSide;
import com.qts.biz.trade.enums.OrderStatus;
import com.qts.biz.trade.enums.OrderType;
import com.qts.biz.trade.event.OrderEventPublisher;
import com.qts.biz.trade.exception.BusinessException;
import com.qts.biz.trade.exception.OrderNotFoundException;
import com.qts.biz.trade.exception.OrderStateTransitionException;
import com.qts.biz.trade.repository.OrderRepository;
import com.qts.biz.trade.statemachine.OrderStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Order Command Service
 * Handles order placement, modification, and cancellation
 */
@Service
public class OrderCommandService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCommandService.class);

    // Error codes from API spec
    private static final int CODE_ACCOUNT_NOT_FOUND = 1001;
    private static final int CODE_INSUFFICIENT_FUNDS = 1002;
    private static final int CODE_POSITION_LIMIT_EXCEEDED = 1003;
    private static final int CODE_RISK_PRECHECK_FAILED = 1004;
    private static final int CODE_INVALID_SYMBOL = 1005;
    private static final int CODE_INVALID_PRICE = 1006;
    private static final int CODE_DUPLICATE_ORDER = 1007;
    private static final int CODE_ORDER_NOT_FOUND = 1008;
    private static final int CODE_ORDER_CANNOT_MODIFY = 1009;
    private static final int CODE_ORDER_CANNOT_CANCEL = 1010;
    private static final int CODE_VERSION_CONFLICT = 1011;
    private static final int CODE_ORDER_ALREADY_FILLED = 1012;

    private final OrderRepository orderRepository;
    private final OrderStateMachine stateMachine;
    private final OrderEventPublisher eventPublisher;
    private final RiskCheckClient riskCheckClient;
    private final AccountServiceClient accountServiceClient;
    private final AssetServiceClient assetServiceClient;

    @Autowired
    public OrderCommandService(OrderRepository orderRepository,
                               OrderStateMachine stateMachine,
                               OrderEventPublisher eventPublisher,
                               RiskCheckClient riskCheckClient,
                               AccountServiceClient accountServiceClient,
                               AssetServiceClient assetServiceClient) {
        this.orderRepository = orderRepository;
        this.stateMachine = stateMachine;
        this.eventPublisher = eventPublisher;
        this.riskCheckClient = riskCheckClient;
        this.accountServiceClient = accountServiceClient;
        this.assetServiceClient = assetServiceClient;
    }

    /**
     * Place a new order
     * 
     * Business logic:
     * 1. Parameter validation (quantity multiple of 100, price > 0, symbol 6 digits)
     * 2. Account existence validation
     * 3. Fund/position sufficiency check (via AssetManager/PositionManager)
     * 4. Risk pre-check (via RiskCheckClient, gRPC sync)
     * 5. Generate order_id (UUID)
     * 6. Initialize state machine (CREATED)
     * 7. Publish order creation event (Kafka)
     * 8. Return OrderDTO
     */
    @Transactional
    public OrderDTO placeOrder(PlaceOrderCmd cmd) {
        logger.info("Placing order for account {} symbol {} side {}", 
                    cmd.getAccountId(), cmd.getSymbol(), cmd.getSide());

        // 1. Parameter validation
        validatePlaceOrderParams(cmd);

        // 2. Account existence validation (mock - in real system call account service)
        validateAccountExists(cmd.getAccountId());

        // 3. Check fund sufficiency (mock - in real system call asset service)
        checkFundSufficiency(cmd);

        // 4. Risk pre-check via gRPC
        performRiskPrecheck(cmd);

        // 5. Check for duplicate order
        checkDuplicateOrder(cmd);

        // 6. Generate order ID and create order entity
        String orderId = UUID.randomUUID().toString();
        OrderEntity order = createOrderEntity(orderId, cmd);

        // 7. Save order and initialize state
        order.setStatus(OrderStatus.CREATED);
        order.setFilledQuantity(0);
        OrderEntity savedOrder = orderRepository.save(order);

        // 8. Publish order creation event
        eventPublisher.publishOrderCreatedEvent(savedOrder);

        logger.info("Order placed successfully: orderId={}, clientOrderId={}", 
                   savedOrder.getOrderId(), savedOrder.getClientOrderId());

        return toDTO(savedOrder);
    }

    /**
     * Modify an existing order
     */
    @Transactional
    public OrderDTO modifyOrder(ModifyOrderCmd cmd) {
        logger.info("Modifying order {} for account {}", cmd.getOrderId(), cmd.getAccountId());

        // Find order
        OrderEntity order = orderRepository.findByOrderId(cmd.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + cmd.getOrderId()));

        // Validate account matches
        if (!order.getAccountId().equals(cmd.getAccountId())) {
            throw new BusinessException(CODE_ORDER_NOT_FOUND, "Order not found for account");
        }

        // Check if order can be modified
        if (!stateMachine.canModify(order.getStatus())) {
            throw new BusinessException(CODE_ORDER_CANNOT_MODIFY, 
                    "Order cannot be modified in status: " + order.getStatus());
        }

        // Check version for optimistic locking
        if (!order.getVersion().equals(cmd.getModifyVersion())) {
            throw new BusinessException(CODE_VERSION_CONFLICT, "Version conflict, please retry");
        }

        // Check if already filled
        if (order.getStatus() == OrderStatus.FILLED) {
            throw new BusinessException(CODE_ORDER_ALREADY_FILLED, "Order already filled");
        }

        // Apply modifications
        if (cmd.getNewPrice() != null) {
            order.setPrice(cmd.getNewPrice());
        }
        if (cmd.getNewQuantity() != null) {
            validateQuantityMultipleOf100(cmd.getNewQuantity());
            order.setQuantity(cmd.getNewQuantity());
        }

        // Update submitted timestamp (re-queue)
        order.setSubmittedAt(LocalDateTime.now());

        OrderEntity savedOrder = orderRepository.save(order);

        logger.info("Order modified successfully: orderId={}", savedOrder.getOrderId());

        return toDTO(savedOrder);
    }

    /**
     * Cancel an order
     */
    @Transactional
    public CancelResult cancelOrder(CancelOrderCmd cmd) {
        logger.info("Cancelling order {} for account {}", cmd.getOrderId(), cmd.getAccountId());

        // Find order
        OrderEntity order = orderRepository.findByOrderId(cmd.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + cmd.getOrderId()));

        // Validate account matches
        if (!order.getAccountId().equals(cmd.getAccountId())) {
            throw new BusinessException(CODE_ORDER_NOT_FOUND, "Order not found for account");
        }

        // Check if order can be cancelled
        if (!stateMachine.canCancel(order.getStatus())) {
            throw new BusinessException(CODE_ORDER_CANNOT_CANCEL, 
                    "Order cannot be cancelled in status: " + order.getStatus());
        }

        // Perform cancellation through state machine
        OrderEntity cancelledOrder = stateMachine.cancel(order);

        CancelResult result = new CancelResult();
        result.setOrderId(cancelledOrder.getOrderId());
        result.setStatus(cancelledOrder.getStatus());
        result.setCancelledAt(cancelledOrder.getCancelledAt());

        logger.info("Order cancelled successfully: orderId={}", cancelledOrder.getOrderId());

        return result;
    }

    // ========== Validation Methods ==========

    private void validatePlaceOrderParams(PlaceOrderCmd cmd) {
        // Validate quantity is multiple of 100
        validateQuantityMultipleOf100(cmd.getQuantity());

        // Validate price > 0 for limit orders
        if (cmd.getOrderType() == OrderType.LIMIT) {
            if (cmd.getPrice() == null || cmd.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(CODE_INVALID_PRICE, "Price must be greater than 0 for limit orders");
            }
        }

        // Validate stop_price for stop orders
        if (cmd.getOrderType() == OrderType.STOP) {
            if (cmd.getStopPrice() == null || cmd.getStopPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(CODE_INVALID_PRICE, "Stop price must be greater than 0 for stop orders");
            }
        }

        // Validate symbol is 6 digit number
        if (cmd.getSymbol() == null || !cmd.getSymbol().matches("^[0-9]{6}$")) {
            throw new BusinessException(CODE_INVALID_SYMBOL, "Symbol must be 6 digit number");
        }
    }

    private void validateQuantityMultipleOf100(Integer quantity) {
        if (quantity == null || quantity % 100 != 0) {
            throw new BusinessException(CODE_INVALID_PRICE, "Quantity must be multiple of 100");
        }
    }

    private void validateAccountExists(String accountId) {
        // Validate account exists via gRPC
        if (accountId == null || accountId.isEmpty()) {
            throw new BusinessException(CODE_ACCOUNT_NOT_FOUND, "Account not found");
        }
        
        AccountServiceClient.AccountValidationResult result = 
            accountServiceClient.validateAccount(accountId);
        
        if (!result.isValid()) {
            logger.warn("Account validation failed for {}: {}", accountId, result.getMessage());
            throw new BusinessException(CODE_ACCOUNT_NOT_FOUND, 
                "Account validation failed: " + result.getMessage());
        }
    }

    private void checkFundSufficiency(PlaceOrderCmd cmd) {
        // Check fund sufficiency via gRPC
        double amount = cmd.getPrice() != null ? 
            cmd.getPrice().doubleValue() * cmd.getQuantity() : 0.0;
        
        AssetServiceClient.AssetCheckResult result = assetServiceClient.checkSufficiency(
            cmd.getAccountId(),
            cmd.getSymbol(),
            cmd.getSide().name(),
            amount,
            cmd.getQuantity() != null ? cmd.getQuantity() : 0
        );
        
        if (!result.isSufficient()) {
            logger.warn("Insufficient funds for account {}: {}", cmd.getAccountId(), result.getMessage());
            throw new BusinessException(CODE_INSUFFICIENT_FUNDS, 
                "Insufficient funds: " + result.getMessage());
        }
    }

    private void performRiskPrecheck(PlaceOrderCmd cmd) {
        // Call risk check service via gRPC
        RiskCheckClient.RiskCheckResult result = riskCheckClient.checkRisk(
                Long.parseLong(cmd.getAccountId()),
                cmd.getSymbol(),
                cmd.getSide().name(),
                cmd.getPrice() != null ? cmd.getPrice().doubleValue() : null,
                cmd.getQuantity() != null ? cmd.getQuantity().doubleValue() : null
        );

        if (!result.isPassed()) {
            logger.warn("Risk precheck failed for account {}: {}", cmd.getAccountId(), result.getMessage());
            throw new BusinessException(CODE_RISK_PRECHECK_FAILED, 
                    "Risk precheck failed: " + result.getMessage());
        }
    }

    private void checkDuplicateOrder(PlaceOrderCmd cmd) {
        if (orderRepository.existsByClientOrderIdAndAccountId(cmd.getClientOrderId(), cmd.getAccountId())) {
            throw new BusinessException(CODE_DUPLICATE_ORDER, "Duplicate order: client_order_id already exists");
        }
    }

    // ========== Helper Methods ==========

    private OrderEntity createOrderEntity(String orderId, PlaceOrderCmd cmd) {
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        order.setClientOrderId(cmd.getClientOrderId());
        order.setAccountId(cmd.getAccountId());
        order.setSymbol(cmd.getSymbol());
        order.setSide(cmd.getSide());
        order.setOrderType(cmd.getOrderType());
        order.setPrice(cmd.getPrice());
        order.setStopPrice(cmd.getStopPrice());
        order.setQuantity(cmd.getQuantity());
        order.setStrategyId(cmd.getStrategyId());
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    private OrderDTO toDTO(OrderEntity entity) {
        return OrderDTO.builder()
                .orderId(entity.getOrderId())
                .clientOrderId(entity.getClientOrderId())
                .accountId(entity.getAccountId())
                .symbol(entity.getSymbol())
                .side(entity.getSide())
                .orderType(entity.getOrderType())
                .price(entity.getPrice())
                .stopPrice(entity.getStopPrice())
                .quantity(entity.getQuantity())
                .filledQuantity(entity.getFilledQuantity())
                .avgPrice(entity.getAvgPrice())
                .status(entity.getStatus())
                .rejectCode(entity.getRejectCode())
                .rejectReason(entity.getRejectReason())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .submittedAt(entity.getSubmittedAt())
                .filledAt(entity.getFilledAt())
                .cancelledAt(entity.getCancelledAt())
                .strategyId(entity.getStrategyId())
                .build();
    }
}
