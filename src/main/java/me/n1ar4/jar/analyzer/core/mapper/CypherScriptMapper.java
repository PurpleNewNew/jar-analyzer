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

import me.n1ar4.jar.analyzer.entity.CypherScriptEntity;

import java.util.ArrayList;

public interface CypherScriptMapper {
    ArrayList<CypherScriptEntity> listScripts();

    CypherScriptEntity getScriptById(long scriptId);

    void insertScript(CypherScriptEntity item);

    void updateScript(CypherScriptEntity item);

    void deleteScript(long scriptId);

    Long selectLastInsertRowId();
}
