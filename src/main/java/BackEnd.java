import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;

public class BackEnd implements Runnable {
    public enum Type {
        HOME_PAGE_SERVER,
        IMAGE_FILE_SERVER
    }

    private class CustomHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParams = extractParams(httpExchange);
            handleResponse(httpExchange, requestParams);
        }

        private void handleResponse(HttpExchange httpExchange, String requestParams) throws IOException {
            OutputStream outputStream = httpExchange.getResponseBody();
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html>").append("<body>")
                    .append("<h1>")
                    .append("Hello")
                    .append("</h1>")
                    .append("</body>")
                    .append("</html>");

            // encode html content
//            String htmlResponse = StringEscapeUtils.escapeHtml4(htmlBuilder.toString());
        }

        private String extractParams(HttpExchange httpExchange) {
            return httpExchange.getRequestURI()
                    .toString()
                    .split("\\?")[1]
                    .split("=")[1];
        }
    }

    public int port;
//    List<HttpRequestInterceptor> requestInterceptors = new LinkedList<HttpRequestInterceptor>();
//    List<HttpResponseInterceptor> responseInterceptors = new LinkedList<HttpResponseInterceptor>();
//    HttpProcessor httpProcessor;
//    HttpRequestHandler requestHandler;

    int[] selectablePorts = new int[100];

    public BackEnd() {
        this.port = port;
        Random rand = new Random();
//        httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);
//        requestHandler = new HttpRequestHandler() {
//            @Override
//            public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
//                BasicHttpEntity responseBody = new BasicHttpEntity();
//                InputStream bodyStream = IOUtils.toInputStream("hello world", StandardCharsets.UTF_8);
//                responseBody.setContent(bodyStream);
//                bodyStream.close();
//                httpResponse.setEntity(responseBody);
//
//                try {
//                    TimeUnit.MILLISECONDS.sleep(rand.nextInt(1000) + 200);
//                } catch (InterruptedException e) {
//                    System.out.println(e.getMessage());
//                    e.printStackTrace();
//                }
//            }
//        };
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
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
            server.createContext("/", new CustomHttpHandler());
    //        server.setExecutor(threadPoolExecutor);
            server.start();
            Logger.log("Server started on port 8001");
        } catch (IOException e) {
            e.printStackTrace();
        }

//            for (int i = 0; i < selectablePorts.length; i++) {
//                port = selectablePorts[i];
//                Logger.log(String.format("Backend | port = %d", port));
//                final HttpServer server;
//
//                try {
//                    server = ServerBootstrap.bootstrap()
//                            .setLocalAddress(hostAddress)
//                            .setListenerPort(port)
//                            .setHttpProcessor(httpProcessor)
//                            .registerHandler("/*", requestHandler)
//                            .setSocketConfig(config)
//                            .create();
//
//                    server.start();
//                    server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
//                    Runtime.getRuntime().addShutdownHook(new Thread() {
//                        @Override
//                        public void run() {
//                            server.shutdown(5, TimeUnit.SECONDS);
//                            }
//                    });
//                    break;
//                } catch (IOException e) {
////                    System.out.println(e.getMessage());
////                    e.printStackTrace();
//                }
//            }
//        } catch (InterruptedException e) {
//            System.out.println(e.getMessage());
//            e.printStackTrace();
//        } catch (UnknownHostException e) {
//            System.out.println(e.getMessage());
//            e.printStackTrace();
//        }
    }
}
