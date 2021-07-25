package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.RequestDecoder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Used to keep track of telemetry for CacheServer instances.
 * Pings associated CacheServer objects.
 */
public class TelemetryServer implements Runnable {

    /**
     * The interval between pings to CacheServer in seconds.
     */
    static int intervalBetweenPings;

    /**
     * The amount of time between iterations of the run() method, in milliseconds;
     */
    static int sleepInterval = 500;

    static {
        intervalBetweenPings = 1;
    }

    /**
     * Associated ServerMonitor object which monitors, records, and processes data on
     * CacheServer instances.
     */
    ServerMonitor serverMonitor;

    /**
     * The id of the CacheServer this object is tasked with tracking.
     */
    int cacheServerId;

    /**
     * The port that the CacheServer is running on.
     */
    int cacheServerPort;

    /**
     * A factory class used to create CloseableHttpClients for sending Http requests.
     */
    HttpClientFactory clientFactory;

    /**
     * Used for extracting JSONObject from a CloseableHttpResponse object.
     */
    RequestDecoder reqDecoder;

    /**
     * The time at which the CacheServer was last pinged. (Seconds since 1-Jan-1970)
     */
    int lastPinged;

    /**
     * Constructor
     * @param serverMonitor     Associated ServerMonitor object which monitors, records, and processes data on
     *                          CacheServer instances.
     * @param cacheServerId     The id of the CacheServer this object is tasked with tracking.
     * @param clientFactory     A factory class used to create CloseableHttpClients for sending Http requests.
     * @param cacheServerPort   The port which the CacheServer is running on.
     */
    public TelemetryServer( ServerMonitor serverMonitor, int cacheServerId, HttpClientFactory clientFactory,
                            int cacheServerPort, RequestDecoder reqDecoder ) {
        this.serverMonitor = serverMonitor;
        this.cacheServerId = cacheServerId;
        this.clientFactory = clientFactory;
        this.cacheServerPort = cacheServerPort;
        this.reqDecoder = reqDecoder;

        lastPinged = (int)(System.currentTimeMillis() / 1_000);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(sleepInterval);
                int currentTime = (int)(System.currentTimeMillis() / 1_000);

                if (currentTime - lastPinged >= intervalBetweenPings) {
                    HttpGet req = new HttpGet("http://127.0.0.1:" + cacheServerPort + "/capacity-factor");

                    try {
                        CloseableHttpClient httpClient = clientFactory.buildApacheClient();
                        CloseableHttpResponse res = httpClient.execute(req);
                        JSONObject resJson = reqDecoder.extractJsonApacheResponse(res);
                        double cf = resJson.getDouble("capacity_factor");
                        httpClient.close();
                    } catch (IOException e) {
                        System.out.println("IOException thrown in TelemetryServer#run().");
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {

            }
        }
    }
}
