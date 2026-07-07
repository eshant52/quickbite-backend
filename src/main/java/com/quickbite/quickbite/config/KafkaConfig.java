package com.quickbite.quickbite.config;

import java.util.Map;

import com.quickbite.quickbite.events.QuickBiteTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final String TRUSTED_PACKAGES = "com.quickbite.quickbite.events,com.quickbite.quickbite.models";

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = kafkaProperties.buildProducerProperties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        properties.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties();
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        properties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, TRUSTED_PACKAGES);
        properties.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        properties.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, Object.class.getName());
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

    @Bean
    public NewTopic orderEventsTopic(
            @Value("${app.kafka.topics.order-events:" + QuickBiteTopics.ORDER_EVENTS + "}") String topicName,
            @Value("${app.kafka.topics.partitions:3}") int partitions,
            @Value("${app.kafka.topics.replication-factor:1}") int replicationFactor) {
        return topic(topicName, partitions, replicationFactor);
    }

    @Bean
    public NewTopic notificationEventsTopic(
            @Value("${app.kafka.topics.notification-events:" + QuickBiteTopics.NOTIFICATION_EVENTS + "}") String topicName,
            @Value("${app.kafka.topics.partitions:3}") int partitions,
            @Value("${app.kafka.topics.replication-factor:1}") int replicationFactor) {
        return topic(topicName, partitions, replicationFactor);
    }

    @Bean
    public NewTopic deliveryEventsTopic(
            @Value("${app.kafka.topics.delivery-events:" + QuickBiteTopics.DELIVERY_EVENTS + "}") String topicName,
            @Value("${app.kafka.topics.partitions:3}") int partitions,
            @Value("${app.kafka.topics.replication-factor:1}") int replicationFactor) {
        return topic(topicName, partitions, replicationFactor);
    }

    @Bean
    public NewTopic orderEventsDlqTopic(
            @Value("${app.kafka.topics.order-events-dlq:" + QuickBiteTopics.ORDER_EVENTS_DLQ + "}") String topicName,
            @Value("${app.kafka.topics.dlq-partitions:1}") int partitions,
            @Value("${app.kafka.topics.replication-factor:1}") int replicationFactor) {
        return topic(topicName, partitions, replicationFactor);
    }

    private NewTopic topic(String topicName, int partitions, int replicationFactor) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}
