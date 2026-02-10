package me.n1ar4.cb;

import java.security.AccessController;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CallbackEntry {
    public void threadStart() {
        new MyThread().start();
    }

    public void executorSubmit() {
        ExecutorService es = Executors.newSingleThreadExecutor();
        try {
            es.submit(new MyRunnable());
        } finally {
            es.shutdown();
        }
    }

    public Object doPrivileged() {
        return AccessController.doPrivileged(new MyAction());
    }

    public void completableSupply() {
        CompletableFuture.supplyAsync(new MySupplier());
    }

    public void reflectInvoke() throws Exception {
        ReflectionTarget.class.getMethod("target", new Class[0])
                .invoke(new ReflectionTarget(), new Object[0]);
    }
}
