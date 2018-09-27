package jcog.pri.bag.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

/** striping via 64bit (pair of 32bit codes) global exclusion locking via busy spin
 *  on a linear probed atomic array of fixed size */
public class Treadmill extends AtomicLongArray implements SpinMutex {

    private final AtomicInteger mod = new AtomicInteger(0);



    public Treadmill() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /** extra space for additional usage */
    public Treadmill(int slots) {
        super(slots);
    }

    @Override
    public int start(long hash) {
        final int slots = length();

        while (true) {

            int now = mod.getAcquire();

            
            boolean collision = false;
            for (int i = 0; i < slots; i++) {
                long v = get(i);
                if (v == hash) {
                    collision = true;
                    break; 
                }
            }

            if (!collision) {
                if (mod.weakCompareAndSetRelease(now, now+1)) {
                    for (int i = 0; i < slots; i++) {
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
