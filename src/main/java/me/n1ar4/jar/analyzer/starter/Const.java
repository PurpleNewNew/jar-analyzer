/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.starter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

public interface Const {
    String version = "6.0";

    int ASMVersion = Opcodes.ASM9;

    int GlobalASMOptions = ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG;
    int AnalyzeASMOptions = ClassReader.EXPAND_FRAMES;
    int DiscoveryASMOptions = ClassReader.SKIP_FRAMES;
    int HeaderASMOptions = ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG;

    String app = "Jar Analyzer - PurpleNewNew - " + version;
    String authorUrl = "https://github.com/4ra1n";
    String coAuthorUrl = "https://github.com/PurpleNewNew";
    String projectUrl = "https://github.com/PurpleNewNew/jar-analyzer";
    String newIssueUrl = "https://github.com/PurpleNewNew/jar-analyzer/issues/new/choose";
    String docsUrl = "https://docs.qq.com/doc/DV3pKbG9GS0pJS0tk";
    String dbFile = "jar-analyzer.db";
    String tempDir = "jar-analyzer-temp";
    String resourceDir = "resources";
    String indexDir = "jar-analyzer-document";
    String downDir = "jar-analyzer-download";
    String OpcodeForm = "Jar Analyzer - Method Opcode";
    String SPELSearch = "Jar Analyzer - SPEL Search";
    String ChangeLogForm = "Jar Analyzer - CHANGELOG";
    String CFGForm = "Jar Analyzer - CFG";
    String FrameForm = "Jar Analyzer - Frame";
    String SQLiteForm = "Jar Analyzer - SQLite";
    String BcelForm = "Jar Analyzer - BCEL Util";
    String StringForm = "Jar Analyzer - String";
    String RemoteForm = "Jar Analyzer - Remote Load";
    String PartForm = "Jar Analyzer - Partition Config";
    String SerUtilForm = "Jar Analyzer - SerUtil";
    String ExportForm = "Jar Analyzer - Export Java Code";
    String ModeForm = "Jar Analyzer - Mode";
    String blackAreaText = "# package black list\n" +
            "java.util.;\n" +
            "# class black list\n" +
            "java.lang.Object;\n";
    String classBlackAreaText = "# package black list\n" +
            "com.test.a.;\n" +
            "# class black list\n" +
            "com.test.a.Test;\n";
}
