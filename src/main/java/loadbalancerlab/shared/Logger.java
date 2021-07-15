package loadbalancerlab.shared;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Used for logging information to terminal.
 * Utilized by various classes.
 */
public class Logger {
    /**
     * A static variable which holds a set of log types to be printed by all Logger objects.
     * Configured through Logger.configure() static method.
     */
    private static Set<LogType> displayedLogTypes;

    /**
     * Integer variable used to control print settings.
     *
     * 0: Default setting (no overrides).
     * -1: Don't print any logs.
     * 1: Print all logs.
     */
    private static int printSetting;

    /**
     * The name of the class for which this Logger object is printing logs.
     */
    private String className;

    static {
        printSetting = -1;
        displayedLogTypes = new HashSet<>();
    }

    /**
     * Types of logs.
     * Specified when calling 'log()' method to indicate the type of log.
     */
    public enum LogType {
        THREAD_MANAGEMENT,      /* Starting and shutting down threads */
        LOAD_MODULATION,        /* Controlling amount of client request traffic */
        RECORDING_DATA,         /* Recording and processing simulation data */
        REQUEST_PASSING,        /* Handling of HttpRequests */
        CAPACITY_MODULATION,    /* Recording load on each CacheServer object and modulating the number of CacheServers */
        PRINT_DATA_TO_CSV,      /* Printing data to csv files */
        ALWAYS_PRINT,           /* A type of log which is always printed */
    }

    /**
     * Setter method for 'printAll' field. When set to true, all logs are printed regardless of type.
     * @param setting       Value used to set 'printSetting' field.
     */
    public static void setPrintSetting(int setting) {
        printSetting = setting;
    }

    /**
     * Static method used for configuring the 'displayedLogTypes' static field.
     * @param types     An array of LogType objects which indicates what type of logs Logger objects should print.
     */
    public static void configure(LogType[] types) {
        displayedLogTypes = new HashSet<>(Arrays.asList(types));
    }

    /**
     * @param className     The name of the class which this Logger object will be printing for.
     */
    public Logger(String className) {

        /* Initialize a logger instance */
        this.className = className;
    }

    /**
     * Logs message to terminal.
     * @param msg   The message to be logged to the terminal.
     * @param type  The type of the message to be logged.
     */
    public void log(String msg, LogType type) {
        if (printSetting == 1) {

            /* Print all logs if printSetting is 1 */
            System.out.printf("%s | %s | %s\n", className, msg, java.time.ZonedDateTime.now());
        } else if (printSetting == -1) {

            /* Don't print anything if printSetting is -1 */
        } else if (printSetting == 0) {

            /* Print if the log is of the type selected to be printed */
            if (displayedLogTypes.contains(type) || type == LogType.ALWAYS_PRINT)
                System.out.printf("%s | %s | %s\n", className, msg, java.time.ZonedDateTime.now());
        }
    }
}
