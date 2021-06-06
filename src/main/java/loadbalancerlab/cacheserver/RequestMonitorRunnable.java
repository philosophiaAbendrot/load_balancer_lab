package loadbalancerlab.cacheserver;

import loadbalancerlab.shared.Logger;

// class for periodically clearing out outdated telemetry
class RequestMonitorRunnable implements Runnable {
    RequestMonitor reqMonitor;

    public RequestMonitorRunnable( RequestMonitor _reqMonitor ) {
        reqMonitor = _reqMonitor;
    }

    @Override
    public void run() {
        Logger.log("CacheServer | Started TelemetryCurator thread", Logger.LogType.THREAD_MANAGEMENT);

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

        Logger.log("CacheServer | Terminated TelemetryCurator thread", Logger.LogType.THREAD_MANAGEMENT);
    }
}