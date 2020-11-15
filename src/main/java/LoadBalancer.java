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

    public LoadBalancer(int port) {
        this.port = port;
    }

    public void start() {
       run();
    }

    public void run() {
        HttpRequestHandler requestHandler = new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpGet httpget = new HttpGet("http://127.0.0.1:6666");
                CloseableHttpResponse response = httpClient.execute(httpget);
                HttpEntity responseBody = response.getEntity();
                httpResponse.setEntity(responseBody);
            }
        };

        List<HttpRequestInterceptor> requestIntercepts = new ArrayList<HttpRequestInterceptor>();
        List<HttpResponseInterceptor> responseIntercepts = new ArrayList<HttpResponseInterceptor>();

        requestIntercepts.add(new HttpRequestInterceptor() {
            @Override
            public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
                Header[] headers = httpRequest.getAllHeaders();
//                for (int i = 0; i < headers.length; i++) {
//                    Header header = headers[i];
//                    System.out.printf("%s : %s \n", header.getName(), header.getValue());
//                }
            }
        });

        responseIntercepts.add(new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                StatusLine status = httpResponse.getStatusLine();
                httpResponse.addHeader("Server", "philosophiaServer");
//                System.out.printf("sending out request | protocol: %s | status: %d - %s", status.getProtocolVersion(),
//                        status.getStatusCode(), status.getReasonPhrase());
            }
        });

        HttpProcessor httpProcessor = new ImmutableHttpProcessor(requestIntercepts, responseIntercepts);

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
