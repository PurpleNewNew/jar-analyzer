package fixture.gadget.rome;

import fixture.gadget.sink.RuntimeSink;

public class GetterBean {
    public String getOutputProperties() {
        return RuntimeSink.exec("rome");
    }
}
