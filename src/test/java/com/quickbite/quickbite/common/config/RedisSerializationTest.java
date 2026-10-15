package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.quickbite.notification.dto.UnreadCountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.NullValue;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisSerializationTest {

    private QuickBiteRedisJsonSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new QuickBiteRedisJsonSerializer(JsonMapper.builder().build());
    }

    @Test
    void testCuisineListSerialization() {
        CuisineResponse c1 = new CuisineResponse(UUID.randomUUID(), "Italian", Instant.now());
        CuisineResponse c2 = new CuisineResponse(UUID.randomUUID(), "Mexican", Instant.now());
        List<CuisineResponse> list = List.of(c1, c2); // ImmutableCollections.ListN from .toList()

        byte[] bytes = serializer.serialize(list);
        Object deserialized = serializer.deserialize(bytes);

        assertThat(deserialized).isInstanceOf(List.class);
        List<?> res = (List<?>) deserialized;
        assertThat(res).hasSize(2);
        assertThat(res.get(0)).isInstanceOf(CuisineResponse.class);
        assertThat(((CuisineResponse) res.get(0)).name()).isEqualTo("Italian");
        assertThat(((CuisineResponse) res.get(1)).name()).isEqualTo("Mexican");
    }

    @Test
    void testCursorPageSerialization() {
        CuisineResponse cuisine = new CuisineResponse(UUID.randomUUID(), "Italian", Instant.now());
        MenuItemResponse item = new MenuItemResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Pizza Margherita",
                "Delicious",
                cuisine,
                BigDecimal.valueOf(200),
                "Main",
                true,
                List.of(),
                Instant.now(),
                Instant.now()
        );
        CursorPage<MenuItemResponse> page = new CursorPage<>(List.of(item), null, false, 1);

        byte[] bytes = serializer.serialize(page);
        Object deserialized = serializer.deserialize(bytes);

        assertThat(deserialized).isInstanceOf(CursorPage.class);
        CursorPage<?> res = (CursorPage<?>) deserialized;
        assertThat(res.content()).hasSize(1);
        assertThat(res.content().get(0)).isInstanceOf(MenuItemResponse.class);
        assertThat(((MenuItemResponse) res.content().get(0)).name()).isEqualTo("Pizza Margherita");
    }

    @Test
    void testSingleRecordSerialization() {
        UnreadCountResponse unread = new UnreadCountResponse(5);
        byte[] bytes = serializer.serialize(unread);
        Object deserialized = serializer.deserialize(bytes);

        assertThat(deserialized).isInstanceOf(UnreadCountResponse.class);
        assertThat(((UnreadCountResponse) deserialized).count()).isEqualTo(5);
    }

    @Test
    void testNullValueSupport() {
        byte[] bytes = serializer.serialize(NullValue.INSTANCE);
        Object deserialized = serializer.deserialize(bytes);
        assertThat(deserialized).isSameAs(NullValue.INSTANCE);
    }

    @Test
    void testLegacyCorruptEntryFallbackToNull() {
        // Simulating the user's exact corrupted cache entry:
        byte[] legacyBytes = "[{\"@class\":\"com.quickbite.quickbite.menu.dto.CuisineResponse\"}]".getBytes();
        Object result = serializer.deserialize(legacyBytes);
        // Returns null -> cache miss instead of 500 error!
        assertThat(result).isNull();
    }
}
