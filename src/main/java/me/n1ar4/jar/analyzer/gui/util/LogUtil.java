/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.util;

import cn.hutool.core.util.StrUtil;

import javax.swing.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LogUtil {
    private static JTextPane t;
    private static Style styleRed;
    private static Style styleGreen;
    private static Style styleYellow;
    private static final ConcurrentLinkedQueue<LogEntry> QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger QUEUE_SIZE = new AtomicInteger(0);
    private static final AtomicBoolean FLUSH_SCHEDULED = new AtomicBoolean(false);
    private static final AtomicLong LAST_FLUSH_MS = new AtomicLong(0L);
    private static final int BATCH_SIZE = 200;
    private static final int MAX_FLUSH = 400;
    private static final long FLUSH_INTERVAL_MS = 40L;
    private static final Object DELAY_LOCK = new Object();
    private static Timer delayedFlushTimer;

    public static void setT(JTextPane t) {
        LogUtil.t = t;
        if (styleRed == null || styleGreen == null || styleYellow == null) {
            styleRed = t.getStyledDocument().addStyle("RedStyle", null);
            StyleConstants.setForeground(styleRed, Color.red);
            styleGreen = t.getStyledDocument().addStyle("BlueStyle", null);
            StyleConstants.setForeground(styleGreen, Color.green);
            styleYellow = t.getStyledDocument().addStyle("YellowStyle", null);
            StyleConstants.setForeground(styleYellow, Color.yellow);
        }
    }

    private static void runOnEdt(Runnable task) {
        if (task == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private static void scheduleFlush(int count) {
        scheduleFlush(count, false);
    }

    private static void scheduleFlush(int count, boolean force) {
        long now = System.currentTimeMillis();
        long last = LAST_FLUSH_MS.get();
        if (!force && count < BATCH_SIZE && now - last < FLUSH_INTERVAL_MS) {
            scheduleDelayedFlush(now, last);
            return;
        }
        if (!FLUSH_SCHEDULED.compareAndSet(false, true)) {
            return;
        }
        runOnEdt(() -> {
            FLUSH_SCHEDULED.set(false);
            flushQueue();
        });
    }

    private static void scheduleDelayedFlush(long now, long last) {
        int delay = (int) Math.max(1L, FLUSH_INTERVAL_MS - (now - last));
        synchronized (DELAY_LOCK) {
            if (delayedFlushTimer == null) {
                delayedFlushTimer = new Timer(delay, e -> scheduleFlush(QUEUE_SIZE.get(), true));
                delayedFlushTimer.setRepeats(false);
            } else {
                delayedFlushTimer.setInitialDelay(delay);
            }
            if (delayedFlushTimer.isRunning()) {
                delayedFlushTimer.restart();
            } else {
                delayedFlushTimer.start();
            }
        }
    }

    private static void flushQueue() {
        if (t == null) {
            QUEUE.clear();
            QUEUE_SIZE.set(0);
            return;
        }
        int processed = 0;
        LogEntry entry;
        while (processed < MAX_FLUSH && (entry = QUEUE.poll()) != null) {
            QUEUE_SIZE.decrementAndGet();
            try {
                t.getStyledDocument().insertString(
                        t.getStyledDocument().getLength(), entry.text, entry.style);
            } catch (Exception ignored) {
            }
            processed++;
        }
        if (processed > 0) {
            t.setCaretPosition(t.getDocument().getLength());
        }
        LAST_FLUSH_MS.set(System.currentTimeMillis());
        if (!QUEUE.isEmpty()) {
            scheduleFlush(QUEUE_SIZE.get(), true);
        }
    }

    private static void enqueue(Style style, String logStr) {
        if (t == null) {
            return;
        }
        QUEUE.add(new LogEntry(style, logStr));
        int count = QUEUE_SIZE.incrementAndGet();
        scheduleFlush(count);
    }

    private static void print(Style style, String msg) {
        if (t == null) {
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = LocalTime.now().format(formatter);
        String head;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 3) {
            head = stackTrace[2].getMethodName();
        } else {
            head = "Unknown Level";
        }
        String logStr = StrUtil.format("[{}] [{}] {}\n", head, formattedTime, msg);
        enqueue(style, logStr);
    }

    public static void info(String msg) {
        print(styleGreen, msg);
    }

    public static void warn(String msg) {
        print(styleYellow, msg);
    }

    public static void error(String msg) {
        print(styleRed, msg);
    }

    private static final class LogEntry {
        private final Style style;
        private final String text;

        private LogEntry(Style style, String text) {
            this.style = style;
            this.text = text;
        }
    }
}
