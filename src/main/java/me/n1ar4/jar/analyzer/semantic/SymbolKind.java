/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.semantic;

public enum SymbolKind {
    METHOD,
    CONSTRUCTOR,
    CLASS,
    INTERFACE,
    ENUM,
    ANNOTATION,
    RECORD,
    PACKAGE,
    FIELD,
    VARIABLE,
    TYPE_PARAM,
    UNKNOWN
}
