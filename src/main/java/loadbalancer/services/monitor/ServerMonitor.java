package loadbalancer.services.monitor;

import java.util.SortedMap;
import java.util.TreeMap;

public class ServerMonitor {
    public SortedMap<Integer, Integer> serverCount;

    public ServerMonitor() {
        this.serverCount = new TreeMap<>();
    }

    public void addRecord(int currentSecond, int numServers) {
        if (!this.serverCount.containsKey(currentSecond))
            this.serverCount.put(currentSecond, numServers);
    }

    public SortedMap<Integer, Integer> deliverData() {
        SortedMap<Integer, Integer> copyMap = new TreeMap<>();
        copyMap.putAll(this.serverCount);
        return copyMap;
    }
}