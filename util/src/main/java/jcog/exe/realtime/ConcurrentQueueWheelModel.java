package jcog.exe.realtime;

import jcog.TODO;

/** TODO
 *  where each wheel is simply its own concurrent queue */
public class ConcurrentQueueWheelModel extends HashedWheelTimer.WheelModel {

    protected ConcurrentQueueWheelModel(int wheels, long resolution) {
        super(wheels, resolution);
    }

    @Override
    public int run(int wheel, HashedWheelTimer timer) {

        return wheel;
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
