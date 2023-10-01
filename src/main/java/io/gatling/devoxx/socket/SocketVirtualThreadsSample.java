package io.gatling.devoxx.socket;

import io.gatling.devoxx.shared.Payload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;

public class SocketVirtualThreadsSample {

  private static void serve(int port) {
    try {
      var serverSocket = new ServerSocket(port);
      serverSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      serverSocket.setOption(StandardSocketOptions.SO_REUSEPORT, true);

      while (true) {
        var socket = serverSocket.accept();
        Thread.startVirtualThread(() -> handle(socket));
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void handle(Socket socket) {
    try (var s = socket) {
      var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      var out = new PrintWriter(socket.getOutputStream());

      var line = in.readLine();
      while (line != null) {
        // GET / HTTP/1.1
        // Host: localhost:8080
        // User-Agent: curl/7.87.0
        // Accept: */*
        // System.out.println(line);
        if (line.isEmpty()) {
          out.println("""
            HTTP/1.1 200 OK
            content-Length: 991
            content-Type: application/json
            
            """ + Payload.JSON_1K);
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
