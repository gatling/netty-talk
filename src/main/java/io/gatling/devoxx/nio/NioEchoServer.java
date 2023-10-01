package io.gatling.devoxx.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

public class NioEchoServer implements AutoCloseable {

    public static void main(String[] args) throws IOException {
        try (var server  = new NioEchoServer(8080)) {
            server.start();
            while (true);
        }
    }

    private final int port;
    private Selector selector;
    private ServerSocketChannel serverSocket;

    public NioEchoServer(int port) {
        this.port = port;
    }

    public void start() {
        Thread.ofVirtual().start(() -> serve());
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }

    private void serve() {
        try {
            selector = SelectorProvider.provider().openSelector();
            serverSocket = ServerSocketChannel.open();
            serverSocket.configureBlocking(false);
            serverSocket.bind(new InetSocketAddress(port), 10_000);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            while (serverSocket.isOpen()) {
                if (selector.select() > 0) {
                    var iterator = selector.selectedKeys().iterator();

                    while (iterator.hasNext()) {
                        var key = iterator.next();
                        // WARN consume ready list!!!
                        iterator.remove();
                        if (key.isAcceptable()) {
                            var clientSocket = serverSocket.accept();
                            if (clientSocket != null) {
                                clientSocket.configureBlocking(false);
                                clientSocket.register(selector, SelectionKey.OP_READ);
                            }
                        } else if (key.isReadable()) {
                            var clientSocket = (SocketChannel) key.channel();
                            var buffer = ByteBuffer.allocate(1024);
                            int r = clientSocket.read(buffer);
                            if (r == -1) {
                                // EOF
                                return;
                            }
                            buffer.flip();
                            // WARN actually, must retry until all bytes are written
                            clientSocket.write(buffer);
                            buffer.clear();
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (serverSocket.isOpen()) {
                throw new RuntimeException(e);
            }
        }
    }
}
