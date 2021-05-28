package loadbalancerlab.monitortest;

import static org.junit.jupiter.api.Assertions.assertEquals;

//public class ServerMonitorTest {
//    private ServerMonitor serverMonitor;
//    int referenceTime;
//
//    @BeforeEach
//    public void setupTests() {
//        this.serverMonitor = new ServerMonitor();
//        this.referenceTime = (int)(System.currentTimeMillis() / 1_000);
//    }
//
//    @Test
//    @DisplayName("Should return the correct number of active servers by second")
//    public void testActiveServerCount() {
//        this.serverMonitor.addRecord(referenceTime + 5, 14);
//        this.serverMonitor.addRecord(referenceTime + 6, 15);
//        this.serverMonitor.addRecord(referenceTime + 7, 13);
//
//        SortedMap<Integer, Integer> outputData = this.serverMonitor.deliverData();
//
//        assertEquals(3, outputData.size());
//        assertEquals(14, outputData.get(referenceTime + 5));
//        assertEquals(15, outputData.get(referenceTime + 6));
//        assertEquals(13, outputData.get(referenceTime + 7));
//    }
//
//    @Test
//    @DisplayName("If there are duplicate entries for a given second, the first one should be recorded")
//    public void testDuplicateEntries() {
//        this.serverMonitor.addRecord(referenceTime + 5, 10);
//        this.serverMonitor.addRecord(referenceTime + 5, 12);
//
//        SortedMap<Integer, Integer> outputData = this.serverMonitor.deliverData();
//        assertEquals(10, outputData.get(referenceTime + 5));
//    }
//}