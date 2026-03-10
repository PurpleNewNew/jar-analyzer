package org.apache.commons.collections.functors;

import org.apache.commons.collections.Transformer;
import ysoserial.secmgr.ExecSink;

import java.io.Serializable;

public class InvokerTransformer implements Transformer, Serializable {
    private final String command;

    public InvokerTransformer(String command) {
        this.command = command;
    }

    @Override
    public Object transform(Object input) {
        String suffix = input == null ? "null" : input.toString();
        return ExecSink.exec(command + ":" + suffix);
    }
}
