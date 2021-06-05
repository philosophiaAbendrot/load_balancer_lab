package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheServerManager;
import loadbalancerlab.cacheservermanager.ServerMonitor;
import loadbalancerlab.cacheservermanager.ServerMonitorRunnable;
import loadbalancerlab.shared.RequestDecoder;

public class ServerMonitorFactory {
    public ServerMonitor produceServerMonitor( CacheServerManager _cacheServerManager) {
        return new ServerMonitor(new HttpClientFactory(), new RequestDecoder(), _cacheServerManager);
    }

    public ServerMonitorRunnable produceServerMonitorRunnable( ServerMonitor serverMonitor, CacheServerManager _cacheServerManager ) {
        return new ServerMonitorRunnable(serverMonitor, _cacheServerManager);
    }
}