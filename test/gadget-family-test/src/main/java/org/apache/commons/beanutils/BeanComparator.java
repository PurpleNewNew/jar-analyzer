package org.apache.commons.beanutils;

import org.apache.commons.collections4.Transformer;

import java.io.Serializable;
import java.util.Comparator;

public class BeanComparator implements Comparator<Object>, Serializable {
    private final Transformer transformer;

    public BeanComparator(Transformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public int compare(Object left, Object right) {
        transformer.transform(left);
        return 0;
    }
}
