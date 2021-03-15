package loadbalancer.monitor;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map;

public class ServerMonitor {
    public SortedMap<Integer, Integer> serverCount;

    public ServerMonitor() {
        this.serverCount = new TreeMap<>();
    }

    public void addRecord(int currentSecond, int numServers) {
        if (!this.serverCount.containsKey(currentSecond))
            this.serverCount.put(currentSecond, numServers);
    }

    public Set<Map.Entry<Integer, Integer>> deliverData() {
        return this.serverCount.entrySet();
    }
}