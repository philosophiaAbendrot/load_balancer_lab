package loadbalancer.monitortest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import loadbalancer.monitor.RequestMonitor;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RequestMonitorTest {
    private RequestMonitor reqMonitor;
    private List<List<Long>> reqData;

    @BeforeEach
    public void setupTest() {
        this.reqMonitor = new RequestMonitor("RequestMonitorTest");
        this.reqData = new ArrayList<>();
    }

    // test that request timestamps are recorded
    @Test
    @DisplayName("Test that request timestamps recorded in RequestMonitor are correct")
    public void checkRequestTimestampsRecorded() {
        setupDefaultTestCase();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {  }

        double activeTime = 0;
        double totalTime = (double)(System.currentTimeMillis() - this.reqData.get(0).get(0));

        for (List<Long> datum : this.reqData)
            activeTime += (datum.get(1) - datum.get(0));

        double expectedCapacityFactor = activeTime / totalTime;
        double actualCapacityFactor = this.reqMonitor.getCapacityFactor();
        assertTrue(percentageDifference(actualCapacityFactor, expectedCapacityFactor) < 10, "Expected and actual capacity factor is different");
    }

    // test that number of requests recorded is correct after the old data is cleared out
    @Test
    @DisplayName("Test that older request timestamps are cleared out in capacityFactor calculation")
    public void checkNumRequests() {
        setupDefaultTestCase();

        // wait for older request data to clear out
        try {
            Thread.sleep(11_000);
        } catch (InterruptedException e) { }

        long newIndexTime = System.currentTimeMillis();

        this.reqData.add(new ArrayList<>(Arrays.asList(newIndexTime + 300L, newIndexTime + 400L)));
        this.reqData.add(new ArrayList<>(Arrays.asList(newIndexTime + 500L, newIndexTime + 600L)));
        this.reqData.add(new ArrayList<>(Arrays.asList(newIndexTime + 650L, newIndexTime + 690L)));
        this.reqData.add(new ArrayList<>(Arrays.asList(newIndexTime + 780L, newIndexTime + 840L)));

        double totalTime = (double)(System.currentTimeMillis() - (newIndexTime + 300L));
        double activeTime = 0;

        for (int i = 4; i < 8; i++) {
            List<Long> datum = this.reqData.get(i);
            activeTime += (double)(datum.get(1) - datum.get(0));
        }

        double expectedCapacityFactor = activeTime / totalTime;
        double actualCapacityFactor = this.reqMonitor.getCapacityFactor();

        assertTrue(percentageDifference(expectedCapacityFactor, actualCapacityFactor) < 10, "Expected and actual capacity factor is different");
    }

    private void setupDefaultTestCase() {
        List<List<Long>> requestData = new ArrayList<>();
        long indexTime = System.currentTimeMillis();

        this.reqData.add(new ArrayList<>(Arrays.asList(indexTime, 200L + indexTime)));
        this.reqData.add(new ArrayList<>(Arrays.asList(300L + indexTime, 400L + indexTime)));
        this.reqData.add(new ArrayList<>(Arrays.asList(650L + indexTime, 750L + indexTime)));
        this.reqData.add(new ArrayList<>(Arrays.asList(800L + indexTime, 850L + indexTime)));

        // add data to request monitor
        for (List<Long> datum : this.reqData)
            this.reqMonitor.addRecord(datum.get(0), datum.get(1));
    }

    private double percentageDifference(double a, double b) {
        return Math.abs(a - b) / ((a + b) / 2.0) * 100;
    }
}