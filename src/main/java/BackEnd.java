import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.text.StringEscapeUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class BackEnd implements Runnable {
    public enum Type {
        HOME_PAGE_SERVER,
        IMAGE_FILE_SERVER
    }

    // http handler that is fed into HttpServer upon initialization
    private class CustomHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParams = extractParams(httpExchange);
            handleResponse(httpExchange, requestParams);
        }

        private void handleResponse(HttpExchange httpExchange, String requestParams) throws IOException {
            Logger.log("=========================================");
            Logger.log("CustomHttpHandler received request");
            OutputStream outputStream = httpExchange.getResponseBody();
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html>").append("<body>")
                    .append("<h1>")
                    .append("Hello")
                    .append("</h1>")
                    .append("</body>")
                    .append("</html>");

            // encode html content
            String htmlResponse = StringEscapeUtils.escapeHtml4(htmlBuilder.toString());

            // send out response
            httpExchange.sendResponseHeaders(200, htmlResponse.length());
            outputStream.write(htmlResponse.getBytes());
            outputStream.flush();
            outputStream.close();
        }

        private String extractParams(HttpExchange httpExchange) {
            return httpExchange.getRequestURI()
                    .toString()
                    .split("\\?")[1]
                    .split("=")[1];
        }
    }

    public int port;
    int[] selectablePorts = new int[100];

    public BackEnd() {
        Random rand = new Random();
        // initialize list of ports 37000 - 37099 as selectable ports for backend server to run on
        initializeSelectablePorts();
    }

    private void initializeSelectablePorts() {
        for (int i = 0; i < selectablePorts.length; i++)
            selectablePorts[i] = 37100 + i;
    }

    @Override
    public void run() {
        // start server
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        HttpHandler customHttpHandler = new CustomHttpHandler();
        HttpServer server = null;

        for (int i = 0; i < selectablePorts.length; i++) {
            port = selectablePorts[i];
            Logger.log(String.format("attempting to start server on port %d\n", port));
            InetSocketAddress socketAddress = new InetSocketAddress("localhost", port);
            try {
                server = HttpServer.create(socketAddress, 0);
                server.createContext("/", customHttpHandler);
                server.setExecutor(threadPoolExecutor);
                break;
            } catch(IOException e) {
                Logger.log(String.format("BackEnd | Failed to start server on socket %s", socketAddress.toString()));
            }
        }

        if (server != null) {
            server.start();
            Logger.log("Server started on port " + port);
        } else {
            Logger.log("Failed to start server on any port");
        }
    }
}
