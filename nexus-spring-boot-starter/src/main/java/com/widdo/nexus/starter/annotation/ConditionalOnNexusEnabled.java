package com.widdo.nexus.starter.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.*;

/**
 * ConditionalOnNexusEnabled.
 * <p>
 * nexus总开关开启的条件之一
 *
 * @author XYL
 * @date 2025/08/27 23:14
 * @since 0.0.1-SNAPSHOT
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(value = "widdo.nexus.enabled", havingValue = "true")
public @interface ConditionalOnNexusEnabled {
}
