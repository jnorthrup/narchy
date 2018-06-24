package nars.task;

import nars.Param;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

public class TaskProxy implements Task {

    public final Task task;

    public TaskProxy(Task task) {
        if (task==null)
            throw new NullPointerException();
        this.task = task;
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
    public void setCyclic(boolean b) {
        
    }

    /** produce a concrete, non-proxy clone */
    public Task clone() {
        return Task.clone(this);
    }

    @Override
    public boolean isCyclic() {
        return task.isCyclic();
    }

    @Override
    public Term term() {
        return task.term();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        return obj instanceof Task && Task.equal(this, (Task) obj);
    }

    @Override
    public int hashCode() {
        return Task.hash(term(), truth(), punc(), start(), end(), stamp());
    }

    @Override
    public short[] cause() {
        return task.cause();
    }

    @Override
    public float priSet(float p) {
        if (Param.DEBUG)
            throw new UnsupportedOperationException();
        return p;
    }

    @Override
    public boolean delete() {
        
        
        return false;
    }

    @Override
    public float pri() {
        return task.pri();
    }

    @Override
    public double coord(boolean maxOrMin, int dimension) {
        return task.coord(maxOrMin, dimension);
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

}
