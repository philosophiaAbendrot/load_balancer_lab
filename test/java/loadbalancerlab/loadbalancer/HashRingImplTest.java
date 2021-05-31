package loadbalancerlab.loadbalancer;

import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.ConfigImpl;
import org.junit.jupiter.api.*;
import org.junit.platform.commons.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class HashRingImplTest {
    HashRingImpl hashRing;
    static final int DEFAULT_ANGLES_PER_SERVER = 15;
    static final int MAX_ANGLES_PER_SERVER = 20;
    static final int MIN_ANGLES_PER_SERVER = 10;
    static final int RING_SIZE = 10_000;

    @BeforeAll
    public static void config() {
        Config config = new ConfigImpl();
        config.setMaxAnglesPerServer(MAX_ANGLES_PER_SERVER);
        config.setDefaultAnglesPerServer(DEFAULT_ANGLES_PER_SERVER);
        config.setMinAnglesPerServer(MIN_ANGLES_PER_SERVER);
        config.setRingSize(RING_SIZE);
        HashRingImpl.configure(config);
    }

    @BeforeEach()
    public void setup() {
        hashRing = new HashRingImpl();
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
            assertEquals(DEFAULT_ANGLES_PER_SERVER, hashRing.anglesByServerId.get(baseServerId));
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
                assertEquals(DEFAULT_ANGLES_PER_SERVER, hashRing.anglesByServerId.get(newServerId));
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
                    hashRing.addServer(baseServerId)
                } catch (IllegalArgumentException e) { }

                newAngleSet = new HashSet<>((hashRing.anglesByServerId).get(baseServerId));

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
        int serverId = 5;
        int numAngles = 3;

        @BeforeEach()
        public void setup() {
            hashRing.addServer(serverId);
        }

        @Test
        @DisplayName("Should add angles to hash ring")
        public void shouldAddAnglesToHashRing() {
            hashRing.addAngle(serverId, numAngles);
            assertEquals(numAngles + DEFAULT_ANGLES_PER_SERVER, hashRing.anglesByServerId.get(serverId).size());
        }

        @Test
        @DisplayName("Angles in hash ring should have server id set correctly")
        public void shouldSetAngleServerId() {
            hashRing.addAngle(serverId, numAngles);

            for (List<HashRingAngle> angleList : hashRing.anglesByServerId.values()) {
                for (HashRingAngle angle : angleList) {
                    assertEquals(angle.getServerId(), serverId);
                }
            }
        }

        @Nested
        @DisplayName("When too many angles are added to the hash ring")
        class WhenTooManyAnglesAddedToHashRing {
            int numAngles = 15;

            @Test
            @DisplayName("Number of angles in hash ring should be set to max value")
            public void shouldSetAnglesToMaxValue() {
                hashRing.addAngle(serverId, numAngles);
                assertEquals(MAX_ANGLES_PER_SERVER, hashRing.anglesByServerId.get(serverId).size());
            }
        }

        @Nested
        @DisplayName("When inputs are made for a server that doesn't exist in the hash ring")
        class WhenInputForServerWhichDoesntExist {
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
}