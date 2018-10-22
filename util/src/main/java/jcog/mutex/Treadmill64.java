package jcog.mutex;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

/** striping via 64bit (pair of 32bit codes) global exclusion locking via busy spin
 *  on a linear probed atomic array of fixed size */
public class Treadmill64 extends AtomicLongArray implements SpinMutex {

    private final AtomicInteger mod = new AtomicInteger(0);

    public Treadmill64() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /** extra space for additional usage */
    public Treadmill64(int slots) {
        super(slots);
    }

    @Override
    public int start(long hash) {
        final int slots = length();

        while (true) {

            int now = mod.getOpaque();

            for (int i = slots-1; i >= 0; i--) {
                long v = getOpaque(i);
                if (v == hash)
                    break;  //collision

                if (i == 0) {
                    if (mod.compareAndSet(now, now+1)) {
                        for (int j = 0; j < slots; j++)
                            if (weakCompareAndSetAcquire(i, 0, hash))
                                return i;
                    }
                }
            }

            Thread.onSpinWait();
        }
    }

    @Override
    public final void end(int slot) {
        setRelease(slot, 0);
    }



}
