package loadbalancerlab.client;

/**
 * Used to induce thread sleeps, and thereby control the request output of Client instances.
 */
public interface DemandFunction {

    /**
     * Makes the thread sleep for a specified amount of time.
     * @throws InterruptedException     Thrown when thread is interrupted while the thread is sleeping.
     */
    void rest() throws InterruptedException;
}
