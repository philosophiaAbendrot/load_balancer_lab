package loadbalancerlab.loadbalancer;

import loadbalancerlab.shared.Config;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class HashRingTest {
    HashRing hashRing;
    static final int DEFAULT_ANGLES_PER_SERVER = 15;
    static final int MAX_ANGLES_PER_SERVER = 20;
    static final int MIN_ANGLES_PER_SERVER = 10;
    static final int RING_SIZE = 10_000;
    int serverId = 5;
    Config config;

    @BeforeEach()
    public void setup() {
        config = new Config();
        config.setMaxAnglesPerServer(MAX_ANGLES_PER_SERVER);
        config.setDefaultAnglesPerServer(DEFAULT_ANGLES_PER_SERVER);
        config.setMinAnglesPerServer(MIN_ANGLES_PER_SERVER);
        config.setRingSize(RING_SIZE);
        config.setHashFunction(new MurmurHashFunctionImpl());
        HashRing.configure(config);
        hashRing = new HashRing();
    }

    @Nested
    @DisplayName("Test addServer()")
    class TestAddServer {
        int baseServerId = 5;

        @BeforeEach
        public void setup() {
            hashRing.addServer(baseServerId);
        }

        @Test
        @DisplayName("should initialize a list of angles of the correct length for that server id")
        public void initializeAngleListCorrectLength() {
            assertEquals(DEFAULT_ANGLES_PER_SERVER, hashRing.anglesByServerId.get(baseServerId).size());
        }

        @Test
        @DisplayName("should add initialized list of angles to angles list")
        public void shouldAddNewAnglesToAnglesList() {
            assertEquals(DEFAULT_ANGLES_PER_SERVER, hashRing.angles.size());
        }

        @Nested
        @DisplayName("When adding a server which does not exist in hash ring")
        class ServerDoesNotExist {
            int newServerId = 8;

            @BeforeEach
            public void setup() {
                hashRing.addServer(newServerId);
            }

            @Test
            @DisplayName("should initialize a list of angles of the correct length for the new server id")
            public void initializeAngleListCorrectLength() {
                assertEquals(DEFAULT_ANGLES_PER_SERVER, hashRing.anglesByServerId.get(newServerId).size());
            }

            @Test
            @DisplayName("should add new angles to angles list")
            public void shouldAddNewAnglesToAnglesList() {
                assertEquals(DEFAULT_ANGLES_PER_SERVER * 2, hashRing.angles.size());
            }
        }

        @Nested
        @DisplayName("When adding a server which exists in hash ring")
        class ServerDoesExist {
            Set<HashRingAngle> newAngleSet;
            Set<HashRingAngle> oldAngleSet;

            @Test
            @DisplayName("Should throw illegal argument exception")
            public void shouldThrowIllegalArgumentException() {
                assertThrows(IllegalArgumentException.class, () -> {
                   hashRing.addServer(baseServerId);
                });
            }

            @Test
            @DisplayName("should not change the list of angles for that server id")
            public void shouldNotChangeListOfAngles() {
                oldAngleSet = new HashSet<>(hashRing.anglesByServerId.get(baseServerId));
                try {
                    hashRing.addServer(baseServerId);
                } catch (IllegalArgumentException e) { }

                newAngleSet = new HashSet<>((hashRing.anglesByServerId).get(baseServerId));

                assertEquals(oldAngleSet.size(), newAngleSet.size());

                for (HashRingAngle angle : newAngleSet) {
                    assertTrue(oldAngleSet.contains(angle));
                }
            }

            @Test
            @DisplayName("should not change total list of angles")
            public void shouldNotChangeTotalListOfAngles() {
                oldAngleSet = new HashSet<>(hashRing.angles.values());
                try {
                    hashRing.addServer(baseServerId);
                } catch (IllegalArgumentException e) { }

                newAngleSet = new HashSet<>(hashRing.angles.values());

                assertEquals(oldAngleSet.size(), newAngleSet.size());

                for (HashRingAngle angle : newAngleSet) {
                    assertTrue(oldAngleSet.contains(angle));
                }
            }
        }
    }

    @Nested
    @DisplayName("Test addAngle()")
    class TestAddAngle {
        int numAngles = 3;

        @BeforeEach()
        public void setup() {
            hashRing.addServer(serverId);
        }

        @Test
        @DisplayName("Should add angles to hash ring anglesByServerId")
        public void shouldAddAnglesToHashRingAnglesByServerId() {
            hashRing.addAngle(serverId, numAngles);
            assertEquals(numAngles + DEFAULT_ANGLES_PER_SERVER, hashRing.anglesByServerId.get(serverId).size());
        }

        @Test
        @DisplayName("Should add angles to hash ring angles list")
        public void shouldAddAnglesToHashRingAnglesList() {
            hashRing.addAngle(serverId, numAngles);
            assertEquals(numAngles + DEFAULT_ANGLES_PER_SERVER, hashRing.angles.size());
        }

        @Test
        @DisplayName("Angles in hash ring anglesByServerId should have server id set correctly")
        public void shouldUpdateSetAngleByServerIdWithCorrectId() {
            hashRing.addAngle(serverId, numAngles);

            for (List<HashRingAngle> angleList : hashRing.anglesByServerId.values()) {
                for (HashRingAngle angle : angleList) {
                    assertEquals(serverId, angle.getServerId());
                }
            }
        }

        @Test
        @DisplayName("Angles in hash ring angles list should have server id set correctly")
        public void shouldUpdateAnglesWithCorrectId() {
            hashRing.addAngle(serverId, numAngles);

            for (HashRingAngle angle : hashRing.angles.values()) {
                assertEquals(serverId, angle.getServerId());
            }
        }

        @Nested
        @DisplayName("When too many angles are added to the hash ring")
        class WhenTooManyAnglesAddedToHashRing {
            int numAngles = 15;

            @Test
            @DisplayName("Number of angles in hash ring should be set to max value")
            public void shouldSetAnglesToMaxValueInAnglesByServerId() {
                hashRing.addAngle(serverId, numAngles);
                assertEquals(MAX_ANGLES_PER_SERVER, hashRing.anglesByServerId.get(serverId).size());
            }

            @Test
            @DisplayName("Number of angles in angles list should be set to max value")
            public void shouldSetAnglesToMaxValueInAnglesList() {
                hashRing.addAngle(serverId, numAngles);
                assertEquals(MAX_ANGLES_PER_SERVER, hashRing.angles.size());
            }
        }

        @Nested
        @DisplayName("When inputs are made for a server that doesn't exist in the hash ring")
        class WhenInputForServerWhichDoesNotExist {
            int newServerId = 8;
            int newNumAngles = 8;

            @Test
            @DisplayName("Should add two lists of angles to hash ring")
            public void shouldAddTwoListsOfAnglesToHashRing() {
                assertThrows(IllegalArgumentException.class, () -> {
                   hashRing.addAngle(newServerId, newNumAngles);
                });
            }
        }
    }

    @Nested
    @DisplayName("Test removeAngle()")
    class TestRemoveAngle {
        int numAngles = 3;

        @BeforeEach
        public void setup() {
            hashRing.addServer(serverId);
        }

        @Test
        @DisplayName("number of angles for that server should be reduced for that server by the correct amount")
        public void anglesShouldBeReducedInAnglesByServerId() {
            hashRing.removeAngle(serverId, numAngles);
            assertEquals(DEFAULT_ANGLES_PER_SERVER - numAngles, hashRing.anglesByServerId.get(serverId).size());
        }

        @Test
        @DisplayName("number of angles for angles list should be reduced by the correct amount")
        public void anglesShouldBeReducedInAnglesList() {
            hashRing.removeAngle(serverId, numAngles);
            assertEquals(DEFAULT_ANGLES_PER_SERVER - numAngles, hashRing.angles.size());
        }

        @Nested
        @DisplayName("When removeAngle() is called with a numAngles value which is too high")
        class WhenNumAnglesTooHigh {
            int numAngles = 15;

            @Test
            @DisplayName("should set number of angles to the minimum value in anglesByServerId")
            public void shouldSetAnglesToMinimumValueInAnglesByServerId() {
                hashRing.removeAngle(serverId, numAngles);
                assertEquals(MIN_ANGLES_PER_SERVER, hashRing.anglesByServerId.get(serverId).size());
            }

            @Test
            @DisplayName("should set number of angles to minimum value in angles list")
            public void shouldSetAnglesToMinimumValueInAnglesList() {
                hashRing.removeAngle(serverId, numAngles);
                assertEquals(MIN_ANGLES_PER_SERVER, hashRing.angles.size());
            }
        }

        @Nested
        @DisplayName("When removeAngle() is called on a server which does not exist")
        class RemoveAngleCalledOnServerWhichDoesNotExist {
            @DisplayName("should throw an IllegalArgumentError")
            @Test
            public void shouldThrowIllegalArgumentError() {
                assertThrows(IllegalArgumentException.class, () -> {
                    hashRing.removeAngle(serverId + 1, numAngles);
                });
            }
        }
    }

    @Nested
    @DisplayName("Test removeServer()")
    class TestRemoveServer {

        @BeforeEach
        public void setup() {
            hashRing.addServer(serverId);
        }

        @Test
        @DisplayName("Server should be removed from anglesByServerId")
        public void shouldBeRemovedFromAnglesByServerId() {
            hashRing.removeServer(serverId);
            assertFalse(hashRing.anglesByServerId.containsKey(serverId));
        }

        @Nested
        @DisplayName("When server does not exist")
        class WhenServerDoesNotExist {
            @DisplayName("should throw an IllegalArgumentError")
            @Test
            public void shouldThrowIllegalArgumentError() {
                assertThrows(IllegalArgumentException.class, () -> {
                    hashRing.removeServer(serverId + 1);
                });
            }
        }
    }

    @Nested
    @DisplayName("Test findServerId()")
    class TestFindServerId {
        int serverId1 = 5;
        int serverId2 = 8;
        int serverId3 = 14;
        int serverId4 = 2;
        int serverId5 = 6;

        String resourceName = "Chooder_Bunny.jpg";
        HashFunction hashFunction = new MurmurHashFunctionImpl();

        @BeforeEach
        public void setup() {
            hashFunction = new MurmurHashFunctionImpl();
            hashRing.addServer(serverId1);
            hashRing.addServer(serverId2);
            hashRing.addServer(serverId3);
            hashRing.addServer(serverId4);
            hashRing.addServer(serverId5);
        }

        @Test
        @DisplayName("Should find server id with the next highest angle for the resource name")
        public void shouldFindServerWithNextHighestAngle() {
            int position = hashFunction.hash(resourceName) % RING_SIZE;
            System.out.println("position = " + position);
            List<HashRingAngle> angles = new ArrayList<>(hashRing.angles.values());

            Collections.sort(angles, ( HashRingAngle a, HashRingAngle b) -> a.getAngle() - b.getAngle());

            HashRingAngle expectedNextAngle = null;

            if (position > angles.get(angles.size() - 1).getAngle()) {
                expectedNextAngle = angles.get(0);
            } else {
                for (int i = 0; i < angles.size() - 1; i++) {
                    if (angles.get(i).getAngle() > position) {
                        expectedNextAngle = angles.get(i);
                        break;
                    }
                }
            }

            assertEquals(expectedNextAngle.getServerId(), hashRing.findServerId(resourceName));
        }

        @Nested
        @DisplayName("When the position of the resource name is higher than any angle")
        class WhenPositionOfResourceNameIsHigherThanAnyAngle {
            HashFunction mockHashFunction = Mockito.mock(HashFunction.class);
            int serverId1 = 5;
            int serverId2 = 8;
            int serverId3 = 14;
            int serverId4 = 2;
            int serverId5 = 6;
            int lowestAngle = Integer.MAX_VALUE;
            int lowestAngleServerId = -1;

            @BeforeEach
            public void setup() {
                config.setHashFunction(mockHashFunction);
                HashRing.configure(config);
                hashRing = new HashRing();
                hashRing.addServer(serverId1);
                hashRing.addServer(serverId2);
                hashRing.addServer(serverId3);
                hashRing.addServer(serverId4);
                hashRing.addServer(serverId5);

                boolean containsHighest = false;
                int serverWithHighest = -1;
                int highestAnglePos = 9_999;
                HashRingAngle highestAngle = null;

                for (HashRingAngle angle : hashRing.angles.values()) {
                    if (angle.getAngle() < lowestAngle) {
                        lowestAngle = angle.getAngle();
                        lowestAngleServerId = angle.getServerId();
                    }

                    if (angle.getAngle() == highestAnglePos) {
                        containsHighest = true;
                        serverWithHighest = angle.getServerId();
                        highestAngle = angle;
                        break;
                    }
                }

                if (containsHighest) {
                    hashRing.anglesByServerId.get(serverWithHighest).remove(highestAngle);
                    hashRing.angles.remove(highestAnglePos);
                }
            }

            @Test
            @DisplayName("It should return the lowest angle")
            public void shouldReturnLowestAngle() {
                when(mockHashFunction.hash(anyString())).thenReturn(9_999);
                assertEquals(lowestAngleServerId, hashRing.findServerId(resourceName));
            }
        }
    }
}