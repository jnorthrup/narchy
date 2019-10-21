package nars.table.question;

import jcog.Util;
import jcog.data.NumberX;
import jcog.math.LongInterval;
import jcog.math.Longerval;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import nars.NAR;
import nars.Task;
import nars.control.op.Remember;
import nars.task.util.Answer;
import nars.term.Term;
import nars.truth.Stamp;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;

public class HijackQuestionTable extends PriHijackBag<Task, Task> implements QuestionTable {

    public HijackQuestionTable(int cap, int reprobes) {
        super(cap, reprobes);
    }

    @Override
    protected Task merge(Task existing, Task incoming, NumberX overflowing) {
        return existing;
    }


    @Override
    public void match(Answer a) {
        sample(a.nar.random(), a.tasks.capacity(), a);
    }

    /** optimized for cases with zero and one stored tasks */
    @Override public Task match(long start, long end, Term template, float dur, NAR nar) {
        switch (size()) {
            case 0: return null;
            case 1:
                return next(0, new Predicate<Task>() {
                    @Override
                    public boolean test(Task t) {
                        return false;
                    }
                });
            default:
                return QuestionTable.super.match(start, end, template, dur, nar);
        }
    }

    @Override
    public final Task key(Task value) {
        return value;
    }

    @Override
    public final boolean isEmpty() {
        return super.isEmpty();
    }


    /** finds nearly equivalent and temporally combineable task.
     * returns null if insertion should proceed with the 'x' instance.
     * if returns 'x' it means its already present
     * if it returns another instance it is an equal but different
     * */
    private @Nullable Task preMerge(Task x) {
        if (isEmpty())
            return null;

        long[] xStamp = x.stamp();

        long xs = TIMELESS, xe = TIMELESS;
        for (Task y : this) {
            if (x.equals(y))
                return y; //found exact

            if (x.term().equals(y.term())) {

                //if (Arrays.equals(xStamp, y.stamp())) {
                long[] yStamp = y.stamp();
                int xys = Stamp.equalsOrContains(xStamp, yStamp);
                if (xys!=Integer.MIN_VALUE) {
                    if (xs == TIMELESS) {
                        xs = x.start(); xe = x.end();
                    }
                    //if (LongInterval.intersectLength(xs, xe, ys, ye)>0) {
                    //if (LongInterval.intersectsSafe(xs, xe, ys, ye)) {
                    if ((xys == 0 || xys == 1) && y.containsSafe(xs, xe))  {
                        //x contained within y, so merge
                        //TODO boost y priority if contributes to it, in proportion to range
                        return y;
                    } else if ((xys == 0 || xys == -1) && y.containedBySafe(xs, xe)) { //TODO y.containedBy(xs,xe)
                        //y contained within x, so remove y.   expect x to get inserted next
                        //TODO boost x priority if contributes to it, in proportion to range
                        remove(y);
                        y.delete();
                        return null;
                    } else if (xys==0 && y.intersects(xs, xe)) {
                        long ys = y.start(), ye = y.end();
                        Longerval u = LongInterval.union(xs, xe, ys, ye);

                        float newPri = xs == ETERNAL || ys == ETERNAL ?
                            Util.or(x.priElseZero(), y.priElseZero()) :
                            (float) (((double) x.priElseZero() * (double)(xe-xs) + (double) (y.priElseZero() * (float) (ye - ys))) / (double) (u.end - u.start));

                        //use larger of the 2 stamps (subsume's smaller)
                        //long[] stamp = sc == +1 ? xStamp : yStamp; //TODO determine if this is safe


                        Task xy = Task.clone(x, x.term(), null, x.punc(), u.start, u.end, xStamp);
                        Task.merge(xy, new Task[] { x, y }, newPri);
                        remove(y); y.delete();
                        return xy;
                    }
                }
            }
        }

        return null;
    }


    @Override
    public void remember(Remember r) {
        Task x = r.input;

        Task y = preMerge(x);
        if (y == null) {
            x = put(x);
            commit(forget(r.nar().questionForgetRate.floatValue() /* estimate */));
            r.remember(x);
        } else
            r.merge(y);
    }

    @Override
    public final int taskCount() {
        return size();
    }

    @Override
    public final void setTaskCapacity(int newCapacity) {
        setCapacity(newCapacity);
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        forEachKey(x);
    }

    @Override
    public boolean removeTask(Task x, boolean delete) {
        Task r = remove(x);
        if (r != null) {
            if (delete)
                r.delete();
            return true;
        }
        return false;
    }

    @Override
    public Stream<? extends Task> taskStream() {
        return stream();
    }

}
