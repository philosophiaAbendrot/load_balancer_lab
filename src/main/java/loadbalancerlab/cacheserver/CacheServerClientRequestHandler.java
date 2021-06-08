package loadbalancerlab.cacheserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.Logger;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;

/**
 * HttpHandler implementation which serves requests to '/' path on CacheServer.
 * Serves requests which originate from the Client class.
 * Returns a dummy response indicating that the response was returned by the CacheServer.
 */
public class CacheServerClientRequestHandler implements HttpHandler {
    /**
     * RequestMonitor which monitors load on the associated CacheServer instance
     */
    RequestMonitor reqMonitor;
    static int processingTime;

    public static void configure( Config config ) {
        processingTime = config.getCacheServerProcessingTime();
    }

    public CacheServerClientRequestHandler( RequestMonitor _reqMonitor ) {
        reqMonitor = _reqMonitor;
    }

    /**
     * handles incoming request for update on the capacity factor of the associated CacheServer instance
     * @param httpExchange - an encapsulation of an Http request for the com.sun.net.httpserver package
     */
    @Override
    public void handle( HttpExchange httpExchange ) throws IOException {
        long startTime = System.currentTimeMillis();
        Logger.log("CacheServer | received request from load balancer", Logger.LogType.REQUEST_PASSING);
        // extract parameters from request uri
        String requestParams = extractParams(httpExchange);

        // simulate processing time
        try {
            Thread.sleep(processingTime);
        } catch (InterruptedException e) {
            System.out.println("within ClientRequestHandler.handle()");
            e.printStackTrace();
        }

        // generate response and send it back
        String responseString = generateResponse(requestParams);
        System.out.println("responseString = " + responseString);
        OutputStream outputStream = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(200, responseString.length());
        outputStream.write(responseString.getBytes());
        outputStream.flush();
        outputStream.close();
        Logger.log("CacheServer | sent request back to load balancer", Logger.LogType.REQUEST_PASSING);

        long endTime = System.currentTimeMillis();
        // record request
        reqMonitor.addRecord(startTime, endTime);
    }

    /**
     * Helper method which is called by 'handle' method.
     * @param requestParams: a parameter which holds the query string of the request URI
     * Generates a dummy response
     */
    private String generateResponse(String requestParams) {
        JSONObject jsonOutput = new JSONObject();
        System.out.println("requestParams = " + requestParams);
        jsonOutput.put("resourceName", requestParams);
        jsonOutput.put("resourceContents", "here it is");
        // encode html content
        String htmlResponse = StringEscapeUtils.escapeJson(jsonOutput.toString());
        return htmlResponse;
    }

    /**
     * Helper method which is called by 'handle' method.
     * Returns the query string of the uri of a request
     * @param httpExchange: httpExchange - an encapsulation of an Http request for the com.sun.net.httpserver package
     * @return query string of the request uri
     */
    private String extractParams(HttpExchange httpExchange) {
        String uri = httpExchange.getRequestURI().toString().substring(1);

        if (uri.length() > 1)
            return uri;
        else
            return "";
    }
}