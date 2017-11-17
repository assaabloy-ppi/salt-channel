package saltchannel.util;

/**
 * Immutable 2-tuple implementation.
 * This class and Triplet forms a very simple tuple library for BergDB. 
 * If we need more, we could switch to http://www.javatuples.org/.
 * 
 * @see Triplet
 * @author Frans Lundberg
 */
public class Pair<T0, T1> {
    private final T0 v0;
    private final T1 v1;
    
    public Pair(T0 v0, T1 v1) {
        this.v0 = v0;
        this.v1 = v1;
    }
    
    public final T0 getValue0() {
        return v0;
    }
    
    public final T1 getValue1() {
        return v1;
    }
}