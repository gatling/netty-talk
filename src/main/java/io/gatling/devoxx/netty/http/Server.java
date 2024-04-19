package io.gatling.devoxx.netty.http;

import io.gatling.devoxx.shared.Payload;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ForkJoinPool;

class Server implements AutoCloseable {

    private final EventLoopGroup parentGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup childGroup = new NioEventLoopGroup();

    private final SslContext sslContext;

    public Server() throws Exception {
        // create the SslContext
        var selfSignedCertificate = new SelfSignedCertificate();
        sslContext = SslContextBuilder
                .forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
                .build();
    }

    public Channel start(int port) throws InterruptedException {
        var bootstrap = new ServerBootstrap()
                .group(parentGroup, childGroup)
                .option(ChannelOption.SO_BACKLOG, 15 * 1024)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        // create a SSLEngine for this channel
                        var sslEngine = sslContext.newEngine(ch.alloc());
                        // WARNING: on the client side, you MUST enable hostname verification in production
                        // sslEngine.getSSLParameters().setEndpointIdentificationAlgorithm("HTTPS");
                        ch.pipeline()
                                .addLast(
                                        // add the SslHandler and offload to the FJP
                                        new SslHandler(sslEngine, ForkJoinPool.commonPool()),
                                        new HttpServerCodec(),
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
