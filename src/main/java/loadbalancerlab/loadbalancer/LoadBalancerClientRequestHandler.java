package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
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

public class LoadBalancerClientRequestHandler implements HttpRequestHandler {
    private List<Integer> incomingRequestTimestamps;
    private CacheRedistributor cacheRedis;
    public static HttpClientFactory clientFactory;
    private Logger logger;

    public static void configure( Config config ) {
        clientFactory = config.getHttpClientFactory();
    }

    public LoadBalancerClientRequestHandler( CacheRedistributor _cacheRedis) {
        incomingRequestTimestamps = Collections.synchronizedList(new LinkedList<>());
        cacheRedis = _cacheRedis;
        logger = new Logger("LoadBalancerClientRequestHandler");
    }

    @Override
    public void handle( HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) {
        CloseableHttpClient httpClient = clientFactory.buildApacheClient();
        String uri = httpRequest.getRequestLine().getUri();
        String[] uriArr = uri.split("/");
        String resourceName = uriArr[uriArr.length - 1];
        int cacheServerPort = cacheRedis.selectPort(resourceName);
        logger.log(String.format("relaying message to cache server at port %d", cacheServerPort), Logger.LogType.REQUEST_PASSING);
        // record request incoming timestamp
        incomingRequestTimestamps.add((int)(System.currentTimeMillis() / 1000));
        HttpGet getReq = new HttpGet("http://127.0.0.1:" + cacheServerPort + "/" + resourceName);

        try {
            CloseableHttpResponse res = httpClient.execute(getReq);
            HttpEntity resBody = res.getEntity();
            httpResponse.setEntity(resBody);
            httpResponse.setStatusCode(200);
        } catch (IOException e) {
            // if cache server failed
            e.printStackTrace();
            JSONObject outputJsonObj = new JSONObject();
            outputJsonObj.put("error_message", "Cache server failed to respond");
            String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
            InputStream stream = new ByteArrayInputStream(htmlResponse.getBytes());
            BasicHttpEntity responseBody = new BasicHttpEntity();
            responseBody.setContent(stream);
            httpResponse.setStatusCode(500);
            httpResponse.setEntity((HttpEntity)responseBody);
        }
    }
}