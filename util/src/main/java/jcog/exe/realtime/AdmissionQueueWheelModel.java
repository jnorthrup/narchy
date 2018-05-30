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

    public AdmissionQueueWheelModel(int wheels, long resolution) {
        super(wheels, resolution);

        this.wheel = new Queue[wheels];
        for (int i = 0; i < wheels; i++) {
            wheel[i] = new ArrayDeque();
        }
    }

    @Override
    public int run(int c, HashedWheelTimer timer) {

        if (incomingCount.get() > 0) {
            int count = incoming.remove(buffer);
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    TimedFuture b = buffer[i];
                    buffer[i] = null;
                    schedule(b, c, timer);
                }
                Arrays.fill(buffer, 0, count, null);
                timer.assertRunning();
                incomingCount.addAndGet(-count);
            }
        }

        
        Queue<TimedFuture<?>> q = wheel[c];
        final int n = q.size();
        switch (n) {
            case 0: break; 
            case 1: {
                
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
                        break; 
                }
                break;
            }
            default: {
                
                Iterator<TimedFuture<?>> i = q.iterator();

                int remain = n;
                while (remain-- > 0) {

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
                            break; 
                    }

                }
            }
        }
        return n;



    }

    @Override
    public boolean canExit() {
        return incomingCount.get() == 0;
    }

    @Override
    public int size() {
        int sum = 0;
        for (Queue d : wheel) {
            sum += d.size();
        }
        return sum;
    }

    @Override public void schedule(TimedFuture<?> r) {
        if (r.state()==TimedFuture.Status.CANCELLED)
            throw new RuntimeException("scheduling an already cancelled task");

        incomingCount.incrementAndGet();

        boolean added = incoming.offer(r);
        if (!added)
            throw new RuntimeException("incoming queue overloaded");

    }

    @Override public void reschedule(int wheel, TimedFuture r) {
        if (!this.wheel[wheel].offer(r)) {
            throw new TODO("grow wheel capacity");
        }
    }



}
