package fixture.gadget.proxy;

import fixture.gadget.sink.RuntimeSink;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class JdkProxyHandler implements InvocationHandler, Serializable {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return RuntimeSink.exec(method == null ? "invoke" : method.getName());
    }
}
