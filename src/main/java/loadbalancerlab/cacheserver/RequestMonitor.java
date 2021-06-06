package loadbalancerlab.cacheserver;

import loadbalancerlab.shared.Logger;

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

    public RequestMonitor(String _parentClass) {
        parametricStorageTime = DEFAULT_PARAMETRIC_STORAGE_TIME;
        parentClass = _parentClass;
        requestData = Collections.synchronizedList(new ArrayList<>());
    }

    public RequestMonitor(String _parentClass, int _parametricStorageTime) {
        parametricStorageTime = _parametricStorageTime;
        parentClass = _parentClass;
        requestData = Collections.synchronizedList(new ArrayList<>());
    }

    public void addRecord(long startTime, long endTime) {
        requestData.add(new RequestDatum(startTime, endTime));
    }

    public void clearOutData(long currentTime) {
        Logger.log("RequestMonitor - " + parentClass + " | clearOutTelemetry running", Logger.LogType.TELEMETRY_UPDATE);
        // delete request data which are out of date
        Iterator<RequestDatum> iterator = requestData.iterator();
        int deleteCount = 0;

        while (iterator.hasNext()) {
            RequestDatum datum = iterator.next();
            if (datum.startTime + parametricStorageTime < currentTime) {
                iterator.remove();
                deleteCount++;
            } else {
                break;
            }
        }

        Logger.log("RequestMonitor - " + parentClass + " | " + deleteCount + " data deleted.", Logger.LogType.TELEMETRY_UPDATE);
    }

    public double getCapacityFactor(long endTime) {
        if (!requestData.isEmpty()) {
            long startTime = requestData.get(0).startTime;
            long runningTime = 0;

            for (RequestDatum datum : requestData)
                runningTime += datum.processingTime;

            double capacityFactor = runningTime / (double)(endTime - startTime);
            Logger.log(String.format("CacheServer | capacityFactor = %f", capacityFactor), Logger.LogType.REQUEST_PASSING);

            return capacityFactor;
        } else {
            return 0.0;
        }
    }
}
