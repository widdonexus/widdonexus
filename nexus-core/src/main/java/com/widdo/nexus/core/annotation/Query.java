package com.widdo.nexus.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Query.
 * <p>
 * 查询注解，用于标记方法对应的Cypher查询
 *
 * @author XYL
 * @date 2025/08/27 20:39
 * @since 0.0.1-SNAPSHOT
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Query {
    String value();

    String description() default "";
}
