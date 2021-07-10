package loadbalancerlab.loadbalancer;

import org.apache.commons.codec.digest.MurmurHash3;

/**
 * Implementation of HashFunction interface which using the MurmurHash3 hash function implemented by Apache
 */
public class MurmurHashFunctionImpl implements HashFunction {
    /**
     * @param input     a resource name
     * @return          the integer that the resource name was hashed to
     */
    @Override
    public int hash( String input ) {
        return Math.abs(MurmurHash3.hash32x86(input.getBytes()));
    }
}