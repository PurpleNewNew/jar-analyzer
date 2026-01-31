/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.mapper;

import org.apache.ibatis.annotations.Param;

public interface SemanticCacheMapper {
    String selectValue(@Param("cacheKey") String cacheKey,
                       @Param("cacheType") String cacheType);

    int upsert(@Param("cacheKey") String cacheKey,
               @Param("cacheType") String cacheType,
               @Param("cacheValue") String cacheValue);

    int deleteByType(@Param("cacheType") String cacheType);

    int deleteAll();
}
