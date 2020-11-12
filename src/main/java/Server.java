import org.apache.http.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.*;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Server {
    public void start(int port) {
        try {
            HttpRequestHandler requestHandler = new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                    System.out.println("handling request");
                }
            };

            List<HttpRequestInterceptor> requestIntercepts = new ArrayList<HttpRequestInterceptor>();
            List<HttpResponseInterceptor> responseIntercepts = new ArrayList<HttpResponseInterceptor>();

            requestIntercepts.add(new HttpRequestInterceptor() {
                @Override
                public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
                    Header[] headers = httpRequest.getAllHeaders();

                    System.out.println("receiving http request " + httpRequest);
                    for (int i = 0; i < headers.length; i++) {
                        Header header = headers[i];
                        HeaderElement[] headerElements = header.getElements();

                        System.out.println("headers:");
                        for (int j = 0; j < headerElements.length; j++) {
                            HeaderElement headerElement = headerElements[j];
                            System.out.printf("%s : %s \n", headerElement.getName(), headerElement.getValue());
                        }
                    }
                }
            });

            responseIntercepts.add(new HttpResponseInterceptor() {
                @Override
                public void process(HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                    StatusLine status = httpResponse.getStatusLine();
                    System.out.printf("sending out request | protocol: %s | status: %d - %s", status.getProtocolVersion(),
                            status.getStatusCode(), status.getReasonPhrase());
                }
            });

            HttpProcessor httpProcessor = new ImmutableHttpProcessor(requestIntercepts, responseIntercepts);

            SocketConfig config = SocketConfig.custom()
                    .setSoTimeout(15000)
                    .setTcpNoDelay(true)
                    .build();

            InetAddress hostAddress = InetAddress.getLocalHost();
            System.out.println("hostaddress = " + hostAddress);

            final HttpServer server = ServerBootstrap.bootstrap()
                    .setLocalAddress(hostAddress)
                    .setListenerPort(port)
                    .setHttpProcessor(httpProcessor)
                    .setSocketConfig(config)
                    .registerHandler("/*", requestHandler)
                    .create();

            System.out.println("initialized server");

            server.start();
            System.out.println("started server on port " + port);
            server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    server.shutdown(5, TimeUnit.SECONDS);
                }
            });
        } catch (IOException e) {
            System.out.println("IO exception");
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Interupted exception");
            System.out.println(e.getMessage());
        }
    }

    public void printHeaders(HttpRequest req) {
        HeaderIterator iterator = req.headerIterator();

        while (iterator.hasNext()) {
            Header header = iterator.nextHeader();
            System.out.printf("%s: %s\n", header.getName(), header.getValue());
        }
    }

    public static void main(String[] args) {
        new Server().start(8080);
    }
}
