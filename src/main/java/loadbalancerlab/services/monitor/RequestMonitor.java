package loadbalancerlab.services.monitor;

import loadbalancerlab.util.Logger;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

// monitors the number of incoming requests, compiles data and delivers reports
public class RequestMonitor {
    List<RequestDatum> requestData;
    String parentClass;
    int parametricStorageTime;
    final static int DEFAULT_PARAMETRIC_STORAGE_TIME = 10_000;

    // class for storing information on requests
    private class RequestDatum {
        long startTime;
        long endTime;
        long processingTime;

        public RequestDatum(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.processingTime = endTime - startTime;
        }

        public String toString() {
            return String.format("startTime = %d | endTime = %d | processingTime = %d", startTime, endTime, processingTime);
        }
    }

    public RequestMonitor(String parentClass) {
        this.parametricStorageTime = DEFAULT_PARAMETRIC_STORAGE_TIME;
        this.parentClass = parentClass;
        this.requestData = Collections.synchronizedList(new ArrayList<>());
    }

    public RequestMonitor(String parentClass, int parametricStorageTime) {
        this.parametricStorageTime = parametricStorageTime;
        this.parentClass = parentClass;
        this.requestData = Collections.synchronizedList(new ArrayList<>());
    }

    public void addRecord(long startTime, long endTime) {
        this.requestData.add(new RequestDatum(startTime, endTime));
    }

    public void clearOutData(long currentTime) {
        Logger.log("RequestMonitor - " + this.parentClass + " | clearOutTelemetry running", Logger.LogType.TELEMETRY_UPDATE);
        // delete request data which are out of date
        Iterator<RequestDatum> iterator = this.requestData.iterator();
        int deleteCount = 0;

        while (iterator.hasNext()) {
            RequestDatum datum = iterator.next();
            if (datum.startTime + this.parametricStorageTime < currentTime) {
                iterator.remove();
                deleteCount++;
            } else {
                break;
            }
        }

        Logger.log("RequestMonitor - " + this.parentClass + " | " + deleteCount + " data deleted.", Logger.LogType.TELEMETRY_UPDATE);
    }

    public double getCapacityFactor(long endTime) {
        if (!this.requestData.isEmpty()) {
            long startTime = this.requestData.get(0).startTime;
            long runningTime = 0;

            for (RequestDatum datum : this.requestData)
                runningTime += datum.processingTime;

            double capacityFactor = runningTime / (double)(endTime - startTime);
            Logger.log(String.format("CacheServer | capacityFactor = %f", capacityFactor), Logger.LogType.REQUEST_PASSING);

            return capacityFactor;
        } else {
            return 0.0;
        }
    }
}
