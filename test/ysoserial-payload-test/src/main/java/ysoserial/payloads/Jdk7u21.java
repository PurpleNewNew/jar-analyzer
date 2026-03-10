package ysoserial.payloads;

import ysoserial.payloads.util.MemoizedInvocationHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;

public class Jdk7u21 implements Serializable {
    public void trigger() throws IOException, ClassNotFoundException {
        readObject(null);
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        InvocationHandler handler = new MemoizedInvocationHandler();
        try {
            handler.invoke(this, Object.class.getMethod("toString"), new Object[0]);
        } catch (Throwable ex) {
            throw new IOException(ex);
        }
    }
}
