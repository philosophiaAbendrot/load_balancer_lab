package loadbalancerlab.cacheservermanager;

import java.util.Map;
import java.util.SortedMap;

interface ServerMonitor {
    void addServer(int id, int port);

    void removeServer(int id);

    void updateServerCount(int currentSecond, int numServers);

    SortedMap<Integer, Integer> deliverData();

    Map<Integer, ServerInfo> getServerInfo();

    void pingCacheServers();
}
