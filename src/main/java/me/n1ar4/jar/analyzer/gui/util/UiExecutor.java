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

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class UiExecutor {
    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
    private static final int POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
            POOL_SIZE,
            POOL_SIZE,
            30L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r, "ui-worker-" + THREAD_ID.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
    );

    private UiExecutor() {
    }

    public static void runAsync(Runnable task) {
        if (task == null) {
            return;
        }
        EXECUTOR.execute(task);
    }

    public static void runOnEdt(Runnable task) {
        if (task == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    public static <T> T callOnEdt(Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return supplier.get();
        }
        AtomicReference<T> ref = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> ref.set(supplier.get()));
        } catch (Exception ignored) {
            return null;
        }
        return ref.get();
    }

    public static void runAsyncWithDialog(JDialog dialog, Runnable task) {
        if (dialog != null) {
            runOnEdt(() -> dialog.setVisible(true));
        }
        runAsync(() -> {
            try {
                if (task != null) {
                    task.run();
                }
            } finally {
                if (dialog != null) {
                    runOnEdt(dialog::dispose);
                }
            }
        });
    }
}
