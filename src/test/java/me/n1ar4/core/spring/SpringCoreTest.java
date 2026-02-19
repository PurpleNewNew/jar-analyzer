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
import static org.junit.jupiter.api.Assertions.fail;

public class SpringCoreTest {
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

            // 读取 spring_method_table 表确保包含了 10 条且每一项的 path 字段都不是空
            PreparedStatement stmt2 = conn.prepareStatement("SELECT COUNT(*) FROM spring_method_table WHERE path IS NOT NULL AND path != ''");
            ResultSet rs2 = stmt2.executeQuery();
            if (rs2.next()) {
                int countWithPath = rs2.getInt(1);
                System.out.println("spring_method_table 表中 path 字段非空的数据条数: " + countWithPath);
                assertEquals(10, countWithPath, "spring_method_table 表应该包含 10 条且每一项的 path 字段都不是空");
            }
            rs2.close();
            stmt2.close();

            // 关闭数据库连接
            conn.close();

            System.out.println("所有数据库验证通过！");

        } catch (Exception e) {
            e.printStackTrace();
            fail("测试执行失败: " + e.getMessage());
        }
    }
}
