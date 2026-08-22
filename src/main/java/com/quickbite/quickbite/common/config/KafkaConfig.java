package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = kafkaProperties.buildProducerProperties();
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties();
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    /**
     * A separate consumer factory that deserializes Kafka message values as raw
     * UTF-8 strings. Used by {@link com.quickbite.quickbite.notification.listener.RestaurantApplicationEventListener}
     * which then manually deserializes the JSON with {@code ObjectMapper}.
     * <p>
     * This is more explicit than relying on {@code JacksonJsonDeserializer} with a
     * global default type, and makes each listener fully responsible for its own
     * deserialization contract.
     */
    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory(
            ConsumerFactory<String, String> stringConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory);
        // Retry up to 3 times with a 1-second fixed delay before sending to the DLT.
        // The DLT is declared below as a NewTopic bean so Kafka auto-creates it.
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(new FixedBackOff(1_000L, 3)));
        return factory;
    }

    // -------------------------------------------------------------------------
    // Topic declarations — Kafka auto-creates these on first use if missing,
    // but declaring them explicitly ensures the correct partition/replication
    // settings are applied in all environments.
    // -------------------------------------------------------------------------

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(QuickBiteTopics.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(QuickBiteTopics.NOTIFICATION_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic deliveryEventsTopic() {
        return TopicBuilder.name(QuickBiteTopics.DELIVERY_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderEventsDlqTopic() {
        return TopicBuilder.name(QuickBiteTopics.ORDER_EVENTS_DLQ).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic restaurantApplicationSubmittedTopic() {
        return TopicBuilder.name(QuickBiteTopics.RESTAURANT_APPLICATION_SUBMITTED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic restaurantApplicationApprovedTopic() {
        return TopicBuilder.name(QuickBiteTopics.RESTAURANT_APPLICATION_APPROVED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic restaurantApplicationRejectedTopic() {
        return TopicBuilder.name(QuickBiteTopics.RESTAURANT_APPLICATION_REJECTED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationDltTopic() {
        return TopicBuilder.name(QuickBiteTopics.NOTIFICATION_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic cuisineRequestedTopic() {
        return TopicBuilder.name(QuickBiteTopics.CUISINE_REQUESTED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic cuisineApprovedTopic() {
        return TopicBuilder.name(QuickBiteTopics.CUISINE_APPROVED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic cuisineRejectedTopic() {
        return TopicBuilder.name(QuickBiteTopics.CUISINE_REJECTED).partitions(3).replicas(1).build();
    }
}
