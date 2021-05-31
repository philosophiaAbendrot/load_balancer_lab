package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheServerManager;
import loadbalancerlab.cacheservermanager.ServerMonitorImpl;
import loadbalancerlab.cacheservermanager.ServerMonitorRunnable;
import loadbalancerlab.shared.RequestDecoderImpl;

public class ServerMonitorFactoryImpl implements ServerMonitorFactory {
    @Override
    public ServerMonitorImpl produceServerMonitor( CacheServerManager _cacheServerManager) {
        return new ServerMonitorImpl(new HttpClientFactoryImpl(), new RequestDecoderImpl(), _cacheServerManager);
    }

    @Override
    public ServerMonitorRunnable produceServerMonitorRunnable( ServerMonitorImpl serverMonitorImpl, CacheServerManager _cacheServerManager ) {
        return new ServerMonitorRunnable(serverMonitorImpl, _cacheServerManager);
    }
}