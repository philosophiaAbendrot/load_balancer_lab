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
    SortedMap<Integer, Map<Integer, List<HashRingAngle>>> angleHistory;
    AngleDataProcessor angleProcessor;
    int[] serverIds;
    Map<Integer, Integer> serverIdTable;
    int[] timestamps;
    List<List<Integer>> numAnglesMat = new ArrayList<>();
    int indexTime;
    int numAngles = 25;
    int maxAnglePosition = 120;

    @BeforeEach
    public void setup() {
        serverIds = new int[] { 39, 18, 16, 5 };
        int[] serverIdsCopy = new int[serverIds.length];
        System.arraycopy(serverIds, 0, serverIdsCopy, 0, serverIds.length);
        Arrays.sort(serverIdsCopy);

        // table mapping server id to order when sorted in ascending order of ids
        serverIdTable = new HashMap<>();

        for (int i = 0; i < serverIds.length; i++)
            serverIdTable.put(serverIdsCopy[i], i);

        numAnglesMat.add(Arrays.asList(5, 4, 9));
        numAnglesMat.add(Arrays.asList(8, 13, 2, 8));
        numAnglesMat.add(Arrays.asList(4, 9, 7, 6));

        indexTime = (int)(System.currentTimeMillis() / 1_000);
        timestamps = new int[] { indexTime - 15, indexTime - 10, indexTime - 5 };
    }

    @Nested
    @DisplayName("Test getNumAnglesByTime()")
    class TestGetNumAnglesByTime {
        String[][] processedResult;

        @BeforeEach
        public void setup() {
            angleHistory = new TreeMap<>();
            List<HashRingAngle> anglePool = new ArrayList<>();

            // pool of angles from which angle positions are chosen
            List<Integer> possibleAngles = new ArrayList<>();

            for (int i = 0; i <= maxAnglePosition; i++) {
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

            angleProcessor = new AngleDataProcessor(angleHistory, maxAnglePosition + 1);
            processedResult = angleProcessor.getNumAnglesByTime();
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
                    int serverId = serverIds[serverIdIdx];
                    int serverIdOrder = serverIdTable.get(serverId);

                    assertEquals(String.valueOf(numAnglesMat.get(timestampIdx).get(serverIdIdx)), processedResult[timestampIdx + 1][serverIdOrder + 1]);
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

            for (int i = 1; i < headerRow.length - 1; i++)
                assertTrue(Integer.parseInt(headerRow[i + 1]) >= Integer.parseInt(headerRow[i]));
        }

        @Test
        @DisplayName("leftmost column should contain all timestamps")
        public void leftMostColumnShouldContainTimestamps() {
            for (int i = 0; i < timestamps.length; i++) {
                int timestamp = timestamps[i];
                boolean contains = false;

                for (int row = 1; row < processedResult.length; row++) {
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

    @Nested
    @DisplayName("Test getTotalSweepAngleByServerByTime()")
    class TestTotalSweepAngleByTime {
        String[][] processedResult;
        SortedMap<Integer, Map<Integer, List<Integer>>> sortedMapByServer = new TreeMap<>();
        Map<Integer, HashRingAngle> angleTable = new HashMap<>();
        int[] anglePositions;
        int numAnglesInitial = 10;
        Map<Integer, List<Integer>> angleAlloc1;
        Map<Integer, List<Integer>> angleAlloc2;
        Map<Integer, List<Integer>> angleAlloc3;
        Map<Integer, Map<Integer, List<Integer>>> angleAllocTable;

        @BeforeEach
        public void setup() {
            serverIds = new int[] { 599, 660, 201, 489, 113 };

            // angle positions of servers
            anglePositions = new int[] { 86, 91, 22, 54, 85, 10, 98, 36, 69, 8, 55, 35, 92 };

            // initialize angle table
            for (int i = 0; i < numAnglesInitial; i++)
                angleTable.put(i, new HashRingAngle(i, anglePositions[i]));

            // initial angle placement
            angleAlloc1 = new HashMap<>();
            angleAlloc2 = new HashMap<>();
            angleAlloc3 = new HashMap<>();

            // first timestamp snapshot
            angleAlloc1.put(serverIds[0], Arrays.asList(0, 2));
            angleAlloc1.put(serverIds[1], Arrays.asList(7, 4));
            angleAlloc1.put(serverIds[2], Arrays.asList(8));
            angleAlloc1.put(serverIds[3], Arrays.asList(1, 5, 6));
            angleAlloc1.put(serverIds[4], Arrays.asList(3));

            // second timestamp snapshot
            angleAlloc2.put(serverIds[0], Arrays.asList(0, 2, 9));
            angleAlloc2.put(serverIds[1], Arrays.asList(7, 4));
            angleAlloc2.put(serverIds[2], Arrays.asList(8, 11));
            angleAlloc2.put(serverIds[3], Arrays.asList(1, 5, 6));
            angleAlloc2.put(serverIds[4], Arrays.asList(3));

            // third timestamp snapshot
            angleAlloc3.put(serverIds[0], Arrays.asList(0, 2, 9));
            angleAlloc3.put(serverIds[1], Arrays.asList(7, 4, 10));
            angleAlloc3.put(serverIds[2], Arrays.asList(8, 11));
            angleAlloc3.put(serverIds[3], Arrays.asList(1, 5, 6));
            angleAlloc3.put(serverIds[4], Arrays.asList(3, 12));

            angleAllocTable.put(timestamps[0], angleAlloc1);
            angleAllocTable.put(timestamps[1], angleAlloc2);
            angleAllocTable.put(timestamps[2], angleAlloc3);

            // convert angle allocation into angle history table
            for (Map.Entry<Integer, Map<Integer, List<Integer>>> entry : angleAllocTable.entrySet()) {
                int timestamp = entry.getKey();
                Map<Integer, List<Integer>> allocSnapshot = entry.getValue();
                Map<Integer, List<HashRingAngle>> angleHistoryEntry = new HashMap<>();

                for (Map.Entry<Integer, List<Integer>> allocSnapshotEntry : allocSnapshot.entrySet()) {
                    int serverId = allocSnapshotEntry.getKey();
                    List<Integer> angleIds = allocSnapshotEntry.getValue();
                    List<HashRingAngle> angleList = new ArrayList<>();

                    for (Integer angleId : angleIds)
                        angleList.add(angleTable.get(angleId));

                    angleHistoryEntry.put(serverId, angleList);
                }

                angleHistory.put(timestamp, angleHistoryEntry);
            }

            angleProcessor = new AngleDataProcessor(angleHistory, maxAnglePosition + 1);
            processedResult = angleProcessor.getSweepAngleByTime();
        }

        @Test
        @DisplayName("header row should contain ids of all servers")
        public void headerRowShouldContainIdsOfAllServers() {
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
        public void headerRowShouldContainIdsOfAllServersInAscendingOrder() {
            String[] headerRow = processedResult[0];

            for (int i = 1; i < headerRow.length - 1; i++)
                assertTrue(Integer.parseInt(headerRow[i + 1]) >= Integer.parseInt(headerRow[i]));
        }

        @Test
        @DisplayName("Leftmost column should contain all timestamps")
        public void leftMostColumnShouldContainAllTimestamps() {
            for (int i = 0; i < timestamps.length; i++) {
                int timestamp = timestamps[i];
                boolean contains = false;

                for (int row = 1; row < processedResult.length; row++) {
                    if (timestamp == Integer.parseInt(processedResult[row][0])) {
                        contains = true;
                        break;
                    }
                }

                assertTrue(contains);
            }
        }

        @Test
        @DisplayName("Result should have correct dimensions")
        public void shouldHaveCorrectDimensions() {
            assertEquals(serverIds.length + 1, processedResult[0].length);
            assertEquals(timestamps.length + 1, processedResult.length);
        }

        @Test
        @DisplayName("Should return correct values for sweep angle")
        public void shouldReturnCorrectValuesForSweepAngle() {
            // row for first timestamp
            assertEquals("18", processedResult[1][1]);
            assertEquals("15", processedResult[1][2]);
            assertEquals("45", processedResult[1][3]);
            assertEquals("13", processedResult[1][4]);
            assertEquals("30", processedResult[1][5]);

            // row for second timestamp
            assertEquals("44", processedResult[2][1]);
            assertEquals("17", processedResult[2][2]);
            assertEquals("28", processedResult[2][3]);
            assertEquals("14", processedResult[2][4]);
            assertEquals("18", processedResult[2][5]);

            // row for third timestamp
            assertEquals("18", processedResult[3][1]);
            assertEquals("27", processedResult[3][2]);
            assertEquals("14", processedResult[3][3]);
            assertEquals("44", processedResult[3][4]);
            assertEquals("18", processedResult[3][5]);
        }
    }
}
