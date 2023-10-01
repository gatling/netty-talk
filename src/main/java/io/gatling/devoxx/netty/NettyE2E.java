package io.gatling.devoxx.netty;

import java.time.Duration;

public class NettyE2E {
    public static void main(String[] args) throws Exception {

        var hostname = "localhost";
        var port = 8080;
        var nbConnections = 1000;
        var requestsPerConnection = 5000;

        try(var server = new NettyHttpServer()) {
            server.start(8080);

            try (var client = new NettyHttpClient(hostname, port, nbConnections, requestsPerConnection)) {
                client.run(Duration.ofMinutes(1));
            }
        }
    }
}
