package loadbalancerlab.loadbalancer;

import loadbalancerlab.shared.Logger;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ClientRequestHandler implements HttpRequestHandler {
    private List<Integer> incomingRequestTimestamps;
    private CacheRedistributorImpl cacheRedisImpl;

    public ClientRequestHandler(CacheRedistributorImpl _cacheRedisImpl) {
        incomingRequestTimestamps = Collections.synchronizedList(new LinkedList<>());
        cacheRedisImpl = _cacheRedisImpl;
    }

    @Override
    public void handle( HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) {
        CloseableHttpClient httpClient = LoadBalancer.this.clientFactory.buildApacheClient();
        String uri = httpRequest.getRequestLine().getUri();
        String[] uriArr = uri.split("/", 0);
        int resourceId = Integer.parseInt(uriArr[uriArr.length - 1]);
        int cacheServerPort = cacheRedisImpl.selectPort("Chooder_Bunny");

        Logger.log(String.format("LoadBalancer | resourceId = %d", resourceId), Logger.LogType.REQUEST_PASSING);
        Logger.log(String.format("LoadBalancer | relaying message to cache server at port %d", cacheServerPort), Logger.LogType.REQUEST_PASSING);

        // record request incoming timestamp
        incomingRequestTimestamps.add((int)(System.currentTimeMillis() / 1000));
        HttpGet httpget = new HttpGet("http://127.0.0.1:" + cacheServerPort);

        try {
            CloseableHttpResponse response = httpClient.execute(httpget);
            HttpEntity responseBody = response.getEntity();
            httpResponse.setEntity(responseBody);
        } catch (IOException e) {
            // if request to cache server failed
            JSONObject outputJsonObj = new JSONObject();
            outputJsonObj.put("error_message", "Cache server failed to respond");
            String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
            InputStream stream = new ByteArrayInputStream(htmlResponse.getBytes());
            BasicHttpEntity responseBody = new BasicHttpEntity();
            responseBody.setContent(stream);
            httpResponse.setStatusCode(500);
            httpResponse.setEntity((HttpEntity)responseBody);
            System.out.println("LoadBalancer | IOException : Cache server failed to respond.");
        }
    }
}