package io.gatling.devoxx.netty;

import io.gatling.devoxx.shared.Payload;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ForkJoinPool;

public class NettyHttpServer implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyHttpServer.class);

    @ChannelHandler.Sharable
    private static class AppHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private static final AppHandler INSTANCE = new AppHandler();

        @Override
        public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            if (request.uri().equals("/json")) {
                LOGGER.debug("USER {} REQUEST", request.headers().get("x-user"));
                var byteBuf = ctx.alloc().buffer(Payload.JSON_1K.length());
                byteBuf.writeCharSequence(Payload.JSON_1K, StandardCharsets.US_ASCII);
                var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
                response.headers().add(HttpHeaderNames.CONTENT_LENGTH, byteBuf.readableBytes());
                ctx.writeAndFlush(response);
            } else {
                var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, Unpooled.EMPTY_BUFFER);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private EventLoopGroup parentGroup = Transport.RESOLVED.newEventLoopGroup(1);
    private EventLoopGroup childGroup = Transport.RESOLVED.newEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

    public Channel start(int port) throws InterruptedException {
        var bootstrap = new ServerBootstrap()
                .group(parentGroup, childGroup)
                .channel(Transport.RESOLVED.serverChannelClass())
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        if (SslContexts.RESOLVED_FOR_SERVER != null) {
                            ch.pipeline()
                                    .addLast(SslContexts.RESOLVED_FOR_SERVER.newHandler(ch.alloc(), ForkJoinPool.commonPool()));
                        }

                        ch.pipeline()
                                .addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192, false))
                                .addLast("aggregator", new HttpObjectAggregator(30000))
                                .addLast("encoder", new HttpResponseEncoder())
                                .addLast("handler", AppHandler.INSTANCE);
                    }
                });

        var channelFuture = bootstrap.bind().sync();
        return channelFuture.channel();
    }

    @Override
    public void close() throws Exception {
        LOGGER.info("server shutdownGracefully");
        childGroup.shutdownGracefully().sync();
        parentGroup.shutdownGracefully().sync();
    }

    public static void main(String[] args) throws Exception {
        try(var server = new NettyHttpServer()) {
            server.start(8080).closeFuture().sync();
        }
    }
}
