package loadbalancerlab.cacheserver;
import loadbalancerlab.shared.Logger;

// class for periodically clearing out outdated telemetry

/**
 * Runnable implementation which wraps around RequestMonitor. Periodically calls clearOutData() method on RequestMonitor
 * to keep capacity factor records on CacheServer up to date.
 */
class RequestMonitorRunnable implements Runnable {
    /**
     * The RequestMonitor instance on the associated CacheServer instance. Tracks data about the load on the CacheServer.
     */
    RequestMonitor reqMonitor;

    /**
     * Used for logging
     */
    private Logger logger;

    public RequestMonitorRunnable( RequestMonitor _reqMonitor ) {
        reqMonitor = _reqMonitor;
        logger = new Logger("RequestMonitorRunnable");
    }

    /**
     * periodically clears out clearOutData() method on associated RequestMonitor to keep load data up to date.
     */
    @Override
    public void run() {
        logger.log("Started TelemetryCurator thread", Logger.LogType.THREAD_MANAGEMENT);

        while (true) {
            try {
                Thread.sleep(300);
                reqMonitor.clearOutData(System.currentTimeMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("CacheServer RequestMonitorRunnable thread interrupted");
                break;
            }
        }

        logger.log("Terminated TelemetryCurator thread", Logger.LogType.THREAD_MANAGEMENT);
    }
}