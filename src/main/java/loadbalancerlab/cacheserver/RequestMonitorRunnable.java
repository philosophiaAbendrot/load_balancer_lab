package loadbalancerlab.cacheserver;
import loadbalancerlab.shared.Logger;

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

    /**
     * @param reqMonitor         Associated RequestMonitor which keeps track of the capacity factor of this object and data on the incoming
     *                           requests.
     */
    public RequestMonitorRunnable( RequestMonitor reqMonitor ) {
        this.reqMonitor = reqMonitor;
        logger = new Logger("RequestMonitorRunnable");
    }

    /**
     * Periodically calls clearOutData() method on associated RequestMonitor to keep load data up to date.
     * Method from Runnable interface.
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