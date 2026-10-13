package com.quickbite.quickbite.order.consumer;

import com.quickbite.quickbite.common.config.property.OrderProperties;
import com.quickbite.quickbite.order.service.OrderLifecycleService;
import com.quickbite.quickbite.order.service.OrderTimeoutQueueService;
import org.redisson.api.RBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Background consumer that listens to Redisson blocking queues for expired
 * order lifecycle timeout events.
 *
 * <p>Concurrency & Isolation:
 * <ul>
 *   <li><b>Atomic Distribution:</b> Calls {@link RBlockingQueue#poll(long, TimeUnit)}, which executes an atomic
 *       Redis {@code BLPOP}. In a multi-pod deployment, each expired order ID is delivered to exactly
 *       one worker thread across all pods, completely preventing pod collisions.</li>
 *   <li><b>Isolated Per-Order Transactions:</b> Each consumed timeout invokes
 *       {@link OrderLifecycleService#cancelUnacceptedOrder(UUID)} or
 *       {@link OrderLifecycleService#abandonOrderById(UUID, String)} in an isolated {@code REQUIRES_NEW}
 *       transaction.</li>
 *   <li><b>Idempotent Skip:</b> If an order was accepted by the restaurant or paid before the timeout
 *       popped, the lifecycle service's status guard detects the non-matching status and exits as an
 *       instant no-op.</li>
 * </ul>
 */
@Component
public class OrderTimeoutConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutConsumer.class);

    private final OrderTimeoutQueueService queueService;
    private final OrderLifecycleService orderLifecycleService;
    private final OrderProperties orderProperties;

    private final AtomicInteger threadIndex = new AtomicInteger(1);
    private ExecutorService executor;
    private volatile boolean running = false;

    public OrderTimeoutConsumer(
            OrderTimeoutQueueService queueService,
            OrderLifecycleService orderLifecycleService,
            OrderProperties orderProperties) {
        this.queueService = queueService;
        this.orderLifecycleService = orderLifecycleService;
        this.orderProperties = orderProperties;
    }

    @Override
    public synchronized void start() {
        if (this.running) {
            return;
        }

        if (!Boolean.TRUE.equals(orderProperties.delayedQueueEnabled())) {
            log.info("Order timeout delayed queue consumer is disabled via configuration.");
            return;
        }

        this.running = true;
        this.executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "order-timeout-worker-" + threadIndex.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });

        executor.submit(this::consumeAcceptanceTimeouts);
        executor.submit(this::consumeAbandonments);
        log.info("Order timeout delayed queue consumers started.");
    }

    @Override
    public synchronized void stop() {
        if (!this.running) {
            return;
        }
        log.info("Stopping order timeout delayed queue consumers...");
        this.running = false;
        if (executor != null) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Order timeout consumers did not terminate within timeout.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("Order timeout delayed queue consumers stopped.");
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        // Start late in container lifecycle, stop early before datasource/Redis shutdown
        return Integer.MAX_VALUE - 100;
    }

    void consumeAcceptanceTimeouts() {
        RBlockingQueue<UUID> queue = queueService.getAcceptanceBlockingQueue();
        log.info("Started consumer for restaurant acceptance timeout queue.");
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                UUID orderId = queue.poll(2, TimeUnit.SECONDS);
                if (orderId != null) {
                    processAcceptanceTimeout(orderId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Acceptance timeout consumer thread interrupted, shutting down.");
                break;
            } catch (Exception e) {
                log.error("Unexpected error in acceptance timeout consumer loop: {}", e.getMessage(), e);
            }
        }
    }

    void consumeAbandonments() {
        RBlockingQueue<UUID> queue = queueService.getAbandonmentBlockingQueue();
        log.info("Started consumer for order abandonment timeout queue.");
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                UUID orderId = queue.poll(2, TimeUnit.SECONDS);
                if (orderId != null) {
                    processAbandonment(orderId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Abandonment timeout consumer thread interrupted, shutting down.");
                break;
            } catch (Exception e) {
                log.error("Unexpected error in abandonment timeout consumer loop: {}", e.getMessage(), e);
            }
        }
    }

    void processAcceptanceTimeout(UUID orderId) {
        try {
            log.info("Processing restaurant acceptance timeout for order: {}", orderId);
            orderLifecycleService.cancelUnacceptedOrder(orderId);
        } catch (Exception e) {
            log.error("Error executing cancellation for timed-out unaccepted order {}: {}", orderId, e.getMessage(), e);
        }
    }

    void processAbandonment(UUID orderId) {
        try {
            log.info("Processing order abandonment for order: {}", orderId);
            orderLifecycleService.abandonOrderById(orderId, "Payment window expired");
        } catch (Exception e) {
            log.error("Error executing abandonment for timed-out order {}: {}", orderId, e.getMessage(), e);
        }
    }
}
