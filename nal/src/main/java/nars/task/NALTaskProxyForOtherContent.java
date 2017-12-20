package nars.task;

import jcog.pri.Priority;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/** limited functionality for a wrapping shadow task */
public class NALTaskProxyForOtherContent implements Task {

    public final Term term;
    public final Task task;

    public NALTaskProxyForOtherContent(Term term, Task task) {
        this.term = term;
        this.task = task;

    }

    @Override
    public void setCyclic(boolean b) {
        //ignore
    }

    @Override
    public boolean isCyclic() {
        return task.isCyclic();
    }

    @Override
    public Term term() {
        return term;
    }

    @Override
    public boolean equals(Object obj) {
        return Task.equal(this, (Task)obj);
    }

    @Override
    public int hashCode() {
        return Task.hash(term(), (DiscreteTruth)truth(), punc(), start(), end(), stamp());
    }

    @Override
    public short[] cause() {
        return task.cause();
    }

    @Override
    public <X> X meta(String key) {
        return null;
    }

    @Override
    public void meta(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <X> X meta(String key, Function<String, Object> valueIfAbsent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float priSet(float p) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Priority clonePri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float pri() {
        return task.pri();
    }

    @Override
    public float coordF(boolean maxOrMin, int dimension) {
        return task.coordF(maxOrMin, dimension);
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
