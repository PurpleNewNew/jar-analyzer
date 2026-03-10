package fixture.gadget.rome;

import com.sun.syndication.feed.impl.ToStringBean;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class RomePayload implements Serializable {
    public void trigger() throws IOException, ClassNotFoundException {
        readObject(null);
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        new BadAttributeBridge().replay(new ToStringBean(new GetterBean(), "getOutputProperties"));
    }
}
