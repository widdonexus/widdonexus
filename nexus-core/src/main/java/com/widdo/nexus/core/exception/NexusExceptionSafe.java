package com.widdo.nexus.core.exception;

import java.util.function.Supplier;

/**
 * NexusExceptionSafe.
 * <p>
 * 异常安全执行模板
 *
 * @author XYL
 * @date 2025/08/27 21:08
 * @since 0.0.1-SNAPSHOT
 */
public class NexusExceptionSafe {

    /**
     * execute.
     *
     * @param operation
     * @param operationName
     * @return T
     * @author XYL
     * @date 2025/08/27 21:59:07
     */
    public static <T> T execute(Supplier<T> operation, String operationName) {
        try {
            return operation.get();
        } catch (NexusException ex) {
            // 重新抛出框架异常
            throw ex;
        } catch (Exception ex) {
            // 包装其他异常
            throw NexusExceptionHelper.wrap(ex)
                    .withContext("operation", operationName);
        }
    }

    /**
     * execute.
     *
     * @param operation
     * @param operationName
     * @return void
     * @author XYL
     * @date 2025/08/27 21:58:56
     */
    public static void execute(Runnable operation, String operationName) {
        execute(() -> {
            operation.run();
            return null;
        }, operationName);
    }

    /**
     * 带重试的异常安全执行
     *
     * @param operation
     * @param operationName
     * @param maxRetries
     * @param delayMs
     * @return T
     * @author XYL
     * @date 2025/08/27 21:58:29
     */
    public static <T> T executeWithRetry(Supplier<T> operation, String operationName,
                                         int maxRetries, long delayMs) {
        int attempt = 0;
        while (true) {
            try {
                return operation.get();
            } catch (Exception ex) {
                attempt++;
                if (attempt > maxRetries) {
                    throw NexusExceptionHelper.wrap(ex)
                            .withContext("operation", operationName)
                            .withContext("attempts", attempt);
                }

                // 等待后重试
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw NexusExceptionHelper.wrap(ie)
                            .withContext("operation", operationName);
                }
            }
        }
    }
}
