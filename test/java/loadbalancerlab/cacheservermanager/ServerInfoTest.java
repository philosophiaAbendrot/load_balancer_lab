package loadbalancerlab.cacheservermanager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServerInfoTest {
    @Nested
    @DisplayName("Test 'updateCapacityFactor'")
    class TestUpdateCapacityFactor {
        ServerInfo serverInfo;
        int serverId = 5;
        int port = 3;

        @BeforeEach
        public void setup() {
            serverInfo = new ServerInfoImpl(serverId, port);
        }

        @Nested
        @DisplayName("When server info is not at capacity")
        class WhenCfRecordsNotFull {
            double[] capFactors;
            int currentTime;
            double lastCfEntry = 0.64;

            @BeforeEach
            public void setup() {
                currentTime = (int)System.currentTimeMillis() / 1_000;
                capFactors = new double[] { 0.25, 0.5, 0.54 };
                int startTime = currentTime - capFactors.length;

                for (int i = 0; i < capFactors.length; i++) {
                    serverInfo.updateCapacityFactor(startTime++, capFactors[i]);
                }

                serverInfo.updateCapacityFactor(currentTime, lastCfEntry);
            }

            @Test
            @DisplayName("newest record should be added")
            public void newestRecordShouldBeAdded() {
                assertTrue(serverInfo.getCapacityFactorRecord().values().contains(lastCfEntry));
                assertTrue(serverInfo.getCapacityFactorRecord().containsKey(currentTime));
            }

            @Test
            @DisplayName("size of cf record increases")
            public void sizeOfRecordRemainsSame() {
                assertEquals(capFactors.length + 1, serverInfo.getCapacityFactorRecord().size());
            }

            @Test
            @DisplayName("average should be updated")
            public void averageShouldBeUpdated() {
                double sum = 0;

                for (int i = 0; i < capFactors.length; i++) {
                    sum += capFactors[i];
                }

                sum += lastCfEntry;

                assertEquals(sum / (capFactors.length + 1), serverInfo.getAverageCapacityFactor());
            }
        }



        @Nested
        @DisplayName("When server info is currently holding the maximum allowed number of cf records and we add a new record")
        class WhenCfRecordsFull {
            int maxRecords = ServerInfoImpl.cfRecordSize;
            int currentTime;
            double[] capFactors;
            double lastCfEntry = 0.81;

            @BeforeEach
            public void setup() {
                currentTime = (int)System.currentTimeMillis() / 1_000;
                int startTime = currentTime - maxRecords;
                capFactors = new double[]{ 0.25, 0.5, 0.54, 0.65, 0.1, 0.44, 0.35, 0.54, 0.54, 0.8 };

                for (int i = 0; i < maxRecords; i++)
                    serverInfo.updateCapacityFactor(startTime++, capFactors[i]);

                serverInfo.updateCapacityFactor(currentTime, lastCfEntry);
            }

            @Test
            @DisplayName("it should remove the oldest existing record")
            public void shouldRemoveLowestExistingRecord() {
                assertFalse(serverInfo.getCapacityFactorRecord().values().contains(0.25));
                assertFalse(serverInfo.getCapacityFactorRecord().containsKey(currentTime - maxRecords));
            }

            @Test
            @DisplayName("newest record should be added")
            public void newestRecordShouldBeAdded() {
                assertTrue(serverInfo.getCapacityFactorRecord().values().contains(lastCfEntry));
                assertTrue(serverInfo.getCapacityFactorRecord().containsKey(currentTime));
            }

            @Test
            @DisplayName("size of cf record remains the same")
            public void sizeOfRecordRemainsSame() {
                assertEquals(maxRecords, serverInfo.getCapacityFactorRecord().size());
            }

            @Test
            @DisplayName("average should be updated")
            public void averageShouldBeUpdated() {
                double sum = 0;

                for (int i = 1; i < capFactors.length; i++) {
                    sum += capFactors[i];
                }

                sum += lastCfEntry;

                assertEquals(sum / maxRecords, serverInfo.getAverageCapacityFactor());
            }
        }
    }
}