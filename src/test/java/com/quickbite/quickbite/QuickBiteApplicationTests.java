package com.quickbite.quickbite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.quickbite.quickbite.common.config.KafkaConfig;
import com.quickbite.quickbite.common.config.RedisConfig;
import com.quickbite.quickbite.common.event.QuickBiteTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {
        RedisConfig.class,
        KafkaConfig.class,
        QuickBiteApplicationTests.TestConfig.class
})
@TestPropertySource(properties = {
        "app.cache.default-ttl=PT10M",
        "app.kafka.topics.order-events=quickbite.order.events",
        "app.kafka.topics.notification-events=quickbite.notification.events",
        "app.kafka.topics.delivery-events=quickbite.delivery.events",
        "app.kafka.topics.order-events-dlq=quickbite.order.events.DLQ",
        "app.kafka.topics.partitions=3",
        "app.kafka.topics.dlq-partitions=1",
        "app.kafka.topics.replication-factor=1",
        "spring.kafka.bootstrap-servers=localhost:9092"
})
class QuickBiteApplicationTests {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory;

    @Autowired
    private Map<String, NewTopic> topics;

    @Test
    void contextLoads() {
    }

    @Test
    void redisAndKafkaInfrastructureBeansLoad() {
        assertThat(cacheManager).isNotNull();
        assertThat(redisTemplate).isNotNull();
        assertThat(kafkaTemplate).isNotNull();
        assertThat(kafkaListenerContainerFactory).isNotNull();
    }

    @Test
    void declaresQuickBiteKafkaTopics() {
        assertThat(topics.values())
                .extracting(NewTopic::name)
                .containsExactlyInAnyOrder(
                        QuickBiteTopics.ORDER_EVENTS,
                        QuickBiteTopics.NOTIFICATION_EVENTS,
                        QuickBiteTopics.DELIVERY_EVENTS,
                        QuickBiteTopics.ORDER_EVENTS_DLQ,
                        QuickBiteTopics.RESTAURANT_APPLICATION_SUBMITTED,
                        QuickBiteTopics.RESTAURANT_APPLICATION_APPROVED,
                        QuickBiteTopics.RESTAURANT_APPLICATION_REJECTED,
                        QuickBiteTopics.NOTIFICATION_DLT);

        // DLQ / DLT topics use 1 partition; all other topics use 3
        var singlePartitionTopics = List.of(QuickBiteTopics.ORDER_EVENTS_DLQ, QuickBiteTopics.NOTIFICATION_DLT);
        assertThat(topics.values())
                .filteredOn(topic -> !singlePartitionTopics.contains(topic.name()))
                .allSatisfy(topic -> {
                    assertThat(topic.numPartitions()).isEqualTo(3);
                    assertThat(topic.replicationFactor()).isEqualTo((short) 1);
                });

        assertThat(topics.values())
                .filteredOn(topic -> singlePartitionTopics.contains(topic.name()))
                .hasSize(2)
                .allSatisfy(topic -> {
                    assertThat(topic.numPartitions()).isEqualTo(1);
                    assertThat(topic.replicationFactor()).isEqualTo((short) 1);
                });

    }


    @Configuration
    @EnableConfigurationProperties(KafkaProperties.class)
    static class TestConfig {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return new RedisConnectionFactory() {
                @Override
                public RedisConnection getConnection() {
                    throw new UnsupportedOperationException("Redis connections are not opened in this test");
                }

                @Override
                public RedisClusterConnection getClusterConnection() {
                    throw new UnsupportedOperationException("Redis cluster connections are not opened in this test");
                }

                @Override
                public RedisSentinelConnection getSentinelConnection() {
                    throw new UnsupportedOperationException("Redis sentinel connections are not opened in this test");
                }

                @Override
                public boolean getConvertPipelineAndTxResults() {
                    return true;
                }

                @Override
                public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
                    return null;
                }
            };
        }
    }

}
