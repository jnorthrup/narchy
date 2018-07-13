package jcog.signal.meter;

import java.util.concurrent.atomic.AtomicInteger;

/** concurrent FIFO circular (ring) buffer of a set of signals
 *  single producer, multi-consumer
 *  depending on the rate of data writing, live viewed data
 *  may change. a view lambda can report if the underlying
 *  data has changed before the function has returned,
 *  so the callee can invalidate that data.
 *
 *  another possibility is
 *  through attached event handlers which the append function can obey
 * */
public class TelemetryRing {
    /** ordering of column ID's */
    public final String[] col;

    /** cache */
    transient final int dim, history;

    /** use with caution */
    public final float[] data;

    private final AtomicInteger count = new AtomicInteger();

    public TelemetryRing(int history, String... columns) {
        this.col = columns;
        this.dim = col.length;
        this.history = history;
        this.data = new float[history * dim];
    }

    public void commit(float[] values) {
        assert(values.length == dim);

        int writeTo = count.updateAndGet((c) -> (c+1) % history);
        System.arraycopy(values, 0, data, writeTo * dim, dim );
    }

    /** samples the latest available data */
    public float[] sample() {
        return sample(0);
    }

    /** samples the latest available data */
    public float[] sample(float[] x) {
        return sample(0, x);
    }

    public float[] sample(int ago) {
        return sample(ago, new float[dim]);
    }

    public float[] sample(int ago, float[] x) {
        assert(ago >= 0);
        if (ago > history -1)
            throw new ArrayIndexOutOfBoundsException("too long ago");

        if (x.length!=dim)
            x = new float[dim];

        int now = count.get();
        int then = now - ago;
        while (then < 0)
            then += history;
        System.arraycopy(data, then * dim, x, 0, dim);

        /* TODO check that the writer hasn't written past where we just read, which
           could have crossed the data we were reading making it corrupt
           int now2 = count.get();
           to be extra paranoid check that it hasnt made a complete cycle and only appears
           to still have travel time
         */
        return x;
    }

}
