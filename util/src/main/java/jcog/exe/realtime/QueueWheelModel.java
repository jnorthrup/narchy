package jcog.exe.realtime;

import jcog.Util;

import java.util.Queue;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public class QueueWheelModel extends HashedWheelTimer.WheelModel {

    /** the wheels (array of queues) */
    final Queue<TimedFuture>[] q;

    public QueueWheelModel(int wheels, Supplier<Queue<TimedFuture>> queueBuilder, long resolution) {
        super(wheels, resolution);
        assert(wheels > 1);
        q = new Queue[wheels];
        for (int i = 0; i < wheels; i++)
            q[i] = queueBuilder.get();
    }

    @Override
    public int run(int c, HashedWheelTimer timer) {
        Queue<TimedFuture> q = this.q[c];

        final int n = q.size();
        if (n == 0)
            return 0;
        else if (n==1) {
            //special optimized case: the only element can be peek'd without poll/offer in case it remains pending
            TimedFuture r = q.peek();
            switch (r.state()) {
                case CANCELLED:
                    q.poll();
                    break;
                case READY:
                    q.poll();
                    r.execute(timer);
                    break;
                case PENDING:
                    break; //<--- ideally most common path
            }
        } else {

            //TODO if n=2 and the previous or next queue is empty try moving one of the items there. this will distribute items across wheels so each has an ideal 0 or 1 size

            for (int i = 0; i < n; i++) {
                TimedFuture r = q.poll();
                switch (r.state()) {
                    case CANCELLED:
                        break;
                    case READY:
                        r.execute(timer);
                        break;
                    case PENDING:
                        q.offer(r); //re-insert
                        break;
                }
            }
        }
        return n;
    }

    @Override
    public boolean accept(TimedFuture<?> r, HashedWheelTimer t) {
        return t.reschedule(r); //immediately
    }

    @Override
    public boolean reschedule(int wheel, TimedFuture r) {

        int remain = q.length - 1;
        do {
            if (q[wheel].offer(r))
                return true;
            if (++wheel == q.length) wheel = 0;
        } while (--remain > 0);

        return false;
    }

    @Override
    public int size() {
        return Util.sum((ToIntFunction<Queue>) Queue::size, q);
    }

    @Override
    public boolean isEmpty() {
        for (Queue<TimedFuture> q : q)
            if (!q.isEmpty())
                return false;
        return true;
    }


}
