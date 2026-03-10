package ysoserial.payloads.util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.PriorityQueue;

public class PriorityQueueBridge extends PriorityQueue<Object> implements Serializable {
    public int replay(Comparator<Object> comparator, Object left, Object right) {
        return comparator.compare(left, right);
    }
}
