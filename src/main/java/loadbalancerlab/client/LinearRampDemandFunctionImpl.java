package loadbalancerlab.client;

import loadbalancerlab.shared.Logger;

/**
 * Sleeps the thread for a specified amount of time.
 * Implementation of DemandFunction interface.
 * A simple linear ramp demand function.
 * Demand increases linearly with time.
 */
public class LinearRampDemandFunctionImpl implements DemandFunction {

    /**
     * The maximum length of sleep interval which is the setting at the beginning of the period.
     */
    private int maxRestInterval;

    /**
     * The minimum length of sleep interval which is reached at the end of the period.
     */
    private int minRestInterval;

    /**
     * Object used for running time (milliseconds).
     */
    private int runningTime;

    /**
     * Time at which demand function was initialized.
     */
    private int startTime;

    /**
     * Object used for logging.
     */
    private Logger logger;

    /**
     * Constructor
     * @param maxRestInterval       The max sleep interval between requests (in milliseconds), which is the setting at
     *                              the beginning of the period.
     * @param minRestInterval       The min sleep interval between requests (in milliseconds), which is reached at the
     *                              end of the period.
     * @param runningTime           The total amount of time (in milliseconds) which the demand function runs for.
     */
    public LinearRampDemandFunctionImpl( int maxRestInterval, int minRestInterval, int runningTime, int startTime ) {
        this.maxRestInterval = maxRestInterval;
        this.minRestInterval = minRestInterval;
        this.runningTime = runningTime;
        this.startTime = startTime;
        logger = new Logger("LinearRampDemandFunctionImpl");
    }

    /**
     * Makes the thread sleep for a certain amount of time.
     * Method from DemandFunction interface.
     * @throws InterruptedException     Thrown when thread is interrupted while the thread is sleeping.
     */
    @Override
    public void rest() throws InterruptedException {
        int currentTime = (int)(System.currentTimeMillis() / 1_000);

        double minDemand = 1 / ((double) maxRestInterval);
        double maxDemand = 1 / ((double) minRestInterval);

        double demandSlope = (maxDemand - minDemand) / ((double)(runningTime / 1000));
        double deltaT = currentTime - startTime;

        /* If time is past the running time, return the maximum demand */
        double currentDemand = Math.min(minDemand + demandSlope * (deltaT), maxDemand);
        int sleepTime = (int)(1.0 / currentDemand);
        Thread.sleep(sleepTime);
    }
}
