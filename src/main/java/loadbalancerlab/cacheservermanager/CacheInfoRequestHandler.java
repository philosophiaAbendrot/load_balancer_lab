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
 * A HttpRequestHandler implementation used to handle requests for info on the associated CacheServer's capacity factor
 * Requests are delegated to this class by the CacheServerManager class.
 */
public class CacheInfoRequestHandler implements HttpRequestHandler {
    private ServerMonitor serverMonitor;

    public CacheInfoRequestHandler( ServerMonitor _serverMonitor) {
        serverMonitor = _serverMonitor;
    }

    /**
     * method for HttpRequestHandler interface
     * @param httpRequest: representation of request received by server
     * @param httpResponse: representation of response to be made by server
     * @param httpContext: represents execution state of an HTTP process
     * @throws IOException: thrown in case of an IO error
     */
    @Override
    public void handle( HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext ) throws IOException {
        // get info on CacheServers from ServerMonitor
        Map<Integer, ServerInfo> serverInfo = serverMonitor.getServerInfo();

        // create output json object
        JSONObject outputJson = new JSONObject();

        // Add info on CacheServers to json object
        for (Map.Entry<Integer, ServerInfo> entry : serverInfo.entrySet()) {
            ServerInfo info = entry.getValue();
            JSONObject jsonElement = new JSONObject();
            jsonElement.put("port", info.getPort());
            jsonElement.put("capacityFactor", info.getCurrentCapacityFactor());
            outputJson.put(String.valueOf(info.getServerId()), jsonElement);
        }

        // insert the json object into the response
        String htmlResponse = StringEscapeUtils.escapeJson(outputJson.toString());
        httpResponse.setEntity(new StringEntity(htmlResponse));
    }
}