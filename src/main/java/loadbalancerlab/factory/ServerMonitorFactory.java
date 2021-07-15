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
     * @return                      A ServerMonitor instance used to compile and process telemetry information for
     *                              CacheServer instances.
     */
    public ServerMonitor produceServerMonitor( CacheServerManager cacheServerManager ) {
        return new ServerMonitor(new HttpClientFactory(), new RequestDecoder(), cacheServerManager);
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