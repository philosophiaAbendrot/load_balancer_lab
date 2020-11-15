import org.apache.http.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.ImmutableHttpProcessor;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BackEndInitiator implements Runnable {
    int port;
    List<HttpRequestInterceptor> requestInterceptors = new ArrayList<HttpRequestInterceptor>();
    List<HttpResponseInterceptor> responseInterceptors = new ArrayList<HttpResponseInterceptor>();
    HttpRequestHandler requestHandler;
    HttpProcessor httpProcessor;

    public BackEndInitiator() {
        port = 3000;
        abstractConstructor();
    }

    public BackEndInitiator(int port) {
        port = 2250;
        abstractConstructor();
    }

    private void abstractConstructor() {
        requestHandler = new DefaultRequestHandler();
        httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);
    }

    @Override
    public void run() {
        SocketConfig config = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        try {
            InetAddress hostAddress = InetAddress.getByName("127.0.0.1");

            final HttpServer server = ServerBootstrap.bootstrap()
                .setLocalAddress(hostAddress)
                .setListenerPort(port)
                .setHttpProcessor(httpProcessor)
                .setSocketConfig(config)
                .registerHandler("/*", requestHandler)
                .create();

            server.start();
            server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    server.shutdown(5, TimeUnit.SECONDS);
                }
            });
        } catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private class DefaultRequestHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
            System.out.println("received request");
        }
    }
}
