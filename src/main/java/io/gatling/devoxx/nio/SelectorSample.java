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
    serverSocket.register(selector, SelectionKey.OP_ACCEPT);

    while (true) {
      selector.select();
      for (var key : selector.selectedKeys()) {
        if (key.isAcceptable()) {
          var clientSocket = serverSocket.accept();
          clientSocket.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
          var clientSocket = (SocketChannel) key.channel();
          var buffer = ByteBuffer.allocate(1024);
          int r = clientSocket.read(buffer);
          if (r == -1) {
            // EOF
            return;
          }
          buffer.flip();
          clientSocket.write(buffer);
        }
      }
    }
  }
}
