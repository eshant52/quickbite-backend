package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.dto.CursorPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * High-performance JSON Redis serializer with polymorphic type preservation.
 *
 * <p>Solves Jackson's known issue with generic collections and non-concrete types
 * (such as {@code List<T>} and {@code CursorPage<T>}) without requiring dangerous
 * default typing that breaks on Java immutable collections.</p>
 *
 * <p>Wraps cached objects into a lightweight type envelope:
 * {@code {"type": "...", "elementType": "...", "json": "..."}}.
 * Stale or incompatible legacy entries gracefully fall back to {@code null}
 * (cache miss) instead of throwing {@link SerializationException} to prevent 500 errors.</p>
 */
public class QuickBiteRedisJsonSerializer implements RedisSerializer<Object> {

    private static final Logger log = LoggerFactory.getLogger(QuickBiteRedisJsonSerializer.class);

    private final ObjectMapper mapper;

    public QuickBiteRedisJsonSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public record CacheEnvelope(String type, String elementType, String json) {}

    @Override
    public byte[] serialize(Object source) throws SerializationException {
        if (source == null) {
            return new byte[0];
        }
        if (source instanceof NullValue) {
            try {
                return mapper.writeValueAsBytes(new CacheEnvelope("null", null, null));
            } catch (Exception e) {
                throw new SerializationException("Could not serialize NullValue: " + e.getMessage(), e);
            }
        }
        try {
            String type = source.getClass().getName();
            String elementType = null;

            if (source instanceof List<?> list) {
                type = List.class.getName();
                if (!list.isEmpty() && list.get(0) != null) {
                    elementType = list.get(0).getClass().getName();
                }
            } else if (source instanceof CursorPage<?> page) {
                type = CursorPage.class.getName();
                if (!page.content().isEmpty() && page.content().get(0) != null) {
                    elementType = page.content().get(0).getClass().getName();
                }
            }

            String json = mapper.writeValueAsString(source);
            CacheEnvelope envelope = new CacheEnvelope(type, elementType, json);
            return mapper.writeValueAsBytes(envelope);
        } catch (Exception e) {
            throw new SerializationException("Could not serialize cache payload: " + e.getMessage(), e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            CacheEnvelope envelope = mapper.readValue(bytes, CacheEnvelope.class);
            if ("null".equals(envelope.type())) {
                return NullValue.INSTANCE;
            }
            if (envelope.json() == null) {
                return null;
            }

            validateTrusted(envelope.type());
            if (envelope.elementType() != null) {
                validateTrusted(envelope.elementType());
            }

            if (List.class.getName().equals(envelope.type())) {
                if (envelope.elementType() != null) {
                    Class<?> elemClass = Class.forName(envelope.elementType());
                    JavaType listType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, elemClass);
                    return mapper.readValue(envelope.json(), listType);
                }
                JavaType listType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, Object.class);
                return mapper.readValue(envelope.json(), listType);
            }

            if (CursorPage.class.getName().equals(envelope.type())) {
                if (envelope.elementType() != null) {
                    Class<?> elemClass = Class.forName(envelope.elementType());
                    JavaType pageType = mapper.getTypeFactory().constructParametricType(CursorPage.class, elemClass);
                    return mapper.readValue(envelope.json(), pageType);
                }
                JavaType pageType = mapper.getTypeFactory().constructParametricType(CursorPage.class, Object.class);
                return mapper.readValue(envelope.json(), pageType);
            }

            Class<?> targetClass = Class.forName(envelope.type());
            return mapper.readValue(envelope.json(), targetClass);
        } catch (Exception ex) {
            log.warn("[CACHE] Failed to deserialize Redis cache entry, evicting/treating as cache miss: {}", ex.getMessage());
            return null;
        }
    }

    private void validateTrusted(String className) {
        if (!className.startsWith("com.quickbite.")
                && !className.startsWith("java.")
                && !className.equals("org.springframework.cache.support.NullValue")) {
            throw new SecurityException("Untrusted cache class rejected during deserialization: " + className);
        }
    }
}
