package io.gatling.devoxx.netty.http;

import java.time.Duration;

public class NettyHttp {
    public static void main(String[] args) throws Exception {

        var hostname = "localhost";
        var port = 8080;
        var nbConnections = 1_000;
        var requestsPerConnection = 5_000;

        var start = System.nanoTime();
        try(var server = new Server()) {
            server.start(8080);

            try (var client = new Client(hostname, port)) {
                client.run(nbConnections, requestsPerConnection);
            }
        }
        var durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        var requestCount = nbConnections * requestsPerConnection;
        var throughput = (double) requestCount / durationMs * 1000;
        System.out.printf("NettyHttp: performed %d requests in %d ms, avg throughput=%.2f rps", requestCount, durationMs, throughput);
    }
}
