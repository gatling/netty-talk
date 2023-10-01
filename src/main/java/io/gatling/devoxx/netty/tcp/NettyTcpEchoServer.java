package io.gatling.devoxx.netty.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class NettyTcpEchoServer {

  @ChannelHandler.Sharable
  static class EchoHandler extends ChannelInboundHandlerAdapter {

    private static final EchoHandler INSTANCE = new EchoHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      ByteBuf in = (ByteBuf) msg;

      ByteBuf dup = in.duplicate();
      String content = dup.toString(StandardCharsets.UTF_8);
      System.out.println(content);
      if (content.equals("close\r\n")) {
        in.release();
        ctx.channel().parent().close();
      } else {
        ctx.writeAndFlush(in);
      }
    }
  }

  public static void main(String[] args) throws InterruptedException {
    EventLoopGroup parentGroup = new NioEventLoopGroup(1);
    EventLoopGroup childGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());

    try {
      ServerBootstrap b = new ServerBootstrap()
        .group(parentGroup, childGroup)
        .channel(NioServerSocketChannel.class)
        .localAddress(new InetSocketAddress(8080))
        .childHandler(EchoHandler.INSTANCE);

      ChannelFuture f = b.bind().sync();
      f.channel().closeFuture().sync();
    } finally {
      childGroup.shutdownGracefully().sync();
      parentGroup.shutdownGracefully().sync();
    }
  }
}
