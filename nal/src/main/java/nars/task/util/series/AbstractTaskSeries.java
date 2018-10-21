package nars.task.util.series;

import nars.Param;
import nars.Task;
import nars.task.util.TimeRangeFilter;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.TIMELESS;

abstract public class AbstractTaskSeries<T extends Task> implements TaskSeries<T> {

    protected final int cap;


    public abstract void push(T t);

    @Nullable protected abstract T pop();

    public AbstractTaskSeries(int cap) {
        this.cap = cap;
    }

    /**
     * maximum durations a steady signal can grow for
     */
    public float latchDur() {
        return Param.SIGNAL_LATCH_DUR;
    }
    public float stretchDurs() {
        return Param.SIGNAL_STRETCH_DUR;
    }





    public void compress() {
        int toRemove = (size()+1) - cap;
        while (toRemove-- > 0) {
            T x = pop();
            if (x!=null)
                x.delete();
        }
    }

    public boolean contains(TimeRangeFilter time) {
        long s = start();
        if (s!=TIMELESS){
            return time.containedBy(s, end());
        }
        return false;
    }
}
