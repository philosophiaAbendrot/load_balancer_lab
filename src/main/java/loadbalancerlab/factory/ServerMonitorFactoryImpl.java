package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheServerManager;
import loadbalancerlab.cacheservermanager.ServerMonitor;
import loadbalancerlab.cacheservermanager.ServerMonitorRunnable;
import loadbalancerlab.shared.RequestDecoderImpl;

public class ServerMonitorFactoryImpl implements ServerMonitorFactory {
    @Override
    public ServerMonitor produceServerMonitor( CacheServerManager _cacheServerManager) {
        return new ServerMonitor(new HttpClientFactoryImpl(), new RequestDecoderImpl(), _cacheServerManager);
    }

    @Override
    public ServerMonitorRunnable produceServerMonitorRunnable( ServerMonitor serverMonitor, CacheServerManager _cacheServerManager ) {
        return new ServerMonitorRunnable(serverMonitor, _cacheServerManager);
    }
}