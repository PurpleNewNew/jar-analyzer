package fixture.gadget.cc;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections4.functors.InvokerTransformer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class CcPayload implements Serializable {
    private final QueueBridge queue = new QueueBridge();
    private final BeanComparator comparator = new BeanComparator(new InvokerTransformer("exec"));

    public void trigger() throws IOException, ClassNotFoundException {
        readObject(null);
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        queue.replay(comparator, "left", "right");
    }
}
