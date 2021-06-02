package loadbalancerlab.cacheservermanager;

import java.util.SortedMap;

public interface ServerInfo {
    SortedMap<Integer, Double> getCapacityFactorRecord();
    double getAverageCapacityFactor();
    int getServerId();
    int getPort();

    void setPort(int id);
    void setServerId(int id);
    void updateCapacityFactor(int timestamp, double cf);
}
