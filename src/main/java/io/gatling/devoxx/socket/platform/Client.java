package io.gatling.devoxx.socket.platform;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Client {

    private final String hostname;
    private final int port;
    private final byte[] request;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.request = ("GET /json HTTP/1.1\nhost: " + hostname + ":" + port + "\nuser-agent: curl/7.87.0\naccept: */*\n\n").getBytes(StandardCharsets.US_ASCII);
    }

    public void run(int nbConnections, int requestsPerConnection) throws Exception {
        var latch = new CountDownLatch(nbConnections);
        for (int i = 0; i < nbConnections; i++) {
            Thread.ofPlatform().start(() -> {
                handle(hostname, port, requestsPerConnection);
                latch.countDown();
            });
        }
        latch.await(2, TimeUnit.MINUTES);
    }

    private void handle(String hostname, int port, int requestsPerConnection) {
      var requestCount = 0;
      while (requestCount < requestsPerConnection) { // reconnect
        try (var socket = new Socket(hostname, port)) {
          var out = new BufferedOutputStream(socket.getOutputStream());
          var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));

          while (requestCount < requestsPerConnection) {
            out.write(request);
            out.flush();

            var line = in.readLine();
            while (line != null) {
              if (line.equals("}")) {
                requestCount++;
                break;
              }
              line = in.readLine();
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          requestCount++;
        }
      }
    }
}
