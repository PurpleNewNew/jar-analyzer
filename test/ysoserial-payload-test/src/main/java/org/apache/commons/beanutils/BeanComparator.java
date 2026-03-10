package org.apache.commons.beanutils;

import org.apache.commons.collections.Transformer;

import java.io.Serializable;
import java.util.Comparator;

public class BeanComparator implements Comparator<Object>, Serializable {
    private final Transformer transformer;

    public BeanComparator(Transformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public int compare(Object left, Object right) {
        Object leftValue = transformer == null ? left : transformer.transform(left);
        Object rightValue = transformer == null ? right : transformer.transform(right);
        return String.valueOf(leftValue).compareTo(String.valueOf(rightValue));
    }
}
