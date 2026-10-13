package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.common.config.property.OrderProperties;
import lombok.Getter;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service managing Redisson delayed queues for distributed order lifecycle timeouts.
 *
 * <p>Replaces active database polling by scheduling timeout events into Redis:
 * <ul>
 *   <li><b>Restaurant Acceptance Timeout:</b> Fires after {@link OrderProperties#restaurantAcceptanceWindow()} (default 2 minutes).</li>
 *   <li><b>Order Abandonment Timeout:</b> Fires after {@link OrderProperties#abandonTtlMinutes()} (default 15 minutes).</li>
 * </ul>
 *
 * <p>Note: Redisson 4 deprecated {@link RDelayedQueue} in favor of {@code RReliableQueue},
 * but {@code RReliableQueue} is a commercial paid feature in Redisson PRO.
 * In open-source Redisson, {@link RDelayedQueue} remains the fully functional delayed queue
 * implementation, and potential Redis pub/sub drops are mitigated by our low-frequency
 * safety-net reapers.
 */
@Service
@SuppressWarnings("deprecation")
public class OrderTimeoutQueueService {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutQueueService.class);

    private final OrderProperties orderProperties;

    @Getter
    private final RBlockingQueue<UUID> acceptanceBlockingQueue;

    @Getter
    private final RDelayedQueue<UUID> acceptanceDelayedQueue;

    @Getter
    private final RBlockingQueue<UUID> abandonmentBlockingQueue;

    @Getter
    private final RDelayedQueue<UUID> abandonmentDelayedQueue;

    public OrderTimeoutQueueService(RedissonClient redissonClient, OrderProperties orderProperties) {
        this.orderProperties = orderProperties;

        this.acceptanceBlockingQueue = redissonClient.getBlockingQueue(orderProperties.acceptanceQueueName());
        this.acceptanceDelayedQueue = redissonClient.getDelayedQueue(this.acceptanceBlockingQueue);

        this.abandonmentBlockingQueue = redissonClient.getBlockingQueue(orderProperties.abandonmentQueueName());
        this.abandonmentDelayedQueue = redissonClient.getDelayedQueue(this.abandonmentBlockingQueue);
    }

    /**
     * Schedules a delayed restaurant acceptance timeout event for the given order.
     *
     * @param orderId the order ID
     * @param delay   the duration before timeout expires
     */
    public void scheduleRestaurantAcceptanceTimeout(UUID orderId, Duration delay) {
        if (!Boolean.TRUE.equals(orderProperties.delayedQueueEnabled())) {
            return;
        }
        try {
            long seconds = Math.max(1, delay.toSeconds());
            acceptanceDelayedQueue.offer(orderId, seconds, TimeUnit.SECONDS);
            log.debug("Scheduled restaurant acceptance timeout for order {} in {} seconds", orderId, seconds);
        } catch (Exception e) {
            log.error("Failed to schedule restaurant acceptance timeout for order {}: {}", orderId, e.getMessage(), e);
        }
    }

    /**
     * Schedules a delayed payment abandonment timeout event for the given order.
     *
     * @param orderId the order ID
     * @param delay   the duration before timeout expires
     */
    public void scheduleOrderAbandonment(UUID orderId, Duration delay) {
        if (!Boolean.TRUE.equals(orderProperties.delayedQueueEnabled())) {
            return;
        }
        try {
            long seconds = Math.max(1, delay.toSeconds());
            abandonmentDelayedQueue.offer(orderId, seconds, TimeUnit.SECONDS);
            log.debug("Scheduled order abandonment timeout for order {} in {} seconds", orderId, seconds);
        } catch (Exception e) {
            log.error("Failed to schedule order abandonment timeout for order {}: {}", orderId, e.getMessage(), e);
        }
    }
}
