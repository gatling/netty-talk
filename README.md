## Launching `NettyE2E`

`./mvnw exec:java`

## Tuning the JVM

env var, eg `export MAVEN_OPTS=-Xmx1024m`

## Available System properties

* `-Dio.netty.allocator.type=unpooled`: disable Netty's memory pooling
* `-Dio.netty.noPreferDirect=false`: force using HeapBuffers
* `-Dtls=boring|jdk`: if defined, switch to https with select provider, using a self-signed certificate
* `-Dtransport=nio|epoll|kqueue|iouring`: select the transport, nio if undefined
