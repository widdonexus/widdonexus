package com.widdo.nexus.core.entity;

import com.widdo.nexus.core.exception.NexusExceptionHelper;
import com.widdo.nexus.core.result.entity.Value;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NexusEntityMapper
 * <p>
 * Nexus实体映射器
 *
 * @author XYL
 * @date 2025/08/27 16:20
 * @since 0.0.1-SNAPSHOT
 */
public class NexusEntityMapper {

    public <T> T mapToEntity(Map<String, Value> data, Class<T> entityClass) {
        try {
            final T instance = entityClass.getDeclaredConstructor().newInstance();

            for (Field field : entityClass.getDeclaredFields()) {
                field.setAccessible(true);
                final Object value = data.get(field.getName());
                try {
                    if (value != null) {
                        field.set(instance, convertValue(value, field.getType()));
                    }
                } catch (Exception e) {
                    throw NexusExceptionHelper.mappingError(
                                    "Failed to map field: " + field.getName(), entityClass)
                            .withContext("field", field.getName())
                            .withContext("value", value);
                }
            }

            return instance;
        } catch (Exception e) {
            throw NexusExceptionHelper.mappingError("Failed to create entity instance", entityClass)
                    .withContext("data", data);
        }
    }

    private Object convertValue(Object value, Class<?> targetType) {
        // 转换逻辑
        if (value == null) {
            return null;
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        // 简单的类型转换逻辑
        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Integer.class || targetType == int.class) {
            return ((Number) value).intValue();
        } else if (targetType == Long.class || targetType == long.class) {
            return ((Number) value).longValue();
        } else if (targetType == Double.class || targetType == double.class) {
            return ((Number) value).doubleValue();
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value.toString());
        }

        return value;
    }

    public <T> List<T> mapToEntities(List<Map<String, Value>> dataList, Class<T> entityClass) {
        return dataList.stream()
                .map(data -> mapToEntity(data, entityClass))
                .collect(Collectors.toList());
    }
}
