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
            serverInfo = new ServerInfo(serverId, port);
        }

        @Nested
        @DisplayName("When server info Cf records are empty")
        class WhenRecordsEmpty {
            @Test
            @DisplayName("should return an average capacity factor of 0.0")
            public void shouldReturnZeroCapFactor() {
                assertEquals(0.0, serverInfo.getCurrentCapacityFactor());
            }
        }

        @Nested
        @DisplayName("When server info is not empty")
        class WhenCfRecordsNotEmpty {
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
                assertEquals(lastCfEntry, serverInfo.getCurrentCapacityFactor());
            }
        }
    }
}