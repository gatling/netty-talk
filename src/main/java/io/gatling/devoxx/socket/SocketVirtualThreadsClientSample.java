package io.gatling.devoxx.socket;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class SocketVirtualThreadsClientSample {

  private static final LongAdder errorCount = new LongAdder();

    private static void handle(String hostname, int port, int requestsPerConnection) {

        var request = STR. """
            GET /json HTTP/1.1
            host: \{ hostname }:\{ port }
            user-agent: curl/7.87.0
            accept: */*

            """ .stripIndent().getBytes(StandardCharsets.US_ASCII);

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
        } catch (Exception ignore) {
          ignore.printStackTrace();
          requestCount++;
          errorCount.increment();
        }
      }
    }

    public static void main(String[] args) throws Exception {
        // String hostname = args[0];
        //int port = Integer.valueOf(args[1]);
        //int nbConnections = Integer.valueOf(args[2]);
        //int requestsPerConnection = Integer.valueOf(args[3]);

        var hostname = "localhost";
        var port = 8080;
        var nbConnections = 1000;
        var requestsPerConnection = 5000;

        var latch = new CountDownLatch(nbConnections);

        var start = System.nanoTime();
        for (int i = 0; i < nbConnections; i++) {
            Thread.startVirtualThread(() -> {
                handle(hostname, port, requestsPerConnection);
                latch.countDown();
            });
        }

        latch.await(2, TimeUnit.MINUTES);
        var durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        var requestCount = nbConnections * requestsPerConnection;
        var throughput = (double) requestCount / durationMs * 1000;
        System.out.println("Performed " + requestCount + " requests in " + durationMs + "ms, avg throughput=" + throughput + " errorCount=" + errorCount.sum());
    }
}
