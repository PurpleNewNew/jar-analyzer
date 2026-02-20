/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage;

public interface AuditStore {
    String engineName();

    void start();

    void stop();
}
