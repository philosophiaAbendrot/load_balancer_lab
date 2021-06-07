package loadbalancerlab.cacheserver;

/**
 * A data-storage class used by RequestMonitor.
 * Keeps track of processing times of requests.
 */

public class RequestDatum {
    /**
     * Time at which associated request started processing, in milliseconds since 1-Jan-1970
     */
    long startTime;
    /**
     * Time at which associated request finished processing, in milliseconds since 1-Jan-1970
     */
    long endTime;
    /**
     * The processing time of the associated request, in milliseconds
     */
    long processingTime;

    public RequestDatum(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.processingTime = endTime - startTime;
    }

    /**
     * @return Returns string representation of instance
     */
    public String toString() {
        return String.format("startTime = %d | endTime = %d | processingTime = %d", startTime, endTime, processingTime);
    }
}