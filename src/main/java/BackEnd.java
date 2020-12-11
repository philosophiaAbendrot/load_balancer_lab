import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.text.StringEscapeUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
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
            Logger.log("BackEnd | =========================================");
            Logger.log("BackEnd | CustomHttpHandler received request");
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
            String[] intermediate1 = httpExchange.getRequestURI().toString().split("\\?");

            if (intermediate1.length > 1)
                return intermediate1[1];
            else
                return "";
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

            try {
                InetAddress host = InetAddress.getByName("127.0.0.1");
                InetSocketAddress socketAddress = new InetSocketAddress(host, port);
                server = HttpServer.create(socketAddress, 0);
                server.createContext("/", customHttpHandler);
                server.setExecutor(threadPoolExecutor);
                Logger.log(String.format("BackEnd | Server started on %s", socketAddress.toString()));
                break;
            } catch(IOException e) {
                Logger.log(String.format("BackEnd | Failed to start server on port %d", port));
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
