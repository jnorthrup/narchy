package jcog.exe.realtime;

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.TODO;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * uses central concurrent admission queue which is drained each cycle.
 * the wheel queues are (hopefully fast) ArrayDeque's safely accessed from one thread only
 */
public class AdmissionQueueWheelModel extends HashedWheelTimer.WheelModel {
    /**
     * for fast test for incoming items
     */
    final AtomicInteger incomingCount = new AtomicInteger();

    /** capacity of incoming admission queue (not the entire wheel) */
    final static int ADMISSION_CAPACITY = 4096;

    final ConcurrentQueue<TimedFuture<?>> incoming = new DisruptorBlockingQueue<>(ADMISSION_CAPACITY);

    final Queue<TimedFuture<?>>[] wheel;

    final TimedFuture[] buffer = new TimedFuture[ADMISSION_CAPACITY];

    public AdmissionQueueWheelModel(int wheels) {
        super(wheels);

        this.wheel = new Queue[wheels];
        for (int i = 0; i < wheels; i++) {
            wheel[i] = new ArrayDeque();
        }
    }

    @Override
    public void run(int c, HashedWheelTimer timer) {

        if (incomingCount.get() > 0) {
            int count = incoming.remove(buffer);
            incomingCount.addAndGet(-count);
            for (int i = 0; i < count; i++) {
                TimedFuture b = buffer[i];
                buffer[i] = null;
                schedule(b, c);
            }
            Arrays.fill(buffer, 0, count, null);
        }

        // TODO: consider extracting processing until deadline for test purposes
        Queue<TimedFuture<?>> q = wheel[c];
        if (q.isEmpty())
            return;

        int n = q.size();
        switch (n) {
            case 0: break; //shoudlnt happen really
            case 1: {
                //simple case
                TimedFuture<?> r = q.peek();
                switch (r.state()) {
                    case CANCELLED:
                        q.poll();
                        break;
                    case READY:
                        q.poll();
                        r.execute(timer);
                        break;
                    case PENDING:
                        break; //keep
                }
                break;
            }
            default: {
                //use an iterator
                Iterator<TimedFuture<?>> i = q.iterator();

                while (i.hasNext() && n-- > 0) {

                    TimedFuture<?> r = i.next();

                    switch (r.state()) {
                        case CANCELLED:
                            i.remove();
                            break;
                        case READY:
                            i.remove();
                            r.execute(timer);
                            break;
                        case PENDING:
                            break; //keep
                    }

                }
            }
        }



    }




    @Override public void schedule(TimedFuture<?> r) {
        boolean added = incoming.offer(r);
        if (!added) {
            throw new RuntimeException("incoming queue overloaded");
        }

        incomingCount.incrementAndGet();
    }

    @Override public void reschedule(int wheel, TimedFuture r) {
        if (!this.wheel[wheel].offer(r)) {
            throw new TODO("grow wheel capacity");
        }
    }



}
