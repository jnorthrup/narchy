package spacegraph.space2d.widget.windo;

import jcog.Util;
import spacegraph.space2d.Surface;

import java.lang.reflect.Array;

/** undirected edge */
public class Wire {

    private final int hash;

    private volatile long aLastActive = Long.MIN_VALUE, bLastActive = Long.MIN_VALUE;
    private volatile int aTypeHash = 0, bTypeHash = 0;

    

    final Surface a, b;

    public Wire(Surface a, Surface b) {
        assert(a!=b);
        if (a.id > b.id) {
            
            Surface x = b;
            b = a;
            a = x;
        }

        this.a = a;
        this.b = b;
        this.hash = Util.hashCombine(a, b);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;

        Wire w = ((Wire)obj);
        return w.hash == hash && (w.a.equals(a) && w.b.equals(b));
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    /** sends to target */
    public final boolean in(Surface sender, Object s) {
        if (((Port)other(sender)).in(this, s)) {
            long now = System.currentTimeMillis();

            Class<?> cl = s.getClass();
            int th = cl.hashCode();
            if (cl.isArray()) {
                
                th = Util.hashCombine(th, Array.getLength(s));
            }

            if (sender == a) {
                this.aLastActive = now;
                this.aTypeHash = th;
            } else if (sender == b) {
                this.bLastActive = now;
                this.bTypeHash = th;
            } else
                throw new UnsupportedOperationException();

            return true;
        }
        return false;
    }

    private Surface other(Surface x) {
        if (x == a) {
            return b;
        } else if (x == b) {
            return a;
        } else {
            throw new RuntimeException();
        }
    }

    /** provides a value between 0 and 1 indicating amount of 'recent' activity.
     * this is entirely relative to itself and not other wires.
     * used for display purposes.
     * time is in milliesconds
     */
    public float activity(boolean aOrB, long now, long window) {
        long l = aOrB ? aLastActive : bLastActive;
        if (l == Long.MIN_VALUE)
            return 0;
        else {
            return 1f/(1f+(Math.abs(now - l))/((float)(window)));
        }
    }

    public final boolean connect() {
        if (a instanceof Port && b instanceof Port) {
            synchronized (this) {
                return ((Port) a).connected((Port) b) && ((Port) b).connected((Port) a);
            }
        }

        return true;
    }

    public int typeHash(boolean aOrB) {
        int x = aOrB ? aTypeHash : bTypeHash;
        if (x == 0 && (aOrB ? aLastActive : bLastActive)==Long.MIN_VALUE)
            return (aOrB ? bTypeHash : aTypeHash ); 
        else
            return x;
    }
}
