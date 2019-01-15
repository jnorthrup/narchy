package jcog.exe.realtime;

import jcog.TODO;
import jcog.data.list.MetalConcurrentQueue;

/** TODO
 *  where each wheel is simply its own concurrent queue */
public class ConcurrentQueueWheelModel extends HashedWheelTimer.WheelModel {

    final MetalConcurrentQueue<TimedFuture>[] wheel;

    protected ConcurrentQueueWheelModel(int wheels, long resolution) {
        super(wheels, resolution);
        wheel = new MetalConcurrentQueue[wheels];
    }

    @Override
    public int run(int c) {
        MetalConcurrentQueue<TimedFuture> q = wheel[c];
        final int n = q.size();
        for (int i = 0; i < n; i++) {
            //q.poll()
        }
        throw new TODO();
//        switch (n) {
//            case 0: break;
//            case 1: {
//
//                TimedFuture<?> r = q.peek();
//                switch (r.state()) {
//                    case CANCELLED:
//                        q.poll();
//                        break;
//                    case READY:
//                        q.poll();
//                        r.execute(timer);
//                        break;
//                    case PENDING:
//                        break;
//                }
//                break;
//            }
//            default: {
//
//                Iterator<TimedFuture<?>> i = q.iterator();
//
//                int remain = n;
//                while (remain-- > 0) {
//
//                    TimedFuture<?> r = i.next();
//
//                    switch (r.state()) {
//                        case CANCELLED:
//                            i.remove();
//                            break;
//                        case READY:
//                            i.remove();
//                            r.execute(timer);
//                            break;
//                        case PENDING:
//                            break;
//                    }
//
//                }
//            }
//        }
//        return n;

    }

    @Override
    public void schedule(TimedFuture<?> r) {

    }

    @Override
    public void reschedule(int wheel, TimedFuture r) {

    }

    @Override
    public int size() {
        throw new TODO();
    }

    @Override
    public boolean canExit() {
        return true;
    }
}
