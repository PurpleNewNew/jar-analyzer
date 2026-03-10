package fixture.gadget.rome;

import com.sun.syndication.feed.impl.ToStringBean;

import javax.management.BadAttributeValueExpException;
import java.io.Serializable;

public class BadAttributeBridge extends BadAttributeValueExpException implements Serializable {
    public BadAttributeBridge() {
        super(null);
    }

    public String replay(ToStringBean bean) {
        return bean.toString();
    }
}
