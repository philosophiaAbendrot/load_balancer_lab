package loadbalancerlab.cacheserver;

public class RequestDatum {
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