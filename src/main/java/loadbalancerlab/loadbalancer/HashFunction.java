package loadbalancerlab.loadbalancer;

/**
 * Interface for classes which are used to hash a resource name into an integer for use by HashRing class's
 * consistent hashing mechanism
 */
public interface HashFunction {
    /**
     * Hashing method. Receives a string name and returns an integer.
     * @param input     a resource name
     * @return          an integer which the resource name is hashed to
     */
    int hash(String input);
}
