package loadbalancerlab.cacheserver;

import loadbalancerlab.shared.Logger;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RequestMonitorTest {
    private RequestMonitor reqMonitor;
    private List<List<Long>> reqData;

    @BeforeAll
    public static void beforeAll() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.PRINT_NOTHING });
    }

    @BeforeEach
    public void setupTest() {
        reqMonitor = new RequestMonitor("RequestMonitorTest");
        reqData = new ArrayList<>();
    }

    // test that request timestamps are recorded
    @Nested
    @DisplayName("Test addRecord()")
    class TestAddRecord {
        @Test
        @DisplayName("Test that request timestamps recorded in RequestMonitor are correct")
        public void checkRequestTimestampsRecorded() {
            // adds entries to reqData and reqMonitor
            long indexTime = System.currentTimeMillis();
            setupDefaultTestCase(indexTime);
            long newIndexTime = indexTime + 1_000;

            double activeTime = 0;
            // reqData is in a format of a 2d array, where each element of the array is a collection
            // of [start_time, end_time] entries
            double totalTime = (double)(newIndexTime - reqData.get(0).get(0));

            for (List<Long> datum : reqData)
                activeTime += (datum.get(1) - datum.get(0));

            double expectedCapacityFactor = activeTime / totalTime;
            double actualCapacityFactor = reqMonitor.getCapacityFactor(newIndexTime);
            assertTrue(percentageDifference(actualCapacityFactor, expectedCapacityFactor) < 10, "Expected and actual capacity factor is different");
        }

    }

    @Nested
    @DisplayName("Test clearOutData()")
    class TestClearOutData {
        // test that number of requests recorded is correct after the old data is cleared out
        @Test
        @DisplayName("Test that older request timestamps are cleared out in capacityFactor calculation")
        public void checkNumRequests() {
            long indexTime = System.currentTimeMillis();
            // enter in old records
            setupDefaultTestCase(indexTime);

            long newIndexTime = System.currentTimeMillis() + 11_000;
            long endTime = System.currentTimeMillis() + 12_000;

            // enter in new records
            reqData.add(new ArrayList<>(Arrays.asList(newIndexTime + 300L, newIndexTime + 400L)));
            reqData.add(new ArrayList<>(Arrays.asList(newIndexTime + 500L, newIndexTime + 600L)));
            reqData.add(new ArrayList<>(Arrays.asList(newIndexTime + 650L, newIndexTime + 690L)));
            reqData.add(new ArrayList<>(Arrays.asList(newIndexTime + 780L, newIndexTime + 840L)));

            for (int i = 4; i < 8; i++)
                reqMonitor.addRecord(reqData.get(i).get(0), reqData.get(i).get(1));

            // clear out old records
            reqMonitor.clearOutData(newIndexTime);

            double totalTime = (double)(endTime - (newIndexTime + 300L));
            double activeTime = 0;

            for (int i = 4; i < 8; i++) {
                List<Long> datum = reqData.get(i);
                activeTime += (double)(datum.get(1) - datum.get(0));
            }

            double expectedCapacityFactor = activeTime / totalTime;
            double actualCapacityFactor = reqMonitor.getCapacityFactor(endTime);
            assertEquals(expectedCapacityFactor, actualCapacityFactor, "Expected and actual capacity factor is different");
        }
    }

    private void setupDefaultTestCase(long indexTime) {
        List<List<Long>> requestData = new ArrayList<>();

        reqData.add(new ArrayList<>(Arrays.asList(indexTime, 200L + indexTime)));
        reqData.add(new ArrayList<>(Arrays.asList(300L + indexTime, 400L + indexTime)));
        reqData.add(new ArrayList<>(Arrays.asList(650L + indexTime, 750L + indexTime)));
        reqData.add(new ArrayList<>(Arrays.asList(800L + indexTime, 850L + indexTime)));

        // add data to request monitor
        for (List<Long> datum : reqData)
            reqMonitor.addRecord(datum.get(0), datum.get(1));
    }

    private double percentageDifference(double a, double b) {
        return Math.abs(a - b) / ((a + b) / 2.0) * 100;
    }
}