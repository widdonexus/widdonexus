package com.widdo.nexus.core.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * QueryRef.
 * <p>
 * 查询引用注解，用于引用YAML中定义的查询
 *
 * @author XYL
 * @date 2025/08/27 20:39
 * @since 0.0.1-SNAPSHOT
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface QueryRef {
    String value();

    String description() default "";
}
