package com.widdo.nexus.core.util;

import com.widdo.nexus.core.exception.NexusExceptionHelper;
import com.widdo.nexus.core.properties.NexusProperties;
import org.springframework.util.StringUtils;

/**
 * NexusPropertiesValidator.
 * <p>
 * 配置验证中的异常处理
 *
 * @author XYL
 * @date 2025/08/27 21:04
 * @since 0.0.1-SNAPSHOT
 */
public class NexusPropertiesValidator {

    /**
     * 配置中校验异常
     *
     * @param properties
     * @return void
     * @author XYL
     * @date 2025/08/27 23:00:30
     */
    public static void validate(NexusProperties properties) {
        // 检查必需的配置
        NexusExceptionHelper.throwIf(properties.getGraph().getDatabase().getType() == null,
                () -> NexusExceptionHelper.configError("Database configuration is required"));

        NexusExceptionHelper.throwIf(!StringUtils.hasLength(properties.getGraph().getDatabase().getType()),
                () -> NexusExceptionHelper.configError("Database type is required"));

        // 检查特定数据库配置
        if ("neo4j".equals(properties.getGraph().getDatabase().getType().toLowerCase())) {
            NexusExceptionHelper.throwIf(!StringUtils.hasLength(properties.getGraph().getNeo4j().getUri()),
                    () -> NexusExceptionHelper.configError("Neo4j URI is required"));
        }
    }
}
