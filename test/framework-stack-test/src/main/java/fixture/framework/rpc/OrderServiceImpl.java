package fixture.framework.rpc;

import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class OrderServiceImpl implements OrderService {
    @Override
    public String submit(String orderId) {
        return "accepted:" + orderId;
    }
}
