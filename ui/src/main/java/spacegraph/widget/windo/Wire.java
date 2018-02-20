package spacegraph.widget.windo;

import spacegraph.Surface;

/** undirected edge */
public class Wire {

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
            return 1f/(1+Math.abs(now - l)/((float)window));
        }
    }

}
