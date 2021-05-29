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

public class CacheInfoRequestHandler implements HttpRequestHandler {
    CacheServerManager cacheServerManager;
    public CacheInfoRequestHandler(CacheServerManager _cacheServerManager) {
        cacheServerManager = _cacheServerManager;
    }

    @Override
    public void handle( HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext ) throws IOException {
        Map<Integer, ServerInfo> serverInfo = cacheServerManager.getServerInfo();

        // create output json object
        JSONObject outputJson = new JSONObject();

        for (Map.Entry<Integer, ServerInfo> entry : serverInfo.entrySet()) {
            ServerInfo info = entry.getValue();
            JSONObject jsonElement = new JSONObject();
            jsonElement.put("port", info.port);
            jsonElement.put("capacityFactor", info.capacityFactor);
            outputJson.put(String.valueOf(info.id), jsonElement);
        }

        // send out json in response
        String htmlResponse = StringEscapeUtils.escapeJson(outputJson.toString());
        httpResponse.setEntity(new StringEntity(htmlResponse));
    }
}