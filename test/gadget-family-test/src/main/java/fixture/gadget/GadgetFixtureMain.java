package fixture.gadget;

import fixture.gadget.cc.CcPayload;
import fixture.gadget.proxy.ProxyPayload;
import fixture.gadget.rome.RomePayload;

public final class GadgetFixtureMain {
    private GadgetFixtureMain() {
    }

    public static void main(String[] args) throws Exception {
        new CcPayload().trigger();
        new ProxyPayload().trigger();
        new RomePayload().trigger();
    }
}
