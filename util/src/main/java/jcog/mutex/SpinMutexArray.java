package jcog.mutex;


import jcog.Util;

import java.util.concurrent.atomic.AtomicLongArray;

/** contains 2 sub-treadmills
 * TODO parameterize the bit which it checks adjustable so these can be chained arbitrarily */
public final class SpinMutexArray implements SpinMutex {

    private final SpinMutex[] mutex;

    public SpinMutexArray() {
        this(Runtime.getRuntime().availableProcessors()*4, 2 );
    }

    public SpinMutexArray(int stripes, int stripeWidth) {
        stripes = Util.largestPowerOf2NoGreaterThan(stripes)*2;
        assert(stripes < (1 << 15));
        assert(stripeWidth < (1 << 15));
        mutex = new SpinMutex[stripes];

        AtomicLongArray buf = new AtomicLongArray(stripes * stripeWidth);
        for (int i  = 0; i < stripes; i++)
            mutex[i] = new Treadmill64(buf, stripeWidth, i*stripeWidth);
    }


    @Override
    public int start(long hash) {
        int s = (Long.hashCode(hash) & (~(1 << 31))) % mutex.length;
        return mutex[s].start(hash) | (s << 16);
    }

    @Override
    public void end(int slot) {
        mutex[slot >> 16].end(slot & 0xffff);
    }
}
