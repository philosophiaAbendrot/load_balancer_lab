package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheServerManager;
import loadbalancerlab.cacheservermanager.ServerMonitor;
import loadbalancerlab.cacheservermanager.ServerMonitorRunnable;
import loadbalancerlab.shared.RequestDecoder;

/**
 * Factory class for producing ServerMonitor instances.
 */
public class ServerMonitorFactory {

    /**
     * Produces ServerMonitor instances.
     * @param cacheServerManager    Used to manage the lifecycle of CacheServer instances.
     * @param reqDecoder            Used to extract JSON objects from CloseableHttpResponse objects.
     * @param httpClientFactory     Factory for generating CloseableHttpClient objects for sending Http requests.
     * @return                      A ServerMonitor instance used to compile and process telemetry information for
     *                              CacheServer instances.
     */
    public ServerMonitor produceServerMonitor( CacheServerManager cacheServerManager, RequestDecoder reqDecoder,
                                               HttpClientFactory httpClientFactory ) {
        return new ServerMonitor(httpClientFactory, reqDecoder, cacheServerManager);
    }

    /**
     * @param serverMonitor         A ServerMonitor instance used to compile and process telemetry information for
     *                              CacheServer instances.
     * @param cacheServerManager    Used to manage the lifecycle of CacheServer instances.
     * @return                      A ServerMonitorRunnable instance which wraps around a ServerMonitor instance.
     */
    public ServerMonitorRunnable produceServerMonitorRunnable( ServerMonitor serverMonitor, CacheServerManager cacheServerManager ) {
        return new ServerMonitorRunnable(serverMonitor, cacheServerManager);
    }
}