package com.qts.biz.trade.event;

import com.qts.biz.trade.entity.OrderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Order Event Publisher
 * Publishes order domain events to Kafka
 */
@Component
public class OrderEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

    private static final String TOPIC_ORDER_EVENTS = "order-events";

    private final KafkaTemplate<String, OrderDomainEvent> kafkaTemplate;

    @Autowired
    public OrderEventPublisher(KafkaTemplate<String, OrderDomainEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish order created event
     */
    public void publishOrderCreatedEvent(OrderEntity order) {
        OrderDomainEvent event = new OrderDomainEvent(order, "ORDER_CREATED");
        publish(event);
        logger.info("Published ORDER_CREATED event for order {}", order.getOrderId());
    }

    /**
     * Publish order submitted event
     */
    public void publishOrderSubmittedEvent(OrderEntity order) {
        OrderDomainEvent event = new OrderDomainEvent(order, "ORDER_SUBMITTED");
        publish(event);
        logger.info("Published ORDER_SUBMITTED event for order {}", order.getOrderId());
    }

    /**
     * Publish order partial filled event
     */
    public void publishOrderPartialFilledEvent(OrderEntity order) {
        OrderDomainEvent event = new OrderDomainEvent(order, "ORDER_PARTIAL_FILLED");
        publish(event);
        logger.info("Published ORDER_PARTIAL_FILLED event for order {}", order.getOrderId());
    }

    /**
     * Publish order filled event
     */
    public void publishOrderFilledEvent(OrderEntity order) {
        OrderDomainEvent event = new OrderDomainEvent(order, "ORDER_FILLED");
        publish(event);
        logger.info("Published ORDER_FILLED event for order {}", order.getOrderId());
    }

    /**
     * Publish order cancelled event
     */
    public void publishOrderCancelledEvent(OrderEntity order) {
        OrderDomainEvent event = new OrderDomainEvent(order, "ORDER_CANCELLED");
        publish(event);
        logger.info("Published ORDER_CANCELLED event for order {}", order.getOrderId());
    }

    /**
     * Publish order rejected event
     */
    public void publishOrderRejectedEvent(OrderEntity order) {
        OrderDomainEvent event = new OrderDomainEvent(order, "ORDER_REJECTED");
        publish(event);
        logger.info("Published ORDER_REJECTED event for order {}", order.getOrderId());
    }

    /**
     * Publish event to Kafka
     */
    private void publish(OrderDomainEvent event) {
        try {
            kafkaTemplate.send(TOPIC_ORDER_EVENTS, event.getOrderId(), event);
        } catch (Exception e) {
            logger.error("Failed to publish event {} for order {}: {}", 
                        event.getEventType(), event.getOrderId(), e.getMessage());
        }
    }
}
