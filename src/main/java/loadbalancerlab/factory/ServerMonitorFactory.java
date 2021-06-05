package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheServerManager;
import loadbalancerlab.cacheservermanager.ServerMonitor;
import loadbalancerlab.cacheservermanager.ServerMonitorRunnable;

public interface ServerMonitorFactory {
    ServerMonitor produceServerMonitor( CacheServerManager cacheServerManager );
    ServerMonitorRunnable produceServerMonitorRunnable( ServerMonitor serverMonitor, CacheServerManager cacheServerManager );
}
