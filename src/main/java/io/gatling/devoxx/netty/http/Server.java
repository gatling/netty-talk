package io.gatling.devoxx.netty.http;

import io.gatling.devoxx.shared.Payload;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

class Server implements AutoCloseable {

    private final EventLoopGroup parentGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup childGroup = new NioEventLoopGroup();

    public Channel start(int port) throws InterruptedException {
        var bootstrap = new ServerBootstrap()
                .group(parentGroup, childGroup)
                .option(ChannelOption.SO_BACKLOG, 15 * 1024)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline()
                                .addLast(new HttpServerCodec(),
                                        new HttpObjectAggregator(Integer.MAX_VALUE),
                                        AppHandler.INSTANCE);
                    }
                });

        var channelFuture = bootstrap.bind().sync();
        return channelFuture.channel();
    }

    @Override
    public void close() throws Exception {
        childGroup.shutdownGracefully().sync();
        parentGroup.shutdownGracefully().sync();
    }

    @ChannelHandler.Sharable
    private static class AppHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private static final AppHandler INSTANCE = new AppHandler();

        @Override
        public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            ByteBuf responseBody = ctx.alloc().buffer();
            responseBody.writeCharSequence(Payload.JSON_1K, StandardCharsets.UTF_8);
            var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, responseBody);
            response.headers().add(HttpHeaderNames.CONTENT_LENGTH, responseBody.readableBytes());
            ctx.writeAndFlush(response);
        }
    }

    public static void main(String[] args) throws Exception {
        var server = new Server();
        server.start(8080);
        while (true);
    }
}
