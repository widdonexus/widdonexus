package com.widdo.nexus.core.exception;

/**
 * NexusMappingException.
 * <p>
 * 映射相关异常
 *
 * @author XYL
 * @date 2025/08/27 16:37
 * @since 0.0.1-SNAPSHOT
 */
public class NexusMappingException extends NexusException {

    public NexusMappingException(String message) {
        super("MAPPING_ERROR", message);
    }

    public NexusMappingException(String message, Throwable cause) {
        super("MAPPING_ERROR", message, cause);
    }

    public NexusMappingException withEntityType(Class<?> entityType) {
        return (NexusMappingException) withContext("entityType", entityType.getName());
    }
}
