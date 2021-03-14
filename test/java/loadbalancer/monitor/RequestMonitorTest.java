package loadbalancer.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RequestMonitorTest {
    private RequestMonitor reqMonitor;

    @BeforeEach
    public void setupTest() {
        this.reqMonitor = new RequestMonitor("RequestMonitorTest");
    }

    // test that request timestamps are recorded
    @Test
    @DisplayName("Test that request timestamps recorded in RequestMonitor are correct")
    public void checkRequestTimestampsRecorded() {
        List<List<Long>> requestData = new ArrayList<>();
        long indexTime = System.currentTimeMillis();

        requestData.add(new ArrayList<>(Arrays.asList(0L, 200L)));
        requestData.add(new ArrayList<>(Arrays.asList(300L, 400L)));
        requestData.add(new ArrayList<>(Arrays.asList(650L, 750L)));
        requestData.add(new ArrayList<>(Arrays.asList(800L, 850L)));

        List<List<Long>> requestDataOffset = new ArrayList<>();

        for (List<Long> datum : requestData) {
            List<Long> newDatum = new ArrayList<>();
            newDatum.add(datum.get(0) + indexTime);
            newDatum.add(datum.get(1) + indexTime);
            requestDataOffset.add(newDatum);
        }

        // add data to request monitor
        for (List<Long> datum : requestDataOffset)
            this.reqMonitor.addRecord(datum.get(0), datum.get(1));


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {  }

        double activeTime = 0;
        long totalTime = System.currentTimeMillis() - requestDataOffset.get(0).get(0);

        for (List<Long> datum : requestData)
            activeTime += (datum.get(1) - datum.get(0));

        double expectedCapacityFactor = activeTime / totalTime;
        double actualCapacityFactor = this.reqMonitor.getCapacityFactor();
        double percentDifference = Math.abs(actualCapacityFactor - expectedCapacityFactor) / ((actualCapacityFactor + expectedCapacityFactor) / 2);
        assertTrue(percentDifference < 0.1, "Expected and actual capacity factor is different");
    }

    // test that number of requests recorded is correct

    // test that capacity factor is correct

    // test that data is cleared out
}