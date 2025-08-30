package com.widdo.nexus.core.exception;

/**
 * NexusConfigurationException.
 * <p>
 * 配置相关异常
 *
 * @author XYL
 * @date 2025/08/27 16:39
 * @since 0.0.1-SNAPSHOT
 */
public class NexusConfigurationException extends NexusException {

    public NexusConfigurationException(String message) {
        super("CONFIGURATION_ERROR", message);
    }

    public NexusConfigurationException(String message, Throwable cause) {
        super("CONFIGURATION_ERROR", message, cause);
    }
}
