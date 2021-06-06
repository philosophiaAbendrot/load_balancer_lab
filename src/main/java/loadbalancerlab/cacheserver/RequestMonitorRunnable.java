package loadbalancerlab.cacheserver;

import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.Logger;

// class for periodically clearing out outdated telemetry
class RequestMonitorRunnable implements Runnable {
    static long requestMonitorTTL;
    RequestMonitor reqMonitor;

    public static void configure( Config config ) {
        requestMonitorTTL = config.getRequestMonitorTTL();
    }

    public RequestMonitorRunnable( RequestMonitor _reqMonitor ) {
        reqMonitor = _reqMonitor;
    }

    @Override
    public void run() {
        Logger.log("CacheServer | Started TelemetryCurator thread", Logger.LogType.THREAD_MANAGEMENT);
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() < startTime + requestMonitorTTL) {
            try {
                Thread.sleep(300);
                reqMonitor.clearOutData(System.currentTimeMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("CacheServer RequestMonitorRunnable thread interrupted");
            }
        }
        Logger.log("CacheServer | Terminated TelemetryCurator thread", Logger.LogType.THREAD_MANAGEMENT);
    }
}