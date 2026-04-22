package com.qts.biz.trade.service;

import com.qts.biz.trade.dto.OrderDTO;
import com.qts.biz.trade.dto.OrderQuery;
import com.qts.biz.trade.dto.PagedResult;
import com.qts.biz.trade.entity.OrderEntity;
import com.qts.biz.trade.exception.OrderNotFoundException;
import com.qts.biz.trade.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Order Query Service
 * Handles order queries with filtering and pagination
 */
@Service
public class OrderQueryService {

    private static final Logger logger = LoggerFactory.getLogger(OrderQueryService.class);

    private final OrderRepository orderRepository;

    @Autowired
    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Get order by orderId
     * @param orderId Order ID
     * @return OrderDTO
     * @throws OrderNotFoundException if order not found
     */
    public OrderDTO getOrder(String orderId) {
        logger.debug("Querying order by orderId: {}", orderId);

        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        return toDTO(order);
    }

    /**
     * Get order by clientOrderId
     * @param clientOrderId Client Order ID
     * @return OrderDTO
     * @throws OrderNotFoundException if order not found
     */
    public OrderDTO getOrderByClientOrderId(String clientOrderId) {
        logger.debug("Querying order by clientOrderId: {}", clientOrderId);

        OrderEntity order = orderRepository.findByClientOrderId(clientOrderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + clientOrderId));

        return toDTO(order);
    }

    /**
     * List orders with query filters and pagination
     * 
     * Query filters: symbol, status, start_time, end_time, page, page_size
     * 
     * @param query OrderQuery with filter parameters
     * @return PagedResult of OrderDTO
     */
    public PagedResult<OrderDTO> listOrders(OrderQuery query) {
        logger.debug("Listing orders with query: accountId={}, symbol={}, status={}", 
                    query.getAccountId(), query.getSymbol(), query.getStatus());

        // Build pageable with sorting
        Pageable pageable = PageRequest.of(
                query.getPage() - 1, // Spring Data Page is 0-indexed
                query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // Execute query
        Page<OrderEntity> page = orderRepository.findByConditions(
                query.getAccountId(),
                query.getSymbol(),
                query.getStatus(),
                query.getStartTime(),
                query.getEndTime(),
                pageable
        );

        // Convert to DTOs
        List<OrderDTO> dtos = page.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new PagedResult<>(
                dtos,
                query.getPage(),
                query.getPageSize(),
                page.getTotalElements()
        );
    }

    /**
     * List orders by account ID with pagination
     * @param accountId Account ID
     * @param page Page number
     * @param pageSize Page size
     * @return PagedResult of OrderDTO
     */
    public PagedResult<OrderDTO> listOrdersByAccountId(String accountId, int page, int pageSize) {
        logger.debug("Listing orders for accountId: {}", accountId);

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderEntity> orderPage = orderRepository.findByAccountId(accountId, pageable);

        List<OrderDTO> dtos = orderPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new PagedResult<>(dtos, page, pageSize, orderPage.getTotalElements());
    }

    /**
     * Convert OrderEntity to OrderDTO
     */
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
