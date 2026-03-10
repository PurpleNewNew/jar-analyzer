package fixture.framework.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class AuthHandler extends SimpleChannelInboundHandler<Object> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        onMessage(String.valueOf(msg));
    }

    private void onMessage(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value");
        }
    }
}
