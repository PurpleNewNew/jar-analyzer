package org.apache.commons.collections4.functors;

import fixture.gadget.sink.RuntimeSink;
import org.apache.commons.collections4.Transformer;

import java.io.Serializable;

public class InvokerTransformer implements Transformer, Serializable {
    private final String methodName;

    public InvokerTransformer(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public Object transform(Object input) {
        return RuntimeSink.exec(methodName + ":" + input);
    }
}
