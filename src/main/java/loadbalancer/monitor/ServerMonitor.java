package loadbalancer.monitor;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map;

import loadbalancer.util.Logger;

public class ServerMonitor implements Runnable {
    public SortedMap<Integer, Integer> serverCount;
    private Map<Integer, Thread> portsToBackendThreads;

    public ServerMonitor(Map<Integer, Thread> portsToBackendThreads) {
        this.serverCount = new TreeMap<>();
        this.portsToBackendThreads = portsToBackendThreads;
    }

    @Override
    public void run() {
        Logger.log("BackendInitiator | Starting ServerMonitor", "threadManagement");

        while (true) {
            try {
                Thread.sleep(100);
                int currentSecond = (int)(System.currentTimeMillis() / 1000);

                if (!this.serverCount.containsKey(currentSecond)) {
                    this.serverCount.put(currentSecond, portsToBackendThreads.size());
                }
            } catch (InterruptedException e) {
                Logger.log("BackEndInitiator | Shutting down ServerMonitor", "threadManagement");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public Set<Map.Entry<Integer, Integer>> getServerCount() {
        return this.serverCount.entrySet();
    }
}