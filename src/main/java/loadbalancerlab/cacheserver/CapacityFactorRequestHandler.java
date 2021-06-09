package loadbalancerlab.cacheserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import loadbalancerlab.shared.Logger;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This is a support class for CacheServer.
 * It is an implementation of the HttpHandler interface for the /capacity-factor route for the server on the CacheServer class.
 * This logic serves requests from the CacheServerManager class for updates on the capacity factor of the CacheServer instance.
 */
public class CapacityFactorRequestHandler implements HttpHandler {
    /**
     * @param _reqMonitor the associated CacheServer instance's associated RequestMonitor instance
     */
    RequestMonitor reqMonitor;
    /**
     * Used for logging
     */
    Logger logger;

    public CapacityFactorRequestHandler( RequestMonitor _reqMonitor ) {
        reqMonitor = _reqMonitor;
        logger = new Logger("CapacityFactorRequestHandler");
    }

    /**
     * handles incoming request for update on the capacity factor of the associated CacheServer instance
     * @param httpExchange - an encapsulation of an Http request for the com.sun.net.httpserver package
     */
    @Override
    public void handle( HttpExchange httpExchange ) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();

        // receive capacity factor from the CacheServer instance's RequestMonitor instance.
        double capacityFactor = reqMonitor.getCapacityFactor(System.currentTimeMillis());
        logger.log(String.format("capacityFactor = %f", capacityFactor), Logger.LogType.REQUEST_PASSING);

        // generate JSON object for response
        JSONObject outputJsonObj = new JSONObject();
        outputJsonObj.put("capacity_factor", capacityFactor);
        String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
        logger.log("CapacityFactorRequestHandler processed request", Logger.LogType.REQUEST_PASSING);
        // send out response
        httpExchange.sendResponseHeaders(200, htmlResponse.length());
        // insert JSON object into response
        outputStream.write(htmlResponse.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}