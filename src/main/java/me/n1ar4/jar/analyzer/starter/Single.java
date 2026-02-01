/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.starter;

import javax.swing.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class Single {
    private static final String LOCK_FILE = "jar-analyzer-lockfile";
    private static RandomAccessFile lockFile;
    private static FileLock lock;

    public static boolean canRun() {
        if (!isInstanceRunning()) {
            return true;
        } else {
            JOptionPane.showMessageDialog(null,
                    "Jar Analyzer is running");
            return false;
        }
    }

    private static boolean isInstanceRunning() {
        try {
            if (lock != null && lock.isValid()) {
                return false;
            }
            lockFile = new RandomAccessFile(LOCK_FILE, "rw");
            lock = lockFile.getChannel().tryLock();
            if (lock == null) {
                lockFile.close();
                lockFile = null;
                return true;
            }
            return false;
        } catch (OverlappingFileLockException | IOException e) {
            return true;
        }
    }
}
