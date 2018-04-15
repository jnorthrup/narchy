package jcog.exe.realtime;

/** TODO
 *  where each wheel is simply its own concurrent queue */
public class ConcurrentQueueWheelModel extends HashedWheelTimer.WheelModel {

    protected ConcurrentQueueWheelModel(int numWheels) {
        super(numWheels);
    }

    @Override
    public void run(int wheel, HashedWheelTimer timer) {

    }

    @Override
    public void schedule(TimedFuture<?> r) {

    }

    @Override
    public void reschedule(int wheel, TimedFuture r) {

    }
}
