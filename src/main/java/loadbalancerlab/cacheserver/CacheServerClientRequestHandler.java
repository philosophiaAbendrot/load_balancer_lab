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
 * HttpHandler implementation which serves client requests forwarded by load balancer.
 * Used by CacheServer class.
 * Returns a dummy response indicating that the response was returned by the CacheServer.
 * Delays the thread for a fixed amount of time to simulate processing time.
 */
public class CacheServerClientRequestHandler implements HttpHandler {

    /**
     * RequestMonitor which monitors load and incoming requests on the associated CacheServer instance.
     */
    RequestMonitor reqMonitor;

    /**
     * Controls how long a CacheServer instance (in milliseconds) waits before sending back a response to a request.
     * Used to simulate request processing time by the CacheServer.
     */
    static int processingTime;

    /**
     * Object used for logging.
     */
    private Logger logger;

    /**
     * Method used for configuring static variables on class.
     * @param config        Config object used for configuring various classes.
     */
    public static void configure( Config config ) {
        processingTime = config.getCacheServerProcessingTime();
    }

    /**
     * @param reqMonitor        RequestMonitor object used for monitoring the number of incoming requests and the
     *                          capacity factor of the associated CacheServer object.
     */
    public CacheServerClientRequestHandler( RequestMonitor reqMonitor ) {
        this.reqMonitor = reqMonitor;
        logger = new Logger("CacheServerClientRequestHandler");
    }

    /**
     * Handles incoming requests for updates on the capacity factor of the associated CacheServer instance.
     * @param httpExchange - an representation of an Http request from the com.sun.net.httpserver package.
     */
    @Override
    public void handle( HttpExchange httpExchange ) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.log("received request from load balancer", Logger.LogType.REQUEST_PASSING);

        /* Extract parameters from request uri */
        String requestParams = extractParams(httpExchange);

        /* Simulate processing time */
        try {
            Thread.sleep(processingTime);
        } catch (InterruptedException e) {
            System.out.println("within ClientRequestHandler.handle()");
            e.printStackTrace();
        }

        /* Generate response and send it back */
        String responseString = generateResponse(requestParams);
        OutputStream outputStream = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(200, responseString.length());
        outputStream.write(responseString.getBytes());
        outputStream.flush();
        outputStream.close();
        logger.log("sent request back to load balancer", Logger.LogType.REQUEST_PASSING);
        long endTime = System.currentTimeMillis();

        /* Record request */
        reqMonitor.addRecord(startTime, endTime);
    }

    /**
     * Helper method which is called by 'handle' method to generate a dummy response.
     * @param requestParams  The query string of the request URI.
     */
    private String generateResponse(String requestParams) {

        /* Dummy contents */
        JSONObject jsonOutput = new JSONObject();
        jsonOutput.put("resourceName", requestParams);
        jsonOutput.put("resourceContents", "here it is");

        /* Encode JSON content */
        String htmlResponse = StringEscapeUtils.escapeJson(jsonOutput.toString());
        return htmlResponse;
    }

    /**
     * Helper method which returns the query string of the uri of a request.
     * @param httpExchange: httpExchange - a representation of a Http request from the com.sun.net.httpserver package.
     * @return query string of the request uri.
     */
    private String extractParams(HttpExchange httpExchange) {
        String uri = httpExchange.getRequestURI().toString().substring(1);

        if (uri.length() > 1)
            return uri;
        else
            return "";
    }
}