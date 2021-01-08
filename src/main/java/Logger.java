import java.util.HashSet;
import java.util.Set;

public class Logger {
    private static Set<String> displayedLogTypes = new HashSet<>();

    public static void configure(String[] types) {
        for (int i = 0; i < types.length; i++)
            displayedLogTypes.add(types[i]);
    }
    public static void log(String msg, String type) {
        if (displayedLogTypes.contains(type))
            System.out.printf("%s | %s\n", msg, java.time.ZonedDateTime.now());
    }
}
