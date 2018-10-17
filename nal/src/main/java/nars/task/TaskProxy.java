package nars.task;

import jcog.pri.UnitPri;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** implementations are immutable but will usually have a different hash and
  * equality than the origin task. hashcode here is calculated lazily.
 * the only mutable components are the hashcode and the cyclic status which is optionally inherited from the source.
 * */
public class TaskProxy extends UnitPri implements Task {

    public final Task task;

    private int hash = 0;

    private volatile boolean cyclic = false;

    public TaskProxy(Task task) {

        this.task = task;

        float p = task.pri();
        if (p!=p)
            delete();
        else
            pri(p);

        if (inheritCyclic() && task.isCyclic())
            setCyclic(true);
    }

    protected boolean inheritCyclic() {
        return true;
    }

    @Override
    public float freq(long start, long end) {
        return task.freq(start, end);
    }

    @Override
    public String toString() {
        return appendTo(null).toString();
    }

    @Override
    public final void setCyclic(boolean b) {
        this.cyclic = b;
    }

    /** produce a concrete, non-proxy clone */
    public Task clone() {
        return Task.clone(this);
    }

    @Override
    public final boolean isCyclic() {
        return cyclic;
    }

    @Override
    public Term term() {
        return task.term();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Task && Task.equal(this, (Task) obj);
    }

    @Override
    public int hashCode() {
        int h = this.hash;
        if (h == 0)
            return this.hash = Task.hash(term(), truth(), punc(), start(), end(), stamp());
        else
            return h;
    }

    @Override
    public short[] cause() {
        return task.cause();
    }




    @Override
    public double coord(int dimension, boolean maxOrMin) {
        return task.coord(dimension, maxOrMin);
    }

    @Override
    public long creation() {
        return task.creation();
    }

    @Override
    public long start() {
        return task.start();
    }

    @Override
    public long end() {
        return task.end();
    }

    @Override
    public long[] stamp() {
        return task.stamp();
    }

    @Override
    public @Nullable Truth truth() {
        return task.truth();
    }

    @Override
    public byte punc() {
        return task.punc();
    }

    /** produce a concrete non-proxy clone of this */
    public NALTask the() {
        return Task.clone(this);
    }
}
