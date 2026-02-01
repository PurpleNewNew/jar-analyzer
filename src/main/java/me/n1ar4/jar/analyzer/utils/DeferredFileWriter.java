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

import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class DeferredFileWriter {
    private static final Logger logger = LogManager.getLogger();
    private static final int QUEUE_CAPACITY = Math.max(256,
            Runtime.getRuntime().availableProcessors() * 64);
    private static final Object LOCK = new Object();
    private static volatile Writer worker;

    private DeferredFileWriter() {
    }

    public static void enqueue(Path path, byte[] bytes, ClassFileEntity owner) {
        if (path == null || bytes == null || bytes.length == 0) {
            return;
        }
        Writer writer = ensureStarted();
        WriteTask task = new WriteTask(path, bytes, owner);
        try {
            writer.queue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeNow(task, writer);
        }
    }

    public static void awaitAndStop() {
        Writer writer = worker;
        if (writer == null) {
            return;
        }
        writer.requestStop();
        try {
            writer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("deferred writer interrupted");
        } finally {
            worker = null;
        }
    }

    private static Writer ensureStarted() {
        Writer current = worker;
        if (current != null) {
            return current;
        }
        synchronized (LOCK) {
            if (worker == null) {
                worker = new Writer();
                worker.start();
            }
            return worker;
        }
    }

    private static void writeNow(WriteTask task, Writer writer) {
        if (task == null) {
            return;
        }
        boolean written = false;
        try {
            Path parent = task.path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(task.path, task.bytes);
            written = true;
        } catch (Exception e) {
            logger.warn("deferred write error: {}", e.toString());
        } finally {
            if (written && task.owner != null) {
                task.owner.clearCachedBytes();
            }
        }
    }

    private static final class Writer extends Thread {
        private final BlockingQueue<WriteTask> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        private final Set<Path> dirCache = new HashSet<>();
        private volatile boolean stopRequested;

        private Writer() {
            super("jar-analyzer-deferred-writer");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    WriteTask task = queue.take();
                    if (task == WriteTask.POISON) {
                        break;
                    }
                    writeTask(task);
                } catch (InterruptedException e) {
                    if (stopRequested) {
                        break;
                    }
                    Thread.currentThread().interrupt();
                }
            }
            drainRemaining();
        }

        private void drainRemaining() {
            WriteTask task;
            while ((task = queue.poll()) != null) {
                if (task == WriteTask.POISON) {
                    continue;
                }
                writeTask(task);
            }
        }

        private void writeTask(WriteTask task) {
            if (task == null || task.path == null || task.bytes == null) {
                return;
            }
            boolean written = false;
            try {
                Path parent = task.path.getParent();
                if (parent != null && (dirCache.isEmpty() || dirCache.add(parent))) {
                    Files.createDirectories(parent);
                }
                Files.write(task.path, task.bytes);
                written = true;
            } catch (Exception e) {
                logger.warn("deferred write error: {}", e.toString());
            } finally {
                if (written && task.owner != null) {
                    task.owner.clearCachedBytes();
                }
            }
        }

        private void requestStop() {
            stopRequested = true;
            try {
                queue.put(WriteTask.POISON);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class WriteTask {
        private static final WriteTask POISON = new WriteTask(null, null, null);
        private final Path path;
        private final byte[] bytes;
        private final ClassFileEntity owner;

        private WriteTask(Path path, byte[] bytes, ClassFileEntity owner) {
            this.path = path;
            this.bytes = bytes;
            this.owner = owner;
        }
    }
}
