package loadbalancerlab.cacheserver;

import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

// monitors the number of incoming requests, compiles data and delivers reports
public class RequestMonitor {
    /**
     * A list of RequestDatum objects. Keeps track of the processing times of the most recent requests
     */
    List<RequestDatum> requestData;
    /**
     * How long RequestMonitor records are kept in memory, in milliseconds.
     */
    static int recordTTL = 10_000;

    /**
     * Used for logging
     */
    private Logger logger;

    public static void configure( Config config ) {
        recordTTL = config.getRequestMonitorRecordTTL();
    }

    public RequestMonitor() {
        requestData = Collections.synchronizedList(new ArrayList<>());
        logger = new Logger("RequestMonitor");
    }

    /**
     * Adds a request record. This method is called whenever a client request is handled by ClientRequestHandler.
     * @param startTime   when request processing began (milliseconds since Jan 1, 1970)
     * @param endTime     when request processing completed (milliseconds since Jan 1, 1970)
     */
    public void addRecord(long startTime, long endTime) {
        requestData.add(new RequestDatum(startTime, endTime));
    }

    /**
     * Clears out request records which are outdated. Records are considered outdated if they are older than 'recordStorageTime' by
     * the given timestamp 'currentTime'. This method is periodically called by RequestMonitorRunnable to keep capacity factor
     * records up to date.
     * @param currentTime: the time which is used to calculate whether the records are old enough to be deleted
     */
    public void clearOutData(long currentTime) {
        logger.log("clearOutTelemetry running", Logger.LogType.TELEMETRY_UPDATE);
        // delete request data which are out of date
        Iterator<RequestDatum> iterator = requestData.iterator();
        int deleteCount = 0;

        while (iterator.hasNext()) {
            RequestDatum datum = iterator.next();
            if (datum.startTime + recordTTL < currentTime) {
                iterator.remove();
                deleteCount++;
            } else {
                break;
            }
        }

        logger.log(deleteCount + " data deleted.", Logger.LogType.TELEMETRY_UPDATE);
    }

    /**
     * Returns a recent capacity factor value by processing recent request records, stored in 'requestData'
     * @param currentTime: a timestamp for the current time in milliseconds since 1-Jan-1970
     * @return capacityFactor: the 'load' on the CacheServer, in terms of running time / total time
     */
    public double getCapacityFactor(long currentTime) {
        if (requestData.isEmpty()) {
            // if records are empty, return 0.0
            return 0.0;
        } else {
            long startTime = requestData.get(0).startTime;
            long runningTime = 0;

            for (RequestDatum datum : requestData)
                runningTime += datum.processingTime;

            double capacityFactor = runningTime / (double)(currentTime - startTime);
            logger.log(String.format("CacheServer | capacityFactor = %f", capacityFactor), Logger.LogType.REQUEST_PASSING);

            return capacityFactor;
        }
    }
}
