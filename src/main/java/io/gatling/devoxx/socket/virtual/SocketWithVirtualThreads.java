package io.gatling.devoxx.socket.virtual;

import java.time.Duration;

public class SocketWithVirtualThreads {

    public static void main(String[] args) throws Exception {
        var nbConnections = 10_000;
        var requestsPerConnection = 500;
        var port = 8080;

        try (var server = new Server(port)) {
            server.start();
            var client = new Client("localhost", port);

            var start = System.nanoTime();
            client.run(nbConnections, requestsPerConnection);

            var durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
            var requestCount = nbConnections * requestsPerConnection;
            var throughput = (double) requestCount / durationMs * 1000;
            System.out.printf("SocketWithVirtualThreads: performed %d requests in %d ms, avg throughput=%.2f rps", requestCount, durationMs, throughput);
        }
    }
}
