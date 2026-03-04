/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IOUtilTest {
    @Test
    void readAllBytesShouldReadFullStream() throws IOException {
        byte[] input = "hello-jar-analyzer".getBytes(StandardCharsets.UTF_8);
        byte[] result = IOUtil.readAllBytes(new ByteArrayInputStream(input));
        assertArrayEquals(input, result);
    }

    @Test
    void readNBytesShouldRespectMaxLength() throws IOException {
        byte[] input = "abcdef".getBytes(StandardCharsets.UTF_8);
        byte[] result = IOUtil.readNBytes(new ByteArrayInputStream(input), 3);
        assertEquals("abc", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void readNBytesShouldRejectNegativeLength() {
        assertThrows(IllegalArgumentException.class,
                () -> IOUtil.readNBytes(new ByteArrayInputStream(new byte[0]), -1));
    }

    @Test
    void readNBytesShouldReturnAvailableBytesWhenLenTooLarge() throws IOException {
        byte[] input = "compat".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(input, IOUtil.readNBytes(new ByteArrayInputStream(input), 64));
    }
}
