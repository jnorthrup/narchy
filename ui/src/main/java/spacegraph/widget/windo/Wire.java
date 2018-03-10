package spacegraph.widget.windo;

import jcog.Util;
import spacegraph.Surface;

/** undirected edge */
public class Wire {

    private final int hash;
    private volatile long lastActive = Long.MIN_VALUE;
    //final AtomicHistogram hits = new AtomicHistogram();

    final Surface a, b;

    public Wire(Surface a, Surface b) {
        assert(a!=b);
        if (a.id > b.id) {
            //ordering
            Surface x = b;
            b = a;
            a = x;
        }

        this.a = a;
        this.b = b;
        this.hash = Util.hashCombine(a, b);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        Wire w = ((Wire)obj);
        return w.hash == hash && (w.a.equals(a) && w.b.equals(b));
    }

    @Override
    public int hashCode() {
        return hash;
    }

    /** sends to target */
    public boolean in(Surface sender, Object s) {
        if (((Port)other(sender)).in(this, s)) {
            lastActive = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public Surface other(Surface x) {
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
    public float activity(long now, long window) {
        long l = lastActive;
        if (l == Long.MIN_VALUE)
            return 0;
        else {
            return 1f/(1f+(Math.abs(now - l))/((float)(window)));
        }
    }

    public boolean connect() {
        if (a instanceof Port && b instanceof Port) {
            synchronized (this) {
                return ((Port) a).connected((Port) b) && ((Port) b).connected((Port) a);
            }
        }

        return true;
    }
}
