package loadbalancerlab.cacheservermanager;

import java.util.SortedMap;

public interface ServerMonitor {
    void addServer(int id, int port);

    void updateServerCount(int currentSecond, int numServers);

    SortedMap<Integer, Integer> deliverData();
}
