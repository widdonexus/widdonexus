package com.widdo.nexus.starter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * EnableNexus.
 * <p>
 * 总开关注解，用于启动框架功能
 * <p>
 * 开启widdo nexus 需要以下步骤：
 * <p>
 * 1. 引入 nexus-spring-boot-starter
 * 2. 引入图数据库适配器， 例如：nexus-neo4j
 * 3. yml中开启开关widdo.nexus.enabled=true
 *
 * @author XYL
 * @date 2025/08/27 23:12
 * @since 0.0.1-SNAPSHOT
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ConditionalOnNexusEnabled
public @interface EnableNexus {
}
