package nars.task;

import jcog.pri.Priority;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

import static nars.Op.NEG;

public class TaskProxy implements Task {

    public final Task task;

    public TaskProxy(Task task) {
        this.task = task;
    }

    @Override
    public String toString() {
        return appendTo(null).toString();
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

    @Override
    public float eternalizability() {
        return task.eternalizability();
    }


    public static class WithTerm extends TaskProxy {

        public final Term term;


        public WithTerm(Term term, Task task) {
            super(task);
            assert(term.op()!=NEG);
            this.term = term;
        }

//        @Override
//        public float freq() {
//            if (isBeliefOrGoal()) {
//                float f = super.freq();
//                if (neg) f = 1-f;
//                return f;
//            }
//            return Float.NaN;
//        }
//
//        @Override
//        public @Nullable Truth truth() {
//            return isBeliefOrGoal() ? new PreciseTruth(freq(), conf()) : null;
//        }

        @Override
        public Term term() {
            return term;
        }

    }

    /**
     * adds a Truth cache
     */
    public static class WithTermCachedTruth extends WithTerm {

        private final int dur;

        final LongObjectHashMap<Truth> truthCached = new LongObjectHashMap<>(0);

        public WithTermCachedTruth(Term term, Task task, int dur) {
            super(term, task);
            this.dur = dur;
        }

        @Override
        public @Nullable Truth truth(long when, long dur) {
            if (dur == this.dur) {
                return truthCached.getIfAbsentPutWithKey(when, w -> super.truth(w, dur));
            } else {
                return super.truth(when, dur);
            }
        }


    }

    public static class WithTruthAndTime extends TaskProxy {

        public final long start, end;

        private final boolean negated;

        Object truth;

        public WithTruthAndTime(Task task, long start, long end, boolean negated, Supplier<Truth> truth) {
            super(task);
            this.start = start;
            this.end = end;
            this.negated = negated;

            this.truth = truth;
        }

        @Override
        public Term term() {
            return super.term().negIf(negated);
        }

        @Override
        public boolean equals(Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable Truth truth(long when, long dur, float minConf) {
            if (when < start) return null;
            if (when > end) return null;
            return truth();
        }



        @Override
        public @Nullable Truth truth() {
            Object tt = this.truth;

            if (tt instanceof Supplier) {
                Truth computed = ((Supplier<Truth>) tt).get();
                if (computed != null) {
                    if (negated) {
                        computed = computed.neg();
                    }
                    this.truth = computed;
                    return computed;
                }
            }
            return (Truth) tt;
        }

    }

}
