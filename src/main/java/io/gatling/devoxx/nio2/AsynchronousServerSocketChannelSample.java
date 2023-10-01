package io.gatling.devoxx.nio2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

public class AsynchronousServerSocketChannelSample {

  public static void main(String[] args) throws IOException {
    var serverSocketChannel = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(8080));

    serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {

      @Override
      public void completed(AsynchronousSocketChannel clientSocketChannel, Void attachment) {
        read(clientSocketChannel);
      }

      @Override
      public void failed(Throwable exc, Void attachment) {
        // TODO
      }
    });
  }

  private static void read(AsynchronousSocketChannel clientSocketChannel) {

    var buffer = ByteBuffer.allocate(1024);
    clientSocketChannel.read(buffer, 20, TimeUnit.SECONDS, null, new CompletionHandler<>() {
      @Override
      public void completed(Integer result, Object attachment) {
        if (result == -1) {
          // EOF
          return;
        }

        buffer.flip();
        // echo
        clientSocketChannel.write(buffer, null, new CompletionHandler<>() {
          @Override
          public void completed(Integer result, Object attachment) {
            // go back to read
            read(clientSocketChannel);
          }

          @Override
          public void failed(Throwable exc, Object attachment) {
            try {
              clientSocketChannel.close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });
      }

      @Override
      public void failed(Throwable exc, Object attachment) {
        // TODO
        try {
          clientSocketChannel.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }
}
