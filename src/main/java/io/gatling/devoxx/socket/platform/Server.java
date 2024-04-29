package io.gatling.devoxx.socket.platform;

import io.gatling.devoxx.shared.Payload;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Server implements AutoCloseable {

    private final int port;

    private ServerSocket serverSocket;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        Thread.ofPlatform().start(() -> serve());
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }

    private void serve() {
        try {
            serverSocket = new ServerSocket(port, 10_000);

            while (!serverSocket.isClosed()) {
                var socket = serverSocket.accept();
                Thread.ofPlatform().start(() -> handle(socket));
            }

        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final String HTTP_RESPONSE = "HTTP/1.1 200 OK\ncontent-length: " + Payload.JSON_1K.length() + "\ncontent-type: application/json\n\n" + Payload.JSON_1K;

    private void handle(Socket socket) {
        try (var s = socket) {
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            var out = new BufferedOutputStream(socket.getOutputStream());

            var line = in.readLine();
            while (line != null) {
                // note: we're cheating here and don't parse the HTTP request, just waiting for the empty line at the end of the request
                // GET / HTTP/1.1
                // Host: localhost:8080
                // User-Agent: curl/7.87.0
                // Accept: */*
                if (line.isEmpty()) {
                    out.write(HTTP_RESPONSE.getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                }
                line = in.readLine();
            }
        } catch (Exception ignore) {
            // auto-close
        }
    }
}
