package loadbalancerlab.shared;

import loadbalancerlab.loadbalancer.HashRingAngle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AngleDataProcessorTest {
    @Nested
    @DisplayName("Test getNumAnglesByTime()")
    class TestGetNumAnglesByTime {
        SortedMap<Integer, Map<Integer, List<HashRingAngle>>> angleHistory;
        AngleDataProcessor angleProcessor;
        String[][] processedResult;
        int[] serverIds = { 5, 18, 16, 39 };
        int[] timestamps;

        int indexTime;
        int numAngles = 25;
        int maxAngle = 120;

        List<List<Integer>> numAnglesMat = new ArrayList<>();

        @BeforeEach
        public void setup() {
            angleProcessor = new AngleDataProcessor();
            numAnglesMat.add(Arrays.asList(5, 4, 9));
            numAnglesMat.add(Arrays.asList(8, 13, 2, 8));
            numAnglesMat.add(Arrays.asList(4, 9, 7, 6));

            indexTime = (int)(System.currentTimeMillis() / 1_000);
            timestamps = new int[] { indexTime - 15, indexTime - 10, indexTime - 5 };

            angleHistory = new TreeMap<>();
            List<HashRingAngle> anglePool = new ArrayList<>();

            // pool of angles from which angle positions are chosen
            List<Integer> possibleAngles = new ArrayList<>();

            for (int i = 0; i <= maxAngle; i++) {
                possibleAngles.add(i);
            }

            Random rand = new Random();

            for (int i = 0; i < numAngles; i++) {
                // fill up angle pool with angles with randomly generated angles
                int randIdx = rand.nextInt(possibleAngles.size());
                int selectedAngle = possibleAngles.get(randIdx);
                // remove selected angle from possible angles to prevent duplicates HashRingAngles with the same angle
                possibleAngles.remove(randIdx);
                anglePool.add(new HashRingAngle(i, selectedAngle));
            }

            angleHistory.put(timestamps[0], new HashMap<>());
            angleHistory.put(timestamps[1], new HashMap<>());
            angleHistory.put(timestamps[2], new HashMap<>());

            // fill up angle history matrix
            for (int timestampIdx = 0; timestampIdx < numAnglesMat.size(); timestampIdx++) {
                Map<Integer, List<HashRingAngle>> timestampEntry = angleHistory.get(timestamps[timestampIdx]);

                for (int serverIdIdx = 0; serverIdIdx < numAnglesMat.get(timestampIdx).size(); serverIdIdx++) {
                    numAngles = numAnglesMat.get(timestampIdx).get(serverIdIdx);
                    List<HashRingAngle> angleList = new ArrayList<>();

                    for (int i = 0; i < numAngles; i++) {
                        int randAngleIdx = rand.nextInt(anglePool.size());
                        angleList.add(anglePool.get(randAngleIdx));
                    }

                    int serverId = serverIds[serverIdIdx];

                    timestampEntry.put(serverId, angleList);
                }
            }

            processedResult = angleProcessor.getNumAnglesByTime(angleHistory);
        }

        @Test
        @DisplayName("result should have correct dimensions")
        public void resultShouldHaveCorrectDimensions() {
            assertEquals(4, processedResult.length);
            assertEquals(serverIds.length + 1, processedResult[0].length);
        }

        @Test
        @DisplayName("should have correct number of angles by server for each timestamp")
        public void shouldReturnNumberOfAnglesByServer() {
            for (int timestampIdx = 0; timestampIdx < timestamps.length; timestampIdx++) {
                for (int serverIdIdx = 0; serverIdIdx < numAnglesMat.get(timestampIdx).size(); serverIdIdx++) {
                    assertEquals(String.valueOf(numAnglesMat.get(timestampIdx).get(serverIdIdx)), processedResult[timestampIdx + 1][serverIdIdx + 1]);
                }
            }
        }

        @Test
        @DisplayName("Result should have correct number of entries")
        public void resultShouldHaveCorrectNumEntries() {
            assertEquals(3 + 1, processedResult.length);
        }

        @Test
        @DisplayName("Result should have correct number of columns")
        public void resultShouldHaveCorrectNumColumns() {
            assertEquals(serverIds.length + 1, processedResult[0].length);
        }

        @Test
        @DisplayName("header row should contain all ids of servers")
        public void headerRowShouldContainAllServerIds() {
            String[] headerRow = processedResult[0];

            for (int i = 0; i < serverIds.length; i++) {
                String serverId = String.valueOf(serverIds[i]);
                boolean contains = false;

                for (int j = 0; j < headerRow.length; j++) {
                    if (serverId.equals(headerRow[j])) {
                        contains = true;
                        break;
                    }
                }

                assertTrue(contains);
            }
        }

        @Test
        @DisplayName("header row should contain ids of all servers in ascending order")
        public void headerRowShouldContainIdsOfServers() {
            String[] headerRow = processedResult[0];

            for (int i = 0; i < headerRow.length - 1; i++)
                assertTrue(Integer.parseInt(headerRow[i + 1]) >= Integer.parseInt(headerRow[i]));
        }

        @Test
        @DisplayName("leftmost column should contain all timestamps")
        public void leftMostColumnShouldContainTimestamps() {
            for (int i = 0; i < timestamps.length; i++) {
                int timestamp = timestamps[i];
                boolean contains = false;

                for (int row = 0; row < timestamps.length; row++) {
                    if (timestamp == Integer.parseInt(processedResult[row][0])) {
                        contains = true;
                        break;
                    }
                }

                assertTrue(contains);
            }
        }

        @Test
        @DisplayName("leftmost column should contain timestamps in ascending order")
        public void leftMostColumnShouldContainTimestampsAscending() {
            for (int row = 1; row < processedResult.length - 1; row++) {
                assertTrue(Integer.parseInt(processedResult[row][0]) <= Integer.parseInt(processedResult[row + 1][0]));
            }
        }
    }
}
