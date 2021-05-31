package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheServerManager;
import loadbalancerlab.cacheservermanager.ServerMonitorImpl;
import loadbalancerlab.cacheservermanager.ServerMonitorRunnable;

public interface ServerMonitorFactory {
    ServerMonitorImpl produceServerMonitor( CacheServerManager cacheServerManager );
    ServerMonitorRunnable produceServerMonitorRunnable( ServerMonitorImpl serverMonitorImpl, CacheServerManager cacheServerManager );
}
