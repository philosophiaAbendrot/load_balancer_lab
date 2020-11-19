import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.ImmutableHttpProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BackEnd implements Runnable {
    public enum Type {
        HOME_PAGE_SERVER,
        IMAGE_FILE_SERVER
    }

    public int port;
    List<HttpRequestInterceptor> requestInterceptors = new LinkedList<HttpRequestInterceptor>();
    List<HttpResponseInterceptor> responseInterceptors = new LinkedList<HttpResponseInterceptor>();
    HttpProcessor httpProcessor;
    HttpRequestHandler requestHandler;
    int[] selectablePorts = new int[100];

    public BackEnd() {
        this.port = port;
        httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);
        requestHandler = new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                BasicHttpEntity responseBody = new BasicHttpEntity();
                InputStream bodyStream = IOUtils.toInputStream("hello world", StandardCharsets.UTF_8);
                responseBody.setContent(bodyStream);
                bodyStream.close();
                httpResponse.setEntity(responseBody);
            }
        };
        initializeSelectablePorts();
    }

    private void initializeSelectablePorts() {
        for (int i = 0; i < selectablePorts.length; i++) {
            selectablePorts[i] = 37100 + i;
        }
    }

    @Override
    public void run() {
        try {
            SocketConfig config = SocketConfig.custom()
                    .setSoTimeout(15000)
                    .setTcpNoDelay(true)
                    .build();

            InetAddress hostAddress = InetAddress.getByName("127.0.0.1");

            for (int i = 0; i < selectablePorts.length; i++) {
                port = selectablePorts[i];
                System.out.println("BackEnd | port = " + port);
                final HttpServer server;

                try {
                    server = ServerBootstrap.bootstrap()
                            .setLocalAddress(hostAddress)
                            .setListenerPort(port)
                            .setHttpProcessor(httpProcessor)
                            .registerHandler("/*", requestHandler)
                            .setSocketConfig(config)
                            .create();

                    server.start();
                    server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            server.shutdown(5, TimeUnit.SECONDS);
                            }
                    });
                    break;
                } catch (IOException e) {
                    System.out.printf("BackEnd | port %d is in use. Attempting another port.\n", port);
//                    System.out.println(e.getMessage());
//                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
