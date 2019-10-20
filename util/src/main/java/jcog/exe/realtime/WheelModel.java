package jcog.exe.realtime;

/**
 * scheduler implementation
 */public
abstract class WheelModel {
    public final int wheels;
    public final long resolution;

    protected WheelModel(int wheels, long resolution) {
        this.wheels = wheels;
        this.resolution = resolution;
    }

    public final void out(TimedFuture<?> x, HashedWheelTimer timer) {
        schedule(x, timer.cursor(), timer);
    }

    public final int idx(int cursor) {
        return cursor % wheels;
    }

    /**
     * returns how approximately how many entries were in the wheel at start.
     * used in part to determine if the entire wheel is empty.
     */
    public abstract int run(int wheel, HashedWheelTimer timer);

    /**
     * return false if unable to schedule
     */
    public abstract boolean accept(TimedFuture<?> r, HashedWheelTimer hashedWheelTimer);

    /**
     * return false if unable to reschedule
     */
    public abstract boolean reschedule(int wheel, TimedFuture r);

    public final boolean schedule(TimedFuture r, int c, HashedWheelTimer timer) {
        int offset = r.offset(resolution);
//            if (offset <= 0)
//                System.out.println(r);
        if (offset > -1 || r.isPeriodic()) {
            if (!reschedule(idx(c + offset), r))
                return false;
        } else {
            timer.execute(r);
        }
        return false;
    }

    /**
     * estimated number of tasks currently in the wheel
     */
    public abstract int size();

    /**
     * allows the model to interrupt the wheel before it decides to sleep
     */
    public abstract boolean isEmpty();

    public void restart(HashedWheelTimer h) {

    }
}
