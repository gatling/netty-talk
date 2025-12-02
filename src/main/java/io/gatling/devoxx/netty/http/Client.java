package io.gatling.devoxx.netty.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class Client implements AutoCloseable {

    private final EventLoopGroup group = new NioEventLoopGroup();

    private final String hostname;
    private final int port;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public void close() throws Exception {
        group.shutdownGracefully().sync();
    }

    public void run(int nbConnections, int requestsPerConnection) throws Exception {

        CountDownLatch latch = new CountDownLatch(nbConnections);

        var bootstrap = new Bootstrap()
                .group(group)
                .option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .channel(NioSocketChannel.class);

        for (int i = 0; i < nbConnections; i++) {
            bootstrap
                    .clone()
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline()
                                    .addLast(new HttpClientCodec(),
                                            new HttpObjectAggregator(Integer.MAX_VALUE),
                                            new AppHandler(latch, requestsPerConnection));
                        }
                    })
                    .connect(new InetSocketAddress(port));
        }

        if (!latch.await(2, TimeUnit.MINUTES)) {
            throw new TimeoutException();
        }
    }


    private final class AppHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final CountDownLatch latch;

        private int remaining;

        private AppHandler(CountDownLatch latch, int requestsPerConnection) {
            this.latch = latch;
            this.remaining = requestsPerConnection;
        }

        private void sendRequest(ChannelHandlerContext ctx) {
            ctx.writeAndFlush(request());
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            sendRequest(ctx);
        }

        private FullHttpRequest request() {
            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/", Unpooled.EMPTY_BUFFER);
            request.headers()
                    .add(HttpHeaderNames.HOST, hostname)
                    .add(HttpHeaderNames.ACCEPT, "*/*")
                    .add(HttpHeaderNames.USER_AGENT, "curl/7.87.0");
            return request;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
            remaining--;

            if (remaining == 0) {
                ctx.close();
                latch.countDown();
            } else {
                sendRequest(ctx);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (ctx.channel().isActive()) {
                cause.printStackTrace();
                ctx.close();
                latch.countDown();
            }
        }
    }
}
