package jcog.decide;

import com.google.common.base.Joiner;
import jcog.TODO;
import jcog.Util;
import jcog.list.FastCoWList;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class AtomicRoulette<X> {

    /**
     * TODO this can be smaller per cause, ex: byte
     */
    private final AtomicIntegerArray pri;

    public final FastCoWList<X> choice;

    private final BlockingQueue<X> onQueue = Util.blockingQueue(16);
    private final BlockingQueue<X> offQueue = Util.blockingQueue(16);

    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final AtomicInteger priTotal = new AtomicInteger(0);

    public AtomicRoulette(int capacity, IntFunction<X[]> arrayBuilder) {
        this.choice = new FastCoWList<>(capacity, arrayBuilder);
        this.pri = new AtomicIntegerArray(capacity);
    }

    public void add(X c) {
        onQueue.add(c);
    }

    protected void remove(X c) {
        offQueue.add(c);
    }

    @Override
    public String toString() {

        if (choice.isEmpty())
            return "empty";
        else
            return Joiner.on("\n").join(IntStream.range(0, choice.size()).mapToObj(
                    x -> pri.get(x) + "=" + choice.getSafe(x)
            ).iterator());
    }

    public boolean commit(@Nullable Runnable r) {
        if (!busy.compareAndSet(false, true))
            return false;

        try {
            if (!onQueue.isEmpty()) {
                onQueue.removeIf((adding) -> {

                    int slot = findSlot(adding);
                    if (slot != -1) {
                        choice.set(slot, adding);
                        onAdd(adding, slot);
                    }
                    return true;
                });
            }
            if (!offQueue.isEmpty()) {
                throw new TODO(); //may need to reassign 'id' if the order changed
            }

            if (r != null)
                r.run();


        } finally {
            busy.set(false);
        }

        return true;

    }

    private int findSlot(X x) {
        int i;
        int s = choice.size();
        for (i = 0; i < s; i++) {
            X ci = choice.getSafe(i);
            if (ci == null)
                return i;
            else if (ci == x)
                return -1; //already have it
        }

        if (i < pri.length())
            return i; //grow

        throw new RuntimeException("overload");
    }

    protected void onAdd(X x, int slot) {

    }

    /**
     * priGetAndSet..
     */
    public int priGetAndSet(int i, int y) {
        int x = pri.getAndSet(i, y);
        if (y != x) {
            int t = priTotal.addAndGet(y - x);
            //assert (t >= 0 && t <= PRI_GRANULARITY * pri.length());
        }
        return x;
    }

    public boolean priGetAndSetIfEquals(int i, int x0, int y) {
        if (pri.compareAndSet(i, x0, y)) {
            int t = priTotal.addAndGet(y - x0);
            //assert (t >= 0 && t <= PRI_GRANULARITY * pri.length());
            return true;
        }
        return false;
    }

    public int pri(int i) {
        return pri.get(i);
    }

    public void decide(Random rng, IntPredicate kontinue) {

        int i = 0;

        boolean kontinued;
        restart: do {

            int priTotal = this.priTotal.get();
            if (priTotal == 0)
                kontinued = kontinue.test(-1);
            else {
                int count = choice.size();

                int distance = (int) (rng.nextFloat() * priTotal);


//                boolean dir = rng.nextBoolean(); //randomize the direction
//                int pp;
//                int start = i;
//                while (((distance = distance - (pp = pri.get(i))) > 0) && (pp == 0)) {
//                    if (dir) { //TODO unroll this to two outer loops, not decide this inside
//                        if (++i == count) i = 0;
//                    } else {
//                        if (--i == -1) i = count - 1;
//                    }
//                    if (i == start) {
//                        i = -1; //idle signal that nothing was selected
//                        break;
//                    }
//                }


                int pp;
                int start = i;
                while (((pp = pri.get(i)) == 0) || ((distance = distance - pp) > 0)) {
                    if (++i == count) i = 0;
                    if (i == start) {
                        kontinued = kontinue.test(-1); //idle signal that nothing was selected
                        continue restart;
                    }
                }

                kontinued = kontinue.test(i);
            }

        } while (kontinued);

    }


}
