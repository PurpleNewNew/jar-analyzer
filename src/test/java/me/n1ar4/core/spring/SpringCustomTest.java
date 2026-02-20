/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.core.spring;

import org.junit.jupiter.api.Tag;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("legacy-sqlite")
public class SpringCustomTest {
    String dbPath = Const.dbFile;

    @Test
    @SuppressWarnings("all")
    public void testRun() {
        try {
            Path file = FixtureJars.springbootTestJar();

            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.run(file, null, false, false, true, null, true);

            // 连接数据库
            String url = "jdbc:sqlite:" + dbPath;
            Connection conn = DriverManager.getConnection(url);

            // 读取当前目录的 jar-analyzer.db 文件的 spring_controller_table 表确保包含了 4 条数据
            PreparedStatement stmt1 = conn.prepareStatement("SELECT COUNT(*) FROM spring_controller_table");
            ResultSet rs1 = stmt1.executeQuery();
            if (rs1.next()) {
                int count = rs1.getInt(1);
                System.out.println("spring_controller_table 表数据条数: " + count);
                assertEquals(4, count, "spring_controller_table 表应该包含 4 条数据");
            }
            rs1.close();
            stmt1.close();

            // 读取 class_table 表确保有 77 条数据
            PreparedStatement stmt3 = conn.prepareStatement("SELECT COUNT(*) FROM class_table");
            ResultSet rs3 = stmt3.executeQuery();
            if (rs3.next()) {
                int classCount = rs3.getInt(1);
                System.out.println("class_table 表数据条数: " + classCount);
                assertEquals(77, classCount, "class_table 表应该包含 77 条数据");
            }
            rs3.close();
            stmt3.close();

            // 读取 method_call_table 表确保有 1931 条数据
            PreparedStatement stmt4 = conn.prepareStatement("SELECT COUNT(*) FROM method_call_table");
            ResultSet rs4 = stmt4.executeQuery();
            if (rs4.next()) {
                int methodCallCount = rs4.getInt(1);
                System.out.println("method_call_table 表数据条数: " + methodCallCount);
                assertTrue(methodCallCount >= 1931,
                        "method_call_table 表数据条数应该不少于 1931（语义增强后可能更多）");
            }
            rs4.close();
            stmt4.close();

            // 验证 lambda 推导边存在
            PreparedStatement stmt5 = conn.prepareStatement(
                    "SELECT COUNT(*) FROM method_call_table WHERE edge_evidence LIKE '%lambda%'");
            ResultSet rs5 = stmt5.executeQuery();
            if (rs5.next()) {
                int lambdaCount = rs5.getInt(1);
                System.out.println("method_call_table 表 lambda 推导边条数: " + lambdaCount);
                assertTrue(lambdaCount > 0, "method_call_table 应该包含至少 1 条 lambda 推导边");
            }
            rs5.close();
            stmt5.close();

            // 关闭数据库连接
            conn.close();

            System.out.println("所有数据库验证通过！");

        } catch (Exception e) {
            e.printStackTrace();
            fail("测试执行失败: " + e.getMessage());
        }
    }
}
