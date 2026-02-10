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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BuildDbWriter implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger();
    private final ExecutorService executor;
    private final List<Future<?>> futures = new ArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public BuildDbWriter() {
        this.executor = Executors.newSingleThreadExecutor(
                Thread.ofPlatform().name("jar-analyzer-db-writer").daemon(true).factory()
        );
    }

    public void submit(Runnable task) {
        if (task == null) {
            return;
        }
        if (closed.get()) {
            task.run();
            return;
        }
        futures.add(executor.submit(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                me.n1ar4.jar.analyzer.utils.InterruptUtil.restoreInterruptIfNeeded(t);
                if (t instanceof Error) {
                    throw (Error) t;
                }
                logger.warn("db writer task error: {}", t.toString());
            }
        }));
    }

    public void await() {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("db writer interrupted");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    me.n1ar4.jar.analyzer.utils.InterruptUtil.restoreInterruptIfNeeded(cause);
                    if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                    logger.warn("db writer task error: {}", cause.toString());
                } else {
                    logger.warn("db writer task error: {}", e.toString());
                }
            }
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        executor.shutdown();
        await();
    }
}
