package io.gatling.devoxx.netty;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public class SslContexts {

    static final SslContext RESOLVED_FOR_SERVER;
    static final SslContext RESOLVED_FOR_CLIENT;

    static {
        SslProvider sslProvider;
        String tls = System.getProperty("tls");
        switch (System.getProperty("tls")) {
            case "boring" -> sslProvider = SslProvider.OPENSSL;
            case "jdk" -> sslProvider = SslProvider.JDK;
            case null -> sslProvider = null;
            default -> throw new IllegalArgumentException("Unknown value for System property tls: " + tls);
        }

        if (sslProvider == null) {
            RESOLVED_FOR_SERVER = null;
            RESOLVED_FOR_CLIENT = null;
        } else {
            try {
                var cert = new SelfSignedCertificate();
                RESOLVED_FOR_SERVER = SslContextBuilder
                        .forServer(cert.certificate(), cert.privateKey())
                        .sslProvider(SslProvider.OPENSSL)
                        .protocols("TLSv1.3", "TLSv1.2")
                        .build();
                RESOLVED_FOR_CLIENT = SslContextBuilder
                        .forClient()
                        .sslProvider(SslProvider.OPENSSL)
                        .protocols("TLSv1.3", "TLSv1.2")
                        .build();


            } catch (CertificateException | SSLException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }
}
