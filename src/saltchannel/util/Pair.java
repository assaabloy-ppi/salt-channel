package saltchannel.util;

/**
 * Immutable 2-tuple implementation.
 * 
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