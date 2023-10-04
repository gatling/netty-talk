package io.gatling.devoxx.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class NettyHttpClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyHttpClient.class);

    private class AppHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final int userId;

        private final Runnable reconnect;

        private AppHandler(int userId, Runnable reconnect) {
            this.userId = userId;
            this.reconnect = reconnect;
        }

        private void sendRequest(ChannelHandlerContext ctx) {
            ctx.writeAndFlush(request()).addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.debug("USER {} WRITE FAILED {}", userId, future.cause().getMessage());
                    registerError("Write failure", future.cause());
                    if (ctx.channel().isActive()) {
                        ctx.close();
                    }
                    reconnect.run();
                }
            });
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            LOGGER.debug("USER {} ACTIVE", userId);
            sendRequest(ctx);
        }

        private FullHttpRequest request() {
            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/json", Unpooled.EMPTY_BUFFER);
            request.headers()
                    .add(HttpHeaderNames.HOST, hostname)
                    .add(HttpHeaderNames.ACCEPT, "*/*")
                    .add(HttpHeaderNames.USER_AGENT, "curl/7.87.0")
                    .add("x-user", userId);
            return request;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
            LOGGER.debug("USER {} RESPONSE", userId);
            if (state.get(userId).decrementAndGet() == 0) {
                state.remove(userId);

                var remaining = state.size();
                LOGGER.debug("USER {} DONE remaining={} errors={}", userId, remaining, printErrors());
                if (remaining < 10) {
                    LOGGER.debug("REMAINING {}", state.keySet());
                }

                ctx.close();

                if (state.isEmpty()) {
                    finishedLatch.countDown();
                }
            } else {
                sendRequest(ctx);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOGGER.debug("USER {} CRASHED {}", userId, cause.getMessage());
            registerError("Exception caught", cause);
            if (ctx.channel().isActive()) {
                ctx.close();
            }
            reconnect.run();
        }
    }

    private EventLoopGroup group = Transport.RESOLVED.newEventLoopGroup(Runtime.getRuntime().availableProcessors());

    @Override
    public void close() throws Exception {
        LOGGER.info("client shutdownGracefully");
        group.shutdownGracefully().sync();
    }

    private final String hostname;
    private final int port;
    private final int nbConnections;
    private final int requestsPerConnection;

    private final CountDownLatch finishedLatch = new CountDownLatch(1);

    private final ConcurrentHashMap<Integer, AtomicInteger> state = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, AtomicInteger> errors = new ConcurrentHashMap<>();

    private void registerError(String type, Throwable cause) {
        errors.computeIfAbsent(type + ": " + cause.getMessage(), s -> new AtomicInteger(0)).incrementAndGet();
    }

    private String printErrors() {
        return errors.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue().get()).collect(Collectors.joining(",\n", "[", "]"));
    }

    public NettyHttpClient(String hostname, int port, int nbConnections, int requestsPerConnection) {
        this.hostname = hostname;
        this.port = port;
        this.nbConnections = nbConnections;
        this.requestsPerConnection = requestsPerConnection;
        for (int i = 0; i < nbConnections; i++) {
            state.put(i, new AtomicInteger(requestsPerConnection));
        }
    }

    public void run(Duration timeout) throws Exception {
        var bootstrap = new Bootstrap()
                .group(group)
                .channel(Transport.RESOLVED.channelClass());

        var start = System.nanoTime();

        for (int i = 0; i < nbConnections; i++) {
            var userId = i;
            var userBootstrap = bootstrap
                    .clone()
                    .remoteAddress(new InetSocketAddress(port));
            userBootstrap
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            if (SslContexts.RESOLVED_FOR_CLIENT != null) {
                                ch.pipeline()
                                        .addLast(SslContexts.RESOLVED_FOR_CLIENT.newHandler(ch.alloc()));
                            }

                            ch.pipeline()
                                    .addLast("client", new HttpClientCodec())
                                    .addLast("aggregator", new HttpObjectAggregator(8192))
                                    .addLast("handler", new AppHandler(userId, () -> connect(userId, userBootstrap)));
                        }
                    });

            connect(userId, userBootstrap);
        }

        if (!finishedLatch.await(timeout.toSeconds(), TimeUnit.SECONDS)) {
            throw new TimeoutException("Failed to complete all requests within " + timeout);
        }
        var durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        var requestCount = nbConnections * requestsPerConnection;
        var throughput = (double) requestCount / durationMs * 1000;
        LOGGER.info("Performed {} requests in {}ms, avg throughput={}", requestCount, durationMs, throughput);

        if (!errors.isEmpty()) {
            throw new Exception("Experienced errors:\n" + printErrors());
        }

    }

    private void connect(int userId, Bootstrap bootstrap) {
        bootstrap.connect().addListener((ChannelFuture future) -> {
            if (!future.isSuccess()) {
                LOGGER.debug("USER {} failed to open channel: {}", userId, future.exceptionNow().getMessage());
                registerError("Connect failure", future.cause());
                connect(userId, bootstrap);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        // String hostname = args[0];
        //int port = Integer.valueOf(args[1]);
        //int nbConnections = Integer.valueOf(args[2]);
        //int requestsPerConnection = Integer.valueOf(args[3]);

        var hostname = "localhost";
        var port = 8080;
        var nbConnections = 200;
        var requestsPerConnection = 1;

        try (var client = new NettyHttpClient(hostname, port, nbConnections, requestsPerConnection)) {
            client.run(Duration.ofMinutes(1));
        }
    }
}
