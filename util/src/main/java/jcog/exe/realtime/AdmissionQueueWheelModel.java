package jcog.exe.realtime;

import jcog.TODO;
import jcog.data.list.MetalConcurrentQueue;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * uses central concurrent admission queue which is drained each cycle.
 * the wheel queues are (hopefully fast) ArrayDeque's safely accessed from one thread only
 */
public class AdmissionQueueWheelModel extends HashedWheelTimer.WheelModel implements Consumer<TimedFuture<?>> {

    /** capacity of incoming admission queue (not the entire wheel) */
    final static int ADMISSION_CAPACITY = 4096;

    final MetalConcurrentQueue<TimedFuture<?>> incoming = new MetalConcurrentQueue<>(ADMISSION_CAPACITY);

    final Queue<TimedFuture<?>>[] wheel;
    private transient int c;
    private HashedWheelTimer timer;

//    /** where incoming temporarily drains to */
//    final TimedFuture[] coming = new TimedFuture[ADMISSION_CAPACITY];

    public AdmissionQueueWheelModel(int wheels, long resolution) {
        super(wheels, resolution);

        this.wheel = new Queue[wheels];
        for (int i = 0; i < wheels; i++) {
            wheel[i] = new ArrayDeque();
        }
    }

    @Override
    public void restart(HashedWheelTimer h) {
        this.timer = h;
    }

    @Override
    public final void accept(TimedFuture<?> x) {
        schedule(x, c, timer);
    }

    @Override
    public int run(int c, HashedWheelTimer timer) {
        this.c = c;
        if (incoming.clear(this) > 0)
            timer.assertRunning();

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
                        q.poll().execute(timer);
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
        return incoming.isEmpty();
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
