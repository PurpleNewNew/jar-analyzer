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

import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CallSiteMapper {
    int insertCallSites(@Param("list") List<CallSiteEntity> list);

    List<CallSiteEntity> selectByCaller(@Param("className") String className,
                                        @Param("methodName") String methodName,
                                        @Param("methodDesc") String methodDesc);
}
