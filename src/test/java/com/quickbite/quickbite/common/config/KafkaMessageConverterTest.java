package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.event.order.OrderEvent;
import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;
import org.springframework.kafka.support.mapping.DefaultJacksonJavaTypeMapper;
import org.springframework.kafka.support.mapping.JacksonJavaTypeMapper;
import org.springframework.messaging.Message;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaMessageConverterTest {

    @Test
    void testMessageConverter() {
        StringJacksonJsonMessageConverter converter = new StringJacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.addTrustedPackages("*");
        converter.setTypeMapper(typeMapper);

        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        String json = """
                {
                    "eventType": "ORDER_PLACED",
                    "orderId": "%s",
                    "customerId": "%s",
                    "customerName": "John Doe",
                    "customerEmail": "john@example.com",
                    "restaurantId": "%s",
                    "restaurantName": "Pizza Place",
                    "totalAmount": 150.00,
                    "placedAt": "2026-10-15T10:00:00Z"
                }
                """.formatted(orderId, customerId, restaurantId);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("quickbite.order.events", 0, 0L, orderId.toString(), json);
        Message<?> message = converter.toMessage(record, null, null, OrderEvent.class);

        assertThat(message.getPayload()).isInstanceOf(OrderPlacedEvent.class);
        OrderPlacedEvent event = (OrderPlacedEvent) message.getPayload();
        assertThat(event.orderId()).isEqualTo(orderId);
    }

    @Test
    void testMessageConverter_invalidJson_throwsConversionException() {
        StringJacksonJsonMessageConverter converter = new StringJacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.addTrustedPackages("*");
        converter.setTypeMapper(typeMapper);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("quickbite.order.events", 0, 0L, "key", "not-a-json");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> converter.toMessage(record, null, null, OrderEvent.class))
                .isInstanceOf(org.springframework.kafka.support.converter.ConversionException.class);
    }
}
