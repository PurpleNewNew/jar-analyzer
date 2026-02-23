/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.jar.analyzer.starter.Const;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.sql.DataSource;

public class SqlSessionFactoryUtil {
    private static final Logger logger = LogManager.getLogger();
    public static volatile SqlSessionFactory sqlSessionFactory = null;
    private static volatile String currentDbFile = Const.dbFile;

    private SqlSessionFactoryUtil() {
    }

    static {
        switchDatabase(Const.dbFile);
    }

    public static synchronized SqlSessionFactory getSqlSessionFactory() {
        if (sqlSessionFactory == null) {
            switchDatabase(currentDbFile);
        }
        return sqlSessionFactory;
    }

    public static synchronized String getCurrentDbFile() {
        return currentDbFile;
    }

    public static synchronized void switchDatabase(String dbFile) {
        String target = dbFile == null || dbFile.trim().isEmpty() ? Const.dbFile : dbFile.trim();
        SqlSessionFactory next = buildFactory(target);
        if (next == null) {
            throw new IllegalStateException("init mybatis factory fail: " + target);
        }
        SqlSessionFactory old = sqlSessionFactory;
        sqlSessionFactory = next;
        currentDbFile = target;
        closeFactory(old);
        logger.info("mybatis factory switched to {}", currentDbFile);
    }

    public static synchronized void shutdownCurrentFactory() {
        SqlSessionFactory old = sqlSessionFactory;
        sqlSessionFactory = null;
        closeFactory(old);
        logger.info("mybatis factory closed");
    }

    private static SqlSessionFactory buildFactory(String dbFile) {
        logger.info("init mybatis factory: {}", dbFile);
        String resource = "mybatis.xml";
        Properties props = new Properties();
        props.setProperty("driver", "org.sqlite.JDBC");
        props.setProperty("url", "jdbc:sqlite:" + dbFile + "?limit_attached=0");
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            return new SqlSessionFactoryBuilder().build(inputStream, props);
        } catch (IOException e) {
            logger.error("init mybatis factory error: {}", e.toString());
            return null;
        }
    }

    private static void closeFactory(SqlSessionFactory factory) {
        if (factory == null) {
            return;
        }
        try {
            DataSource dataSource = factory.getConfiguration()
                    .getEnvironment()
                    .getDataSource();
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
        } catch (Throwable t) {
            logger.debug("close mybatis factory datasource fail: {}", t.toString());
        }
    }
}
