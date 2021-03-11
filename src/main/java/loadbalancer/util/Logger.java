package loadbalancer.util;

import java.util.HashSet;
import java.util.Set;

public class Logger {
    // list of log types (not exhaustive)
    // threadManagment = starting and shutting down threads
    // loadModulation = recording load on each server and adjusting server capacity
    // recordingData = recording simulation data and synthesizing it

    private static Set<String> displayedLogTypes = new HashSet<>();

    public static void configure(String[] types) {
        for (int i = 0; i < types.length; i++)
            displayedLogTypes.add(types[i]);
    }
    public static void log(String msg, String type) {
        if (displayedLogTypes.contains(type) || type == "alwaysPrint")
            System.out.printf("%s | %s\n", msg, java.time.ZonedDateTime.now());
    }
}
