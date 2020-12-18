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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BackEndInitiator implements Runnable {
    int port;
    List<HttpRequestInterceptor> requestInterceptors = new ArrayList<HttpRequestInterceptor>();
    List<HttpResponseInterceptor> responseInterceptors = new ArrayList<HttpResponseInterceptor>();
    HttpProcessor httpProcessor;
    int[] selectablePorts = new int[100];

    public BackEndInitiator() {
        port = 3000;
        abstractConstructor();
    }

    public BackEndInitiator(int port) {
        port = 2250;
        abstractConstructor();
    }

    private void abstractConstructor() {
        for (int i = 0; i < selectablePorts.length; i++) {
            selectablePorts[i] = 37100 + i;
        }

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
                .registerHandler("/backend/start", new InitiateRequestHandler())
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

    private class InitiateRequestHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
            System.out.println("BackendInitiator | received backend initiate request");
            BackEnd backend = new BackEnd();
            Thread backendThread = new Thread(backend);
            System.out.println("BackEndInitiator | started backend thread");
            backendThread.start();

            // wait for backend port to be selected by backend
            int tryNumber = 1;
            while (backend.port == 0) {
                // check periodically for the backend port
                try {
                    Thread.sleep(20);
                } catch(InterruptedException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                Logger.log("BackEndInitiator | backendPort = " + backend.port + " tryNumber = " + tryNumber++);
            }

            Logger.log("chosen backend port = " + backend.port);
            BasicHttpEntity responseEntity = new BasicHttpEntity();
            InputStream responseStream = IOUtils.toInputStream(String.valueOf(backend.port), StandardCharsets.UTF_8.name());
            responseEntity.setContent(responseStream);
            responseStream.close();
            httpResponse.setEntity(responseEntity);
        }
    }
}
