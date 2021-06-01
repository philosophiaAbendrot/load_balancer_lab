package loadbalancerlab.loadbalancer;

import org.apache.commons.codec.digest.MurmurHash3;

public class MurmurHashFunctionImpl implements HashFunction {
    @Override
    public int hash( String input ) {
        return Math.abs(MurmurHash3.hash32x86(input.getBytes()));
    }
}