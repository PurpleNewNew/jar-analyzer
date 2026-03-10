package io.netty.channel;

public abstract class SimpleChannelInboundHandler<I> implements ChannelInboundHandler {
    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        channelRead0(ctx, (I) msg);
    }

    protected abstract void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception;
}
