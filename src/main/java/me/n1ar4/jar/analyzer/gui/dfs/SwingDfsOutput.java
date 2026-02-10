/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.dfs;

import me.n1ar4.jar.analyzer.dfs.DFSEdge;
import me.n1ar4.jar.analyzer.dfs.DfsOutput;
import me.n1ar4.jar.analyzer.gui.ChainsResultPanel;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.Timer;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Swing-based DFS output implementation.
 * <p>
 * This class keeps Swing dependencies out of engine/server code.
 */
public final class SwingDfsOutput implements DfsOutput {
    private static final Logger logger = LogManager.getLogger();

    // Reduce EDT churn: batch many updates into a smaller number of invokeLater flushes.
    private static final int BATCH_SIZE = 200;
    private static final int MAX_FLUSH = 400;
    private static final long FLUSH_INTERVAL_MS = 40L;
    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final AtomicBoolean flushScheduled = new AtomicBoolean(false);
    private final AtomicLong lastFlushMs = new AtomicLong(0L);
    private final Object delayLock = new Object();
    private Timer delayedFlushTimer;

    private final JTextArea textArea;
    private final ChainsResultPanel chainsPanel;

    private SwingDfsOutput(JTextArea textArea, ChainsResultPanel chainsPanel) {
        this.textArea = textArea;
        this.chainsPanel = chainsPanel;
    }

    public static SwingDfsOutput forTextArea(JTextArea textArea) {
        return new SwingDfsOutput(textArea, null);
    }

    public static SwingDfsOutput forChainsPanel(ChainsResultPanel panel) {
        return new SwingDfsOutput(null, panel);
    }

    @Override
    public void clear() {
        enqueueUi(() -> {
            if (textArea != null) {
                textArea.setText("");
            }
            if (chainsPanel != null) {
                chainsPanel.clear();
            }
        });
    }

    @Override
    public void onMessage(String msg) {
        if (msg == null) {
            return;
        }
        enqueueUi(() -> {
            if (textArea != null) {
                textArea.append(msg);
                textArea.append("\n");
                return;
            }
            if (chainsPanel != null) {
                chainsPanel.append(msg);
            }
        });
    }

    @Override
    public void onChainFound(String chainId,
                             String title,
                             List<String> methods,
                             List<DFSEdge> edges,
                             boolean showEdgeMeta) {
        enqueueUi(() -> {
            if (chainsPanel != null) {
                chainsPanel.addChain(chainId, title, methods, edges, showEdgeMeta);
                return;
            }
            if (textArea != null) {
                if (title != null) {
                    textArea.append(title);
                    textArea.append("\n");
                }
                if (methods != null) {
                    for (int i = 0; i < methods.size(); i++) {
                        String method = methods.get(i);
                        String arrow = (i == 0) ? "" : " -> ";
                        textArea.append(arrow);
                        if (method != null) {
                            textArea.append(method);
                        }
                        textArea.append("\n");
                    }
                }
                textArea.append("\n");
            }
        });
    }

    private void enqueueUi(Runnable action) {
        if (action == null) {
            return;
        }
        queue.add(action);
        int count = queueSize.incrementAndGet();
        scheduleFlush(count, false);
    }

    private void scheduleFlush(int count, boolean force) {
        long now = System.currentTimeMillis();
        long last = lastFlushMs.get();
        if (!force && count < BATCH_SIZE && now - last < FLUSH_INTERVAL_MS) {
            scheduleDelayedFlush(now, last);
            return;
        }
        if (!flushScheduled.compareAndSet(false, true)) {
            return;
        }
        // Always defer to EDT queue to avoid deep recursion when flush triggers another flush.
        SwingUtilities.invokeLater(() -> {
            flushScheduled.set(false);
            flushQueue();
        });
    }

    private void scheduleDelayedFlush(long now, long last) {
        int delay = (int) Math.max(1L, FLUSH_INTERVAL_MS - (now - last));
        synchronized (delayLock) {
            if (delayedFlushTimer == null) {
                delayedFlushTimer = new Timer(delay, e -> scheduleFlush(queueSize.get(), true));
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

    private void flushQueue() {
        int processed = 0;
        Runnable task;
        while (processed < MAX_FLUSH && (task = queue.poll()) != null) {
            queueSize.decrementAndGet();
            try {
                task.run();
            } catch (Throwable t) {
                InterruptUtil.restoreInterruptIfNeeded(t);
                if (t instanceof Error) {
                    throw (Error) t;
                }
                logger.debug("swing dfs output task failed: {}", t.toString());
            }
            processed++;
        }
        if (processed > 0) {
            lastFlushMs.set(System.currentTimeMillis());
            // Keep caret updates out of per-message runnables.
            if (textArea != null) {
                try {
                    textArea.setCaretPosition(textArea.getDocument().getLength());
                } catch (Throwable t) {
                    InterruptUtil.restoreInterruptIfNeeded(t);
                }
            }
        }
        if (!queue.isEmpty()) {
            scheduleFlush(queueSize.get(), true);
        }
    }

    private static void runOnEdt(Runnable action) {
        if (action == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
