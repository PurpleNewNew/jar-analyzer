/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultEntityNullSafetyTest {
    @Test
    void methodResultGetPathShouldHandleNull() {
        MethodResult result = new MethodResult();
        assertEquals("path: none", result.getPath());
    }

    @Test
    void leakResultToStringShouldHandleNullValue() {
        LeakResult result = new LeakResult();
        result.setClassName("demo/Test");
        result.setTypeName("demo");
        assertDoesNotThrow(result::toString);
    }

    @Test
    void resultJarIdShouldNormalizeNullToZero() {
        AnnoMethodResult anno = new AnnoMethodResult();
        anno.setJarId((Integer) null);
        assertEquals(0, anno.getJarId());

        LeakResult leak = new LeakResult();
        leak.setJarId((Integer) null);
        assertEquals(0, leak.getJarId());
    }
}
