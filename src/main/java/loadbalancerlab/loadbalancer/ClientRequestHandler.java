package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.Logger;
import org.apache.commons.io.IOUtils;
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
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ClientRequestHandler implements HttpRequestHandler {
    private List<Integer> incomingRequestTimestamps;
    private CacheRedistributorImpl cacheRedis;
    private static HttpClientFactory clientFactory;

    public static void configure( Config config ) {
        clientFactory = config.getClientFactory();
    }

    public ClientRequestHandler(CacheRedistributorImpl _cacheRedis) {
        incomingRequestTimestamps = Collections.synchronizedList(new LinkedList<>());
        cacheRedis = _cacheRedis;
    }

    @Override
    public void handle( HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) {
        CloseableHttpClient httpClient = clientFactory.buildApacheClient();
        String uri = httpRequest.getRequestLine().getUri();
        String[] uriArr = uri.split("/", 0);
        String resourceName = uriArr[uriArr.length - 1];
        int cacheServerPort = cacheRedis.selectPort(resourceName);

        Logger.log(String.format("ClientRequestHandler | relaying message to cache server at port %d", cacheServerPort), Logger.LogType.REQUEST_PASSING);

        // record request incoming timestamp
        incomingRequestTimestamps.add((int)(System.currentTimeMillis() / 1000));

        HttpGet getReq = new HttpGet("http://127.0.0.1:" + cacheServerPort);

        try {
            CloseableHttpResponse res = httpClient.execute(getReq);
            HttpEntity resBody = res.getEntity();

            InputStream content = resBody.getContent();
            String contentString = IOUtils.toString(content, StandardCharsets.UTF_8);

            httpResponse.setEntity(resBody);
            EntityUtils.consume(resBody);
            httpResponse.setStatusCode(200);
        } catch (IOException e) {
            // if cache server failed
            JSONObject outputJsonObj = new JSONObject();
            outputJsonObj.put("error_message", "Cache server failed to respond");
            String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
            InputStream stream = new ByteArrayInputStream(htmlResponse.getBytes());
            BasicHttpEntity responseBody = new BasicHttpEntity();
            responseBody.setContent(stream);
            httpResponse.setStatusCode(500);
            httpResponse.setEntity((HttpEntity)responseBody);
        }

//        try {
//            CloseableHttpResponse response = httpClient.execute();
//            Logger.log("ClientRequestHandler | sent message", Logger.LogType.REQUEST_PASSING);
//            HttpEntity responseBody = response.getEntity();
//            httpResponse.setEntity(responseBody);
//        } catch (IOException e) {
//            // if request to cache server failed
//            JSONObject outputJsonObj = new JSONObject();
//            outputJsonObj.put("error_message", "Cache server failed to respond");
//            String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
//            InputStream stream = new ByteArrayInputStream(htmlResponse.getBytes());
//            BasicHttpEntity responseBody = new BasicHttpEntity();
//            responseBody.setContent(stream);
//            httpResponse.setStatusCode(500);
//            httpResponse.setEntity((HttpEntity)responseBody);
//            System.out.println("ClientRequestHandler | IOException : Cache server failed to respond.");
//        }
    }
}