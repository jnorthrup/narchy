package nars.task.util.series;

import nars.NAR;
import nars.Param;
import nars.table.dynamic.SeriesBeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

abstract public class AbstractTaskSeries<T extends SeriesBeliefTable.SeriesTask> implements TaskSeries<T> {

    protected final int cap;

    abstract protected T newTask(Term term, byte punc, long nextStart, long nextEnd, Truth next, NAR nar);

    protected abstract void push(T t);

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

    public T add(Truth next, long nextStart, long nextEnd, int dur, Term term, byte punc, NAR nar) {

        T nextT = null;
        T last = last();
        boolean stretchPrev = false;
        if (next != null && last != null) {
            long lastStart = last.start();
            long lastEnd = last.end();
            if (lastEnd > nextStart)
                return null;

            double gapDurs = ((double)(nextStart - lastEnd)) / dur;
            if (gapDurs <= stretchDurs()) {

                double stretchDurs = ((double)(nextEnd - lastStart)) / dur;
                if (stretchDurs <= latchDur()) {
                    Truth lastEnds = last.truth(lastEnd, dur);
                    if (lastEnds.equals(next)) {
                        //stretch
                        last.setEnd(nextEnd);
                        return last;
                    }
                }

                //form new task either because the value changed, or because the latch duration was exceeded
                long midGap = Math.max(lastEnd, (lastEnd + nextStart)/2L);
                assert(midGap >= lastEnd): lastEnd + " " + midGap + " "+ nextStart;
                last.setEnd(midGap);
                nextStart = midGap+1; //start the new task directly after the midpoint between its start and the end of the last task
                nextEnd = Math.max(nextStart, nextEnd);
                stretchPrev = false;

            } else {

                stretchPrev = false;
                nextStart = Math.max(nextStart, lastEnd+1);
                nextEnd = Math.max(nextEnd, nextStart);

                //form new task at the specified interval, regardless of the previous task since it was excessively long ago
                //TODO maybe grow the previous task half a gap duration
            }

        }

        //assert(nextStart <= nextEnd);

        if (!stretchPrev && next != null) {
            nextT = newTask(term, punc, nextStart, nextEnd, next, nar);
            if (nextT == null)
                return null;

            synchronized (this) {

                compress();

                push(nextT);

            }
        }

        return nextT;

    }




    private void compress() {
        int toRemove = (size()+1) - cap;
        while (toRemove-- > 0) {
            T x = pop();
            if (x!=null)
                x.delete();
        }
    }
}
