import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.*;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoadBalancer implements Runnable {
    int port;
    List<HttpRequestInterceptor> requestInterceptors = new ArrayList<HttpRequestInterceptor>();
    List<HttpResponseInterceptor> responseInterceptors = new ArrayList<HttpResponseInterceptor>();
    HttpRequestHandler requestHandler;
    HttpProcessor httpProcessor;

    public LoadBalancer(int port) {
        this.port = port;
        requestHandler = new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpGet httpget = new HttpGet("http://127.0.0.1:6666");
                CloseableHttpResponse response = httpClient.execute(httpget);
                HttpEntity responseBody = response.getEntity();
                httpResponse.setEntity(responseBody);
            }
        };

        httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);
    }

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

    public static void main(String[] args) {
        new LoadBalancer(8080).run();
    }
}
