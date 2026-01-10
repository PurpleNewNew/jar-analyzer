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

import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ResourceMapper {
    int insertResources(List<ResourceEntity> resources);

    List<ResourceEntity> selectResources(@Param("path") String path,
                                         @Param("jarId") Integer jarId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    int selectCount(@Param("path") String path, @Param("jarId") Integer jarId);

    ResourceEntity selectResourceById(@Param("rid") int rid);

    ResourceEntity selectResourceByPath(@Param("jarId") Integer jarId,
                                        @Param("path") String path);

    List<ResourceEntity> selectTextResources(@Param("jarId") Integer jarId);
}
