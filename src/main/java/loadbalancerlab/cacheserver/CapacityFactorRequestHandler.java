package loadbalancerlab.cacheserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import loadbalancerlab.shared.Logger;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Used by CacheServer class.
 * An implementation of the HttpHandler interface to handle updates on the capacity factor of the associated CacheServer
 * object.
 */
public class CapacityFactorRequestHandler implements HttpHandler {

    /**
     * reqMonitor       RequestMonitor class used for logging data on the capacity factor of the associated CacheServer
     *                  object and the number of requests it receives.
     */
    RequestMonitor reqMonitor;

    /**
     * Object used for logging
     */
    Logger logger;

    /**
     * @param reqMonitor        RequestMonitor class used for logging data on the capacity factor of the associated CacheServer
     *                          object and the number of requests it receives.
     */
    public CapacityFactorRequestHandler( RequestMonitor reqMonitor ) {
        this.reqMonitor = reqMonitor;
        logger = new Logger("CapacityFactorRequestHandler");
    }

    /**
     * handles incoming request for update on the capacity factor of the associated CacheServer instance
     * @param httpExchange - a representation of an Http request from the com.sun.net.httpserver package
     */
    @Override
    public void handle( HttpExchange httpExchange ) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();

        /* Receive capacity factor from the CacheServer instance's RequestMonitor instance */
        double capacityFactor = reqMonitor.getCapacityFactor(System.currentTimeMillis());
        logger.log(String.format("capacityFactor = %f", capacityFactor), Logger.LogType.REQUEST_PASSING);

        /* Generate JSON object for response */
        JSONObject outputJsonObj = new JSONObject();
        outputJsonObj.put("capacity_factor", capacityFactor);
        String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
        logger.log("CapacityFactorRequestHandler processed request", Logger.LogType.REQUEST_PASSING);

        /* Send out response */
        httpExchange.sendResponseHeaders(200, htmlResponse.length());

        /* Insert JSON object into response */
        outputStream.write(htmlResponse.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}