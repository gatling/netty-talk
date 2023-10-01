package io.gatling.devoxx.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.incubator.channel.uring.IOUring;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;
import io.netty.incubator.channel.uring.IOUringSocketChannel;

enum Transport {

    NIO {
        @Override
        EventLoopGroup newEventLoopGroup(int nThreads) {
            return new NioEventLoopGroup(nThreads);
        }


        @Override
        Class<? extends ServerChannel> serverChannelClass() {
            return NioServerSocketChannel.class;
        }

        @Override
        Class<? extends SocketChannel> channelClass() {
            return NioSocketChannel.class;
        }
    },

    EPOLL {
        @Override
        EventLoopGroup newEventLoopGroup(int nThreads) {
            return new EpollEventLoopGroup(nThreads);
        }

        @Override
        Class<? extends ServerChannel> serverChannelClass() {
            return EpollServerSocketChannel.class;
        }

        @Override
        Class<? extends SocketChannel> channelClass() {
            return EpollSocketChannel.class;
        }
    },

    KQUEUE {
        @Override
        EventLoopGroup newEventLoopGroup(int nThreads) {
            return new KQueueEventLoopGroup(nThreads);
        }

        @Override
        Class<? extends ServerChannel> serverChannelClass() {
            return KQueueServerSocketChannel.class;
        }

        @Override
        Class<? extends SocketChannel> channelClass() {
            return KQueueSocketChannel.class;
        }
    },

    IOURING {
        @Override
        EventLoopGroup newEventLoopGroup(int nThreads) {
            return new IOUringEventLoopGroup(nThreads);
        }

        @Override
        Class<? extends ServerChannel> serverChannelClass() {
            return IOUringServerSocketChannel.class;
        }

        @Override
        Class<? extends SocketChannel> channelClass() {
            return IOUringSocketChannel.class;
        }
    };

    abstract EventLoopGroup newEventLoopGroup(int nThreads);

    abstract Class<? extends ServerChannel> serverChannelClass();

    abstract Class<? extends SocketChannel> channelClass();

    static Transport RESOLVED = resolve();

    private static Transport resolve() {
        String transportProp = System.getProperty("transport");
        Transport transport;
        switch (transportProp) {
            case "epoll" -> {
                Epoll.ensureAvailability();
                transport = Transport.EPOLL;
            }
            case "iouring" -> {
                IOUring.ensureAvailability();
                transport = Transport.IOURING;
            }
            case "kqueue" -> {
                KQueue.ensureAvailability();
                transport = Transport.KQUEUE;
            }
            case null -> transport = Transport.NIO;
            case "nio" -> transport = Transport.NIO;
            default -> throw new IllegalArgumentException("Unknown value for System property transport: " + transportProp);
        }
        return transport;
    }
}
