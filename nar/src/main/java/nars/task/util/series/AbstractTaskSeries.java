package nars.task.util.series;

import nars.NAL;
import nars.Task;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractTaskSeries<T extends Task> implements TaskSeries<T> {

    private final int cap;


    public abstract void push(T t);

    /** remove the oldest task, and delete it */
    protected abstract @Nullable T pop();

    AbstractTaskSeries(int cap) {
        this.cap = cap;
    }

    /**
     * maximum durations a steady signal can grow for
     */
    public static float latchDurs() {
        return NAL.signal.SIGNAL_LATCH_LIMIT_DURS;
    }
    public static float stretchDurs() {
        return NAL.signal.SIGNAL_STRETCH_LIMIT_DURS;
    }

    public final void compress() {
        var toRemove = (size()+1) - cap;
        while (toRemove-- > 0) {
            var x = pop();
            if (x!=null)
                x.delete();
        }
    }

}
