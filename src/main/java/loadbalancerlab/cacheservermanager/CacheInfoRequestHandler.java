package loadbalancerlab.cacheservermanager;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

/**
 * A HttpRequestHandler implementation used to handle requests for info on the capacity factor of all CacheServer instances
 * Requests are delegated to this class by the CacheServerManager class.
 */
public class CacheInfoRequestHandler implements HttpRequestHandler {

    /**
     * ServerMonitor object which monitors, records, and processes data on CacheServer instances
     */
    private ServerMonitor serverMonitor;

    /**
     * @param serverMonitor     ServerMonitor object used to monitor, record and process data on CacheServer instances
     */
    public CacheInfoRequestHandler( ServerMonitor serverMonitor ) {
        this.serverMonitor = serverMonitor;
    }

    /**
     * Handles Http requests from LoadBalancerRunnable for updates for telemetry on CacheServer objects.
     * Method for HttpRequestHandler interface.
     * @param httpRequest   Representation of request received by server. From org.apache.http package.
     * @param httpResponse  Representation of response to be made by server. From org.apache.http package.
     * @param httpContext   Represents execution state of an HTTP process. From org.apache.http.HttpContext package.
     * @throws IOException  Thrown in case of an IO error.
     */
    @Override
    public void handle( HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext ) throws IOException {

        /* Get info on CacheServers from ServerMonitor */
        Map<Integer, ServerInfo> serverInfo = serverMonitor.getServerInfo();

        /* Create output json object */
        JSONObject outputJson = new JSONObject();

        /* Add info on CacheServers to json object */
        for (Map.Entry<Integer, ServerInfo> entry : serverInfo.entrySet()) {
            ServerInfo info = entry.getValue();
            JSONObject jsonElement = new JSONObject();
            jsonElement.put("port", info.getPort());
            jsonElement.put("capacityFactor", info.getCurrentCapacityFactor());
            outputJson.put(String.valueOf(info.getServerId()), jsonElement);
        }

        /* Insert the json object into the response */
        String htmlResponse = StringEscapeUtils.escapeJson(outputJson.toString());
        httpResponse.setEntity(new StringEntity(htmlResponse));
    }
}