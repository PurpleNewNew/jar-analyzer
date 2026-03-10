package ysoserial.payloads.util;

import javax.management.BadAttributeValueExpException;
import java.io.Serializable;

public class RomeBadAttributeValueExpException extends BadAttributeValueExpException implements Serializable {
    public RomeBadAttributeValueExpException() {
        super("rome");
    }

    public String replay(Object value) {
        return value == null ? "" : value.toString();
    }
}
