package loadbalancerlab.cacheserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import loadbalancerlab.shared.Logger;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.io.OutputStream;

// http handler that is fed into CacheServer
// serves requests from load balancer which originate from the client
public class ClientRequestHandler implements HttpHandler {
    RequestMonitor reqMonitor;

    public ClientRequestHandler( RequestMonitor _reqMonitor ) {
        reqMonitor = _reqMonitor;
    }

    @Override
    public void handle( HttpExchange httpExchange ) throws IOException {
        String requestParams = extractParams(httpExchange);
        handleResponse(httpExchange, requestParams);
    }

    private void handleResponse(HttpExchange httpExchange, String requestParams) throws IOException {
        long startTime = System.currentTimeMillis();
        Logger.log("CacheServer | received request from load balancer", Logger.LogType.REQUEST_PASSING);
        try {
            Thread.sleep(200);
        } catch(InterruptedException e) {
            System.out.println("within CacheServer::CustomHandler.handleResponse");
            e.printStackTrace();
        }

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
        Logger.log("CacheServer | sent request back to load balancer", Logger.LogType.REQUEST_PASSING);
        outputStream.flush();
        outputStream.close();
        long endTime = System.currentTimeMillis();
        reqMonitor.addRecord(startTime, endTime);
    }

    private String extractParams(HttpExchange httpExchange) {
        String[] intermediate1 = httpExchange.getRequestURI().toString().split("\\?");

        if (intermediate1.length > 1)
            return intermediate1[1];
        else
            return "";
    }
}