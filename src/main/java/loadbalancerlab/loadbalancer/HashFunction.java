package loadbalancerlab.loadbalancer;

/**
 * Interface for classes which are used to hash a resource name into an integer for use by the HashRing class's
 * consistent hashing mechanism.
 */
public interface HashFunction {

    /**
     * Hashing method. Receives a string name and returns an integer.
     * @param input     A resource name.
     * @return          An integer which the resource name is hashed to.
     */
    int hash(String input);
}
