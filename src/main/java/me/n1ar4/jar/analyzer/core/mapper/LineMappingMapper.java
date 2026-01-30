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

import me.n1ar4.jar.analyzer.entity.LineMappingEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LineMappingMapper {
    int insertMappings(@Param("list") List<LineMappingEntity> list);

    int deleteByClass(@Param("className") String className,
                      @Param("jarId") Integer jarId,
                      @Param("decompiler") String decompiler);

    List<LineMappingEntity> selectByClass(@Param("className") String className,
                                          @Param("jarId") Integer jarId,
                                          @Param("decompiler") String decompiler);
}
