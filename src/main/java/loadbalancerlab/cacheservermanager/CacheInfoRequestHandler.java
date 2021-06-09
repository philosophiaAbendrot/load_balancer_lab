package loadbalancerlab.cacheservermanager;

import loadbalancerlab.shared.Logger;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class CacheInfoRequestHandler implements HttpRequestHandler {
    private ServerMonitor serverMonitor;

    public CacheInfoRequestHandler( ServerMonitor _serverMonitor) {
        serverMonitor = _serverMonitor;
    }

    @Override
    public void handle( HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext ) throws IOException {
        Map<Integer, ServerInfo> serverInfo = serverMonitor.getServerInfo();

        // create output json object
        JSONObject outputJson = new JSONObject();

        for (Map.Entry<Integer, ServerInfo> entry : serverInfo.entrySet()) {
            ServerInfo info = entry.getValue();
            JSONObject jsonElement = new JSONObject();
            jsonElement.put("port", info.getPort());
            jsonElement.put("capacityFactor", info.getAverageCapacityFactor());
            outputJson.put(String.valueOf(info.getServerId()), jsonElement);
        }

        // send out json in response
        String htmlResponse = StringEscapeUtils.escapeJson(outputJson.toString());
        httpResponse.setEntity(new StringEntity(htmlResponse));
    }
}