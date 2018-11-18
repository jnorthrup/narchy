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
        if (hash == 0) hash = 1; //skip 0

        final int slots = length();

        restart: while (true) {

            int now = mod.getOpaque();
            if (now == -1) now = 0; //skip 0

            for (int i = slots-1; i >= 0; i--) {
                long v = getAcquire(i);
                if (v == hash) {
                    Thread.onSpinWait();
                    continue restart;  //collision
                }
            }

            if (mod.compareAndSet(now, now+1)) { //TODO separate into modIn and modOut
                for (int j = 0; j < slots; j++)
                    if (weakCompareAndSetRelease(j, 0, hash))
                        return j;
            }

            Thread.onSpinWait();
        }
    }

    @Override
    public final void end(int slot) {
        setRelease(slot, 0);
    }



}
