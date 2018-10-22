package jcog.mutex;


/** contains 2 sub-treadmills
 * TODO parameterize the bit which it checks adjustable so these can be chained arbitrarily */
public final class TreadmillPair64 implements SpinMutex {

    private final SpinMutex a, b;
    private final int cHalf;

    public TreadmillPair64() {
        this(Runtime.getRuntime().availableProcessors());
    }

    private TreadmillPair64(int concurrency) {
        cHalf = Math.max(1, concurrency/2);
        a = new Treadmill64( cHalf );
        b = new Treadmill64( cHalf );
    }


    @Override
    public int start(long hash) {
        boolean aOrB = (hash & 1) == 0;
        SpinMutex x = aOrB ? a : b;
        int i = x.start(hash);
        if (!aOrB)
            i += cHalf;
        return i;
    }

    @Override
    public void end(int slot) {
        SpinMutex which;
        if (slot >= cHalf) {
            slot -= cHalf;
            which = b;
        } else {
            which = a;
        }
        which.end(slot);
    }
}
