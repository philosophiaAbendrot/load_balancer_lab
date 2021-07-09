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

/**
 * HttpRequestHandler implementation for handling HTTP requests from Client received by the LoadBalancerRunnable class
 */
public class LoadBalancerClientRequestHandler implements HttpRequestHandler {
    /**
     * A synchronized list which records the timestamps of all incoming requests (seconds since 1-Jan-1970)
     */
    private List<Integer> incomingRequestTimestamps;

    /**
     * CacheRedistributor object which is used to handle logic for assigning requests to a particular CacheServer object.
     * This is done through a consistent hashing mechanism in conjunction with a HashRing object.
     */
    private CacheRedistributor cacheRedis;

    /**
     * Factory used for generating CloseableHttpClient instances to send HttpRequests
     */
    public static HttpClientFactory clientFactory;

    /**
     * Logger object used for logging
     */
    private Logger logger;

    /**
     * Configuration method used for configuring static fields
     * @param config: Config object used to configure various classes
     */
    public static void configure( Config config ) {
        clientFactory = config.getHttpClientFactory();
    }

    /**
     * @param cacheRedis    CacheRedistributor object used to manage consistent hashing mechanism to allocate client
     *                      requests to CacheServer instances
     */
    public LoadBalancerClientRequestHandler( CacheRedistributor cacheRedis ) {
        incomingRequestTimestamps = Collections.synchronizedList(new LinkedList<>());
        this.cacheRedis = cacheRedis;
        logger = new Logger("LoadBalancerClientRequestHandler");
    }

    /**
     * Method from HttpRequestHandler interface. Used to receive and generate response to Http requests from clients.
     * @param httpRequest       HttpRequest object which represents Http request from client
     * @param httpResponse      HttpResponse object which represents response which will be sent back to the client
     * @param httpContext       HttpContext object which represents execution state of an Http process
     */
    @Override
    public void handle( HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) {
        CloseableHttpClient httpClient = clientFactory.buildApacheClient();
        String uri = httpRequest.getRequestLine().getUri();
        String[] uriArr = uri.split("/");
        String resourceName = uriArr[uriArr.length - 1];

        /* select port to forward request to using consistent hashing mechanism */
        int cacheServerPort = cacheRedis.selectPort(resourceName);
        logger.log(String.format("relaying message to cache server at port %d", cacheServerPort), Logger.LogType.REQUEST_PASSING);

        /* record request incoming timestamp */
        incomingRequestTimestamps.add((int)(System.currentTimeMillis() / 1000));
        HttpGet getReq = new HttpGet("http://127.0.0.1:" + cacheServerPort + "/" + resourceName);

        try {

            /* Forward request to selected CacheServer instance */
            CloseableHttpResponse res = httpClient.execute(getReq);

            /* Send back response received from from CacheServer */
            HttpEntity resBody = res.getEntity();
            httpResponse.setEntity(resBody);
            httpResponse.setStatusCode(200);
        } catch (IOException e) {

            /* if cache server failed */
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