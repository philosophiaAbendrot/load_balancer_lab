package loadbalancer.monitortest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import loadbalancer.monitor.ServerMonitor;

public class ServerMonitorTest {
    private ServerMonitor serverMonitor;

    private class DummyThread implements Runnable {
        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @Test
    @DisplayName("Should return the correct number of active servers by second")
    public void testActiveServerCount() {
        Map<Integer, Thread> portsToBackendThreads = new HashMap<>();
        SortedMap<Integer, Integer> serverCount = new TreeMap<>();
        this.serverMonitor = new ServerMonitor(portsToBackendThreads);
    }
}