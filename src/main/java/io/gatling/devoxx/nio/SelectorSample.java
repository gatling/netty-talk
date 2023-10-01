package io.gatling.devoxx.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SelectorSample {

  public static void main(String[] args) throws IOException {
    var selector = Selector.open();
    var serverSocket = ServerSocketChannel.open();
    serverSocket.bind(new InetSocketAddress(8080));
    serverSocket.configureBlocking(false);
    serverSocket.register(selector, SelectionKey.OP_ACCEPT);

    while (true) {
      selector.select();
      for (var key : selector.selectedKeys()) {
        if (key.isAcceptable()) {
          var clientSocket = serverSocket.accept();
          clientSocket.configureBlocking(false);
          clientSocket.register(selector, SelectionKey.OP_READ);
        }

        if (key.isReadable()) {
          var clientSocket = (SocketChannel) key.channel();
          var buffer = ByteBuffer.allocate(1024);
          // we could read until returns 0
          int r = clientSocket.read(buffer);
          if (r == -1) {
            // EOF
            return;
          }
          buffer.flip();
          // echo
          clientSocket.write(buffer);
        }
      }
    }
  }
}
