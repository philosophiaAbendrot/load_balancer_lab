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

public class BackEnd implements Runnable {
    int port;
    public BackEnd(int port) {
        this.port = port;
    }

    public void run() {
        try {
            start();
        } catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (HttpException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void start() throws IOException, HttpException, InterruptedException {
        HttpRequestHandler requestHandler = new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
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
                BasicHttpEntity responseBody = new BasicHttpEntity();
                InputStream bodyStream = IOUtils.toInputStream("hello world", StandardCharsets.UTF_8.name());
                responseBody.setContent(bodyStream);
                bodyStream.close();
                httpResponse.setEntity(responseBody);
                System.out.printf("sending out request | protocol: %s | status: %d - %s", status.getProtocolVersion(),
                        status.getStatusCode(), status.getReasonPhrase());
            }
        });

        HttpProcessor httpProcessor = new ImmutableHttpProcessor(requestIntercepts, responseIntercepts);

        SocketConfig config = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

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
    }

    public static void main(String[] args) {
        try {
            new BackEnd(6666).start();
        } catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (HttpException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
