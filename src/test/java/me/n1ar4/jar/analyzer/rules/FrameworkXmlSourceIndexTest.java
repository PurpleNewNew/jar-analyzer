package me.n1ar4.jar.analyzer.rules;

import me.n1ar4.jar.analyzer.core.facts.ResourceEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameworkXmlSourceIndexTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldExtractWebXmlAndStrutsEntries() throws Exception {
        Path webXml = tempDir.resolve("web.xml");
        Files.writeString(webXml, """
                <web-app>
                  <servlet>
                    <servlet-class>demo.web.LegacyServlet</servlet-class>
                  </servlet>
                  <filter>
                    <filter-class>demo.web.AuthFilter</filter-class>
                  </filter>
                  <listener>
                    <listener-class>demo.web.AppListener</listener-class>
                  </listener>
                </web-app>
                """, StandardCharsets.UTF_8);
        Path strutsXml = tempDir.resolve("struts.xml");
        Files.writeString(strutsXml, """
                <struts>
                  <package name="default" namespace="/">
                    <action name="login" class="demo.struts.LoginAction" method="login"/>
                    <action name="index" class="demo.struts.IndexAction"/>
                  </package>
                </struts>
                """, StandardCharsets.UTF_8);

        FrameworkXmlSourceIndex.Result index = FrameworkXmlSourceIndex.fromResources(
                java.util.List.of(resource("WEB-INF/web.xml", webXml), resource("WEB-INF/classes/struts.xml", strutsXml))
        );

        assertTrue(index.servletClasses().contains("demo/web/LegacyServlet"));
        assertTrue(index.filterClasses().contains("demo/web/AuthFilter"));
        assertTrue(index.listenerClasses().contains("demo/web/AppListener"));
        assertEquals(2, index.methodPatterns().size());
        assertTrue(index.methodPatterns().stream().anyMatch(pattern ->
                "demo/struts/LoginAction".equals(pattern.className())
                        && "login".equals(pattern.methodName())
                        && "*".equals(pattern.methodDesc())));
        assertTrue(index.methodPatterns().stream().anyMatch(pattern ->
                "demo/struts/IndexAction".equals(pattern.className())
                        && "execute".equals(pattern.methodName())));
    }

    private static ResourceEntity resource(String resourcePath, Path path) throws Exception {
        ResourceEntity entity = new ResourceEntity();
        entity.setResourcePath(resourcePath);
        entity.setPathStr(path.toString());
        entity.setFileSize(Files.size(path));
        entity.setJarId(1);
        entity.setJarName("app.war");
        return entity;
    }
}
