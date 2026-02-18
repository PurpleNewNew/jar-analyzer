package me.n1ar4.cb;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CallbackEntry {
    private static final class TaskBox {
        Task task;
    }

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

    public void dynamicProxy() {
        Runnable proxy = (Runnable) Proxy.newProxyInstance(
                CallbackEntry.class.getClassLoader(),
                new Class[]{Runnable.class},
                new MyInvocationHandler());
        proxy.run();
    }

    public void cglibProxy() {
        org.springframework.cglib.proxy.Enhancer.create(new MyCglibInterceptor());
    }

    public void springAopProxy() {
        org.springframework.aop.framework.ProxyFactory factory = new org.springframework.aop.framework.ProxyFactory();
        factory.getProxy();
    }

    public void byteBuddyProxy() {
        new net.bytebuddy.ByteBuddy()
                .subclass(Runnable.class)
                .method(null)
                .make();
    }

    public void reflectViaParams(String className, String methodName) throws Exception {
        Class<?> clazz = Class.forName(className);
        Object instance = clazz.getDeclaredConstructor().newInstance();
        clazz.getMethod(methodName, new Class[0]).invoke(instance, new Object[0]);
    }

    public void ptaFieldSensitiveDispatch() {
        TaskBox box = new TaskBox();
        Task seed = new FastTask();
        box.task = seed;
        Task task = box.task;
        task.run();
    }

    public void ptaArraySensitiveDispatch() {
        Task[] arr = new Task[1];
        Task seed = new FastTask();
        arr[0] = seed;
        Task task = arr[0];
        task.run();
    }

    public void ptaNativeArrayCopyDispatch() {
        Task[] src = new Task[1];
        Task seed = new FastTask();
        src[0] = seed;
        Task[] dst = new Task[1];
        System.arraycopy(src, 0, dst, 0, 1);
        Task task = dst[0];
        task.run();
    }

    public void ptaNoiseInstantiate() {
        Task noise = new SlowTask();
        if (noise == null) {
            noise.run();
        }
    }

    public void reflectWithClassLoaderChain() throws Exception {
        String cls = "me.n1ar4.cb.".concat("ReflectionTarget");
        ClassLoader loader = CallbackEntry.class.getClassLoader();
        Class<?> clazz = Class.forName(cls, true, loader);
        String method = " target ".trim().substring(0, 6);
        clazz.getMethod(method, new Class[0]).invoke(clazz.getDeclaredConstructor().newInstance(), new Object[0]);
    }

    public void reflectViaLoadClassApi() throws Exception {
        String cls = new StringBuilder("me")
                .append(".n1ar4")
                .append(".cb")
                .append(".")
                .append("ReflectionTarget")
                .toString();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = CallbackEntry.class.getClassLoader();
        }
        Class<?> clazz = loader.loadClass(cls.trim());
        String method = "TARGET".toLowerCase();
        clazz.getDeclaredMethod(method, new Class[0]).invoke(clazz.getDeclaredConstructor().newInstance(), new Object[0]);
    }

    public void reflectViaHelperFlow() throws Exception {
        ClassLoader loader = helperLoader();
        String className = helperClassPrefix() + helperClassSuffix();
        Class<?> clazz = loader.loadClass(className);
        String method = helperMethodNameFromSignature().toUpperCase(Locale.ROOT).toLowerCase(Locale.ROOT);
        clazz.getDeclaredMethod(method, new Class[0]).invoke(clazz.getDeclaredConstructor().newInstance(), new Object[0]);
    }

    public void methodHandleViaHelperFlow() throws Throwable {
        ClassLoader loader = helperLoader();
        Class<?> clazz = Class.forName(helperClassPrefix() + helperClassSuffix(), true, loader);
        MethodType mt = MethodType.fromMethodDescriptorString(helperMethodDescriptorFromSignature(), loader);
        MethodHandle mh = MethodHandles.lookup().findVirtual(clazz, helperMethodNameFromSignature(), mt);
        Object receiver = clazz.getDeclaredConstructor().newInstance();
        mh.invoke(receiver);
    }

    private ClassLoader helperLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = CallbackEntry.class.getClassLoader();
        }
        return loader;
    }

    private String helperClassPrefix() {
        return "me.n1ar4.cb.";
    }

    private String helperClassSuffix() {
        return new StringBuilder("Reflection")
                .append("Target")
                .toString();
    }

    private String helperMethodNameFromSignature() {
        String sig = helperMethodSignature();
        return sig.substring(0, sig.indexOf('('));
    }

    private String helperMethodDescriptorFromSignature() {
        String sig = helperMethodSignature();
        return sig.substring(sig.indexOf('('));
    }

    private String helperMethodSignature() {
        return "target()V";
    }
}
