package loadbalancerlab.cacheserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import loadbalancerlab.shared.Logger;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;

// http handler that is fed into CacheServer
// serves direct requests from CacheServerManager for updates on capacity factor
public class CapacityFactorRequestHandler implements HttpHandler {
    RequestMonitor reqMonitor;

    public CapacityFactorRequestHandler( RequestMonitor _reqMonitor ) {
        reqMonitor = _reqMonitor;
    }

    @Override
    public void handle( HttpExchange httpExchange ) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();

        double capacityFactor = reqMonitor.getCapacityFactor(System.currentTimeMillis());

        Logger.log(String.format("CacheServer | capacityFactor = %f", capacityFactor), Logger.LogType.REQUEST_PASSING);

        JSONObject outputJsonObj = new JSONObject();
        outputJsonObj.put("capacity_factor", capacityFactor);

        // encode html content
        String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
        Logger.log("CacheServer | CapacityFactorRequestHandler processed request", Logger.LogType.REQUEST_PASSING);
        // send out response
        httpExchange.sendResponseHeaders(200, htmlResponse.length());
        outputStream.write(htmlResponse.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}