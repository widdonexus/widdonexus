package com.widdo.nexus.spring.handler;

import com.widdo.nexus.core.exception.*;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.properties.NexusProperties;
import jakarta.servlet.ServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.catalina.util.FilterUtil.getRequestPath;

/**
 * NexusExceptionHandler.
 * <p>
 * 全局异常处理
 *
 * @author XYL
 * @date 2025/08/27 20:20
 * @since 0.0.1-SNAPSHOT
 */
@RestControllerAdvice
@Slf4j
public class NexusExceptionHandler {
    private final NexusLogger logger = NexusLogger.getLogger(NexusExceptionHandler.class);

    private final boolean includeStackTrace;

    public NexusExceptionHandler(NexusProperties properties) {
        this.includeStackTrace = properties.getException().isIncludeStacktrace();
    }

    /**
     * 处理所有Nexus框架异常
     */
    @ExceptionHandler(NexusException.class)
    public ResponseEntity<ErrorResponse> handleNexusException(NexusException ex, ServletRequest request) {
        // 记录错误日志（带上下文信息）
        logger.error("Nexus operation failed: " + ex.getErrorCode(), ex);

        ErrorResponse error = createErrorResponse(ex, request);
        HttpStatus status = determineHttpStatus(ex);

        // 根据日志级别记录不同的信息
        if (status.is5xxServerError()) {
            logger.error("Server error: {}", ex.getErrorCode(), ex);
        } else {
            logger.warn("Client error: {}", ex.getErrorCode(), ex);
        }

        return new ResponseEntity<>(error, status);
    }

    /**
     * 创建统一的错误响应
     */
    private ErrorResponse createErrorResponse(NexusException ex, ServletRequest request) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(getRequestPath(request))
                .context(includeStackTrace ? ex.getContext() : filterSensitiveContext(ex.getContext()))
                .stacktrace(includeStackTrace ? getStackTrace(ex) : null)
                .build();
    }

    /**
     * 过滤敏感上下文信息
     */
    private Map<String, Object> filterSensitiveContext(Map<String, Object> context) {
        return context.entrySet().stream()
                .filter(entry -> !isSensitiveKey(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> isSensitiveKey(entry.getKey()) ? "***" : entry.getValue()
                ));
    }

    private StackTraceElement[] getStackTrace(NexusException ex) {
        return ex.getStackTrace();
    }

    private boolean isSensitiveKey(String key) {
        return key.contains("password") || key.contains("secret") || key.contains("token");
    }


    /**
     * 处理其他未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, ServletRequest request) {
        // 转换为框架异常
        NexusException nexusEx = NexusExceptionHelper.wrap(ex);
        return handleNexusException(nexusEx, request);
    }

    private HttpStatus determineHttpStatus(NexusException ex) {
        if (ex instanceof NexusConnectionException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (ex instanceof NexusQueryException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof NexusConfigurationException) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 统一的错误响应格式
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class ErrorResponse {
        private Instant timestamp;
        private String errorCode;
        private String message;
        private Map<String, Object> context;
        private String path;
        private StackTraceElement[] stacktrace;

        public ErrorResponse() {
            this.timestamp = Instant.now();
        }
    }
}
