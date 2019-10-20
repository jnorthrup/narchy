package jcog.mutex;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

/** striping via 64bit (pair of 32bit codes) global exclusion locking via busy spin
 *  on a linear probed atomic array of fixed size */
public class Treadmill64 implements SpinMutex {

    final AtomicLongArray buf;
    final int size;
    final int offset;
    private final AtomicInteger mod = new AtomicInteger(0);


    /** extra space for additional usage */
    public Treadmill64(AtomicLongArray buf, int size, int offset) {
        this.buf = buf;
        this.size = size;
        this.offset = offset;
    }

    @Override
    public int start(long hash) {
        if (hash == 0L) hash = 1L; //skip 0

        restart: while (true) {

            int now = mod.getAcquire();

            /** optimistic pre-scan determined free slot */
            int jProlly = -1;

            for (int i = offset; i < size+offset; i++) {
                long v = buf.getOpaque(i);
                if (v == hash) {
                    Thread.onSpinWait();
                    continue restart;  //collision
                } else if (v == 0L && jProlly == -1)
                    jProlly = i; //first empty cell candidate
            }

            if (mod.weakCompareAndSetRelease(now, now+1)) { //TODO separate into modIn and modOut?
                if (jProlly != -1 && buf.weakCompareAndSetAcquire(jProlly, 0L, hash))
                    return jProlly;

                int os = offset + size;
                for (int j = offset; j < os; j++)
                    if (j!=jProlly && buf.weakCompareAndSetAcquire(j, 0L, hash))
                        return j;
            }

            Thread.onSpinWait();
        }
    }

    @Override
    public final void end(int slot) {
        buf.setRelease(slot, 0L);
    }



}
