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

public interface InitMapper {
    void createJarTable();

    void createClassTable();

    void createClassFileTable();

    void createMemberTable();

    void createMethodTable();

    void createAnnoTable();

    void createInterfaceTable();

    void createMethodCallTable();

    void addMethodCallEdgeTypeColumn();

    void addMethodCallEdgeConfidenceColumn();

    void addMethodCallEdgeEvidenceColumn();

    void createMethodImplTable();

    void createStringTable();

    void createStringFtsTable();

    void createStringIndex();

    void createResourceTable();

    void createResourceIndex();

    void createSpringControllerTable();

    void createSpringMappingTable();

    void createSpringInterceptorTable();

    void createJavaWebTable();

    void createDFSResultTable();

    void createDFSResultListTable();

    void createFavoriteTable();

    void createHistoryTable();
}
