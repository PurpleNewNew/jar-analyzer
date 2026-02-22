/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class LoggingStream extends PrintStream {
    private final Logger logger;
    private final OutputStream originalOut;
    private final PrintStream originalPrintStream;
    private final Charset outputCharset;

    public LoggingStream(OutputStream out, Logger logger) {
        super(out);
        this.logger = logger;
        this.originalOut = out;
        this.originalPrintStream = out instanceof PrintStream ps ? ps : null;
        this.outputCharset = resolveOutputCharset();
    }

    @Override
    public void println(String x) {
        if (!isLoggerCall()) {
            logger.info(x);
        } else {
            directPrintln(x);
        }
    }

    private boolean isLoggerCall() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().equals("me.n1ar4.log.Logger")) {
                return true;
            }
        }
        return false;
    }

    private void directPrintln(String x) {
        if (originalPrintStream != null) {
            originalPrintStream.println(x);
            return;
        }
        synchronized (this) {
            byte[] bytes = (x + System.lineSeparator()).getBytes(outputCharset);
            try {
                originalOut.write(bytes);
                originalOut.flush();
            } catch (IOException e) {
                setError();
            }
        }
    }

    private static Charset resolveOutputCharset() {
        String stdoutEncoding = System.getProperty("stdout.encoding");
        if (stdoutEncoding != null && !stdoutEncoding.isBlank()) {
            try {
                return Charset.forName(stdoutEncoding.trim());
            } catch (Exception ignored) {
            }
        }
        String sunStdoutEncoding = System.getProperty("sun.stdout.encoding");
        if (sunStdoutEncoding != null && !sunStdoutEncoding.isBlank()) {
            try {
                return Charset.forName(sunStdoutEncoding.trim());
            } catch (Exception ignored) {
            }
        }
        try {
            return Charset.defaultCharset();
        } catch (Exception ignored) {
            return StandardCharsets.UTF_8;
        }
    }
}
