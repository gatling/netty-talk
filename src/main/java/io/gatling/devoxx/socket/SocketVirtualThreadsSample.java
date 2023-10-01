package io.gatling.devoxx.socket;

import io.gatling.devoxx.shared.Payload;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.charset.StandardCharsets;

import static java.lang.StringTemplate.STR;

public class SocketVirtualThreadsSample {

  private static void serve(int port) {
    try {
      var serverSocket = new ServerSocket(port);
      //serverSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      //serverSocket.setOption(StandardSocketOptions.SO_REUSEPORT, true);

      while (true) {
        var socket = serverSocket.accept();
        Thread.startVirtualThread(() -> handle(socket));
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String HTTP_RESPONSE =
          STR."""
            HTTP/1.1 200 OK
            content-length: \{Payload.JSON_1K.length()}
            content-type: application/json

            \{Payload.JSON_1K}""";

  private static void handle(Socket socket) {
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
        //System.out.println(line);
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

  public static void main(String[] args) throws Exception {
    Thread.startVirtualThread(() -> serve(8080));
    while (true) {
      Thread.sleep(1_000);
    }
  }
}
