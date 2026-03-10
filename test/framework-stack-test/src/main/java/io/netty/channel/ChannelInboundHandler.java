package io.netty.channel;

public interface ChannelInboundHandler {
    void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;
}
