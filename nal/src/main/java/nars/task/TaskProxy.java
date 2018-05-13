package nars.task;

import jcog.pri.Priority;
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
        //ignore
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
        return Task.equal(this, (Task) obj);
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
        //ignore
        //throw new UnsupportedOperationException();
        return p;
    }

    @Override
    public @Nullable Priority clonePri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete() {
        //ignore
        //throw new UnsupportedOperationException();
        return false;
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

//    @Override
//    public float freq() {
//        return task.freq();
//    }
//
//    @Override
//    public float freqMean() {
//        return task.freqMean();
//    }
//
//    @Override
//    public float freqMax() {
//        return task.freqMax();
//    }
//
//    @Override
//    public float freqMin() {
//        return task.freqMin();
//    }
//
//    @Override
//    public float conf() {
//        return task.conf();
//    }
//
//    @Override
//    public float evi() {
//        return task.evi();
//    }
//
//    @Override
//    public float evi(long when, long dur) {
//        return task.evi(when, dur);
//    }
//
//    @Override
//    public float eviInteg() {
//        return task.eviInteg();
//    }

    @Override
    public byte punc() {
        return task.punc();
    }


    //    /**
//     * adds a Truth cache
//     */
//    public static class WithTermCachedTruth extends WithTerm {
//
//        private final int dur;
//
//        final LongObjectHashMap<Truth> truthCached = new LongObjectHashMap<>(0);
//
//        public WithTermCachedTruth(Term term, Task task, int dur) {
//            super(term, task);
//            this.dur = dur;
//        }
//
//        @Override
//        public @Nullable Truth truth(long when, long dur) {
//            if (dur == this.dur) {
//                return truthCached.getIfAbsentPutWithKey(when, w -> super.truth(w, dur));
//            } else {
//                return super.truth(when, dur);
//            }
//        }
//
//
//    }

}
