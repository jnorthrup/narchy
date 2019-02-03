package jcog.mutex;


import jcog.Util;

/** contains 2 sub-treadmills
 * TODO parameterize the bit which it checks adjustable so these can be chained arbitrarily */
public final class SpinMutexArray implements SpinMutex {

    private final SpinMutex[] mutex;

    public SpinMutexArray() {
        this(Runtime.getRuntime().availableProcessors()*8, 2 /* x8 bytes each, less than a 64 byte cache line */ );
    }

    public SpinMutexArray(int stripes, int stripeWidth) {
        stripes = Util.largestPowerOf2NoGreaterThan(stripes)*2;
        assert(stripes < (1 << 15));
        assert(stripeWidth < (1 << 15));
        mutex = new SpinMutex[stripes];
        for (int i  = 0; i < stripes; i++)
            mutex[i] = new Treadmill64(stripeWidth);
    }


    @Override
    public int start(long hash) {
        int s = (int) ((hash & (~(1 << 31))) % mutex.length);
        return mutex[s].start(hash) | (s << 16);
    }

    @Override
    public void end(int slot) {
        mutex[slot >> 16].end(slot & 0xffff);
    }
}
