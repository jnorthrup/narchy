package jcog.exe.realtime;

import jcog.TODO;
import jcog.data.list.MetalConcurrentQueue;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;

import static jcog.exe.realtime.TimedFuture.*;

/**
 * uses central concurrent admission queue which is drained each cycle.
 * the wheel queues are (hopefully fast) ArrayDeque's safely accessed from one thread only
 */
public class AdmissionQueueWheelModel extends WheelModel {

    /** capacity of incoming admission queue (not the entire wheel) */
    static final int ADMISSION_CAPACITY = 4096;

    final MetalConcurrentQueue<TimedFuture<?>> incoming = new MetalConcurrentQueue<>(ADMISSION_CAPACITY);

    final Queue<TimedFuture<?>>[] wheel;

//    /** where incoming temporarily drains to */
//    final TimedFuture[] coming = new TimedFuture[ADMISSION_CAPACITY];

    public AdmissionQueueWheelModel(int wheels, long resolution) {
        super(wheels, resolution);

        this.wheel = new Queue[wheels];
        for (int i = 0; i < wheels; i++) {
            wheel[i] = new ArrayDeque();
        }
    }

    /**
     * HACK TODO note this method isnt fair because it implicitly prioritizes 'tenured' items that were inserted and remained.
     * instead it should behave like ConcurrentQueueWheelModel's impl
     */
    @Override public int run(int c, HashedWheelTimer timer) {
        if (incoming.clear(this::out, timer) > 0)
            timer.assertRunning(); //is this necessary?

        Queue<TimedFuture<?>> q = wheel[c];
        int n = q.size();
        switch (n) {
            case 0: break; 
            case 1: {
                
                TimedFuture<?> r = q.peek();
                switch (r.state()) {
                    case CANCELLED:
                        q.poll();
                        break;
                    case READY:
                        q.poll().execute(timer);
                        break;
                    case PENDING:
                        break; 
                }
                break;
            }
            default: {


                //Fair
                for (int remain = n; remain > 0; remain--) {
                    TimedFuture<?> r = q.poll();
                    switch (r.state()) {
                        case CANCELLED:
                            break;
                        case READY:
                            r.execute(timer);
                            break;
                        case PENDING:
                            if (!q.offer(r))
                                throw new TODO();
                            break;
                    }
                }

            }
        }


        return n;
    }

    @Override
    public boolean isEmpty() {
        return incoming.isEmpty();
    }

    @Override
    public int size() {
        return Arrays.stream(wheel).mapToInt(Collection::size).sum();
    }



    @Override public boolean accept(TimedFuture<?> r, HashedWheelTimer t) {
        return incoming.offer(r);
    }

    @Override public boolean reschedule(int wheel, TimedFuture r) {
        return this.wheel[wheel].offer(r);
    }

}
