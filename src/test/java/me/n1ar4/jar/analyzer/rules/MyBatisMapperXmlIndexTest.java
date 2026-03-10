package me.n1ar4.jar.analyzer.rules;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyBatisMapperXmlIndexTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void clearDatabase() {
        DatabaseManager.clearAllData();
    }

    @Test
    void shouldResolveDynamicSqlMapperAsSqlSink() throws Exception {
        Path mapper = tempDir.resolve("UserMapper.xml");
        Files.writeString(mapper, """
                <mapper namespace="demo.mapper.UserMapper">
                  <sql id="base">
                    select * from users
                  </sql>
                  <select id="findUsers">
                    <include refid="base"/>
                    where name = ${name}
                  </select>
                </mapper>
                """, StandardCharsets.UTF_8);
        DatabaseManager.saveResources(List.of(resource("mapper/UserMapper.xml", mapper)));

        MyBatisMapperXmlIndex.Result index = MyBatisMapperXmlIndex.fromResources(DatabaseManager.getResources());
        assertEquals(1, index.sinkPatterns().size());
        assertNotNull(index.resolve("demo/mapper/UserMapper", "findUsers", "()Ljava/util/List;"));

        ModelRegistry.SinkDescriptor descriptor = ModelRegistry.resolveSinkDescriptor(
                new MethodReference.Handle(
                        new ClassReference.Handle("demo/mapper/UserMapper", 1),
                        "findUsers",
                        "()Ljava/util/List;"
                )
        );
        assertEquals("sql", descriptor.getKind());
        assertTrue(descriptor.getTags().contains("mybatis"));
    }

    private static ResourceEntity resource(String resourcePath, Path path) throws Exception {
        ResourceEntity entity = new ResourceEntity();
        entity.setResourcePath(resourcePath);
        entity.setPathStr(path.toString());
        entity.setFileSize(Files.size(path));
        entity.setJarId(1);
        entity.setJarName("app.jar");
        return entity;
    }
}
