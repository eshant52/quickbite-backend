package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;

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
    public NewTopic restaurantApprovedTopic() {
        return TopicBuilder.name(QuickBiteTopics.RESTAURANT_APPROVED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic restaurantRejectedTopic() {
        return TopicBuilder.name(QuickBiteTopics.RESTAURANT_REJECTED).partitions(3).replicas(1).build();
    }
}
