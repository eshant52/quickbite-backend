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
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
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
     * A consumer factory that deserializes Kafka message values as raw UTF-8 strings.
     * Listeners manually deserialize the polymorphic JSON payloads.
     */
    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Container factory for string consumers with automatic Dead Letter Topic (DLT) recovery.
     * Retries up to 3 times with a 1-second fixed delay before publishing the unrecoverable
     * poisoned record to &lt;topic&gt;.DLT.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory(
            ConsumerFactory<String, String> stringConsumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory);

        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    // -------------------------------------------------------------------------
    // Topic declarations — Domain Aggregate Streams & Dead Letter Queues
    // -------------------------------------------------------------------------

    // 1. Order Stream
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(QuickBiteTopics.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderEventsDltTopic() {
        return TopicBuilder.name(QuickBiteTopics.ORDER_EVENTS + QuickBiteTopics.DLT_SUFFIX).partitions(1).replicas(1).build();
    }

    // 2. Restaurant Application Stream
    @Bean
    public NewTopic restaurantApplicationEventsTopic() {
        return TopicBuilder.name(QuickBiteTopics.RESTAURANT_APPLICATION_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic restaurantApplicationEventsDltTopic() {
        return TopicBuilder.name(QuickBiteTopics.RESTAURANT_APPLICATION_EVENTS + QuickBiteTopics.DLT_SUFFIX).partitions(1).replicas(1).build();
    }

    // 3. Cuisine Stream
    @Bean
    public NewTopic cuisineEventsTopic() {
        return TopicBuilder.name(QuickBiteTopics.CUISINE_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic cuisineEventsDltTopic() {
        return TopicBuilder.name(QuickBiteTopics.CUISINE_EVENTS + QuickBiteTopics.DLT_SUFFIX).partitions(1).replicas(1).build();
    }

    // 4. Payment Stream
    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(QuickBiteTopics.PAYMENT_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentEventsDltTopic() {
        return TopicBuilder.name(QuickBiteTopics.PAYMENT_EVENTS + QuickBiteTopics.DLT_SUFFIX).partitions(1).replicas(1).build();
    }
}
