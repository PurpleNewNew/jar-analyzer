/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks code that is intentionally kept for backward compatibility.
 * <p>
 * Compatibility code should never be the default route. New primary logic
 * must run first, and this path should only be used as fallback/bridge.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface CompatibilityCode {
    /**
     * Preferred primary path.
     */
    String primary();

    /**
     * Why compatibility code still exists.
     */
    String reason() default "";
}
