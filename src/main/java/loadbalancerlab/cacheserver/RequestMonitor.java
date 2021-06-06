package loadbalancerlab.cacheserver;

import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

// monitors the number of incoming requests, compiles data and delivers reports
public class RequestMonitor {
    List<RequestDatum> requestData;
    String parentClass;
    int recordStorageTime;
    static int defaultRecordStorageTime = 10_000;

    public void configure( Config config ) {
        defaultRecordStorageTime = config.getRequestMonitorRecordTTL();
    }

    public RequestMonitor(String _parentClass) {
        recordStorageTime = defaultRecordStorageTime;
        parentClass = _parentClass;
        requestData = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Adds a request record
     * @param startTime   when request processing began (milliseconds since Jan 1, 1970)
     * @param endTime     when request processing completed (milliseconds since Jan 1, 1970)
     */
    public void addRecord(long startTime, long endTime) {
        requestData.add(new RequestDatum(startTime, endTime));
    }

    /**
     * Clears out request records which are outdated. Records are considered outdated if they are older than 'recordStorageTime' by
     * the given timestamp 'currentTime'.
     * @param currentTime: the time which is used to calculate whether the records are old enough to be deleted
     */
    public void clearOutData(long currentTime) {
        Logger.log("RequestMonitor - " + parentClass + " | clearOutTelemetry running", Logger.LogType.TELEMETRY_UPDATE);
        // delete request data which are out of date
        Iterator<RequestDatum> iterator = requestData.iterator();
        int deleteCount = 0;

        while (iterator.hasNext()) {
            RequestDatum datum = iterator.next();
            if (datum.startTime + recordStorageTime < currentTime) {
                iterator.remove();
                deleteCount++;
            } else {
                break;
            }
        }

        Logger.log("RequestMonitor - " + parentClass + " | " + deleteCount + " data deleted.", Logger.LogType.TELEMETRY_UPDATE);
    }

    /**
     * Returns a recent capacity factor value by processing recent request records, stored in 'requestData'
     * @param currentTime: a timestamp for the current time in milliseconds since 1-Jan-1970
     * @return
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
            Logger.log(String.format("CacheServer | capacityFactor = %f", capacityFactor), Logger.LogType.REQUEST_PASSING);

            return capacityFactor;
        }
    }
}
