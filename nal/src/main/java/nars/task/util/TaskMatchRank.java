package nars.task.util;

import nars.Op;
import nars.Task;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.Revision;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;

/** query object used for selecting tasks */
@Deprecated public interface TaskMatchRank {


    /** max values to return */
    int limit();

    /** hard time limit. allows temporal index skipping */
    long timeMin();
    /** hard time limit. allows temporal index skipping */
    long timeMax();

    /** value ranking function */
    FloatFunction<Task> value();


    /** gets one task, sampled fairly from the available tasks in the
     * given range according to strength */
    static TaskMatchRank best(long start, long end, @Nullable Term template) {
        return
                template == null || !template.hasAny(Op.Temporal) ?
                        new Best(start, end) :
                        new BestWithFactor(start, end, t-> 1 / (1 + Revision.dtDiff(template, t.term())));
    }

    //TODO involving some randomness:
    //static TaskMatch sampled(long start, long end, @Nullable Term template) {


        /** prefilter */
    default boolean filter(Task task) {
        return true;
    }

    class Best implements TaskMatchRank, FloatFunction<Task> {
        private final long start;
        private final long end;
        private final FloatFunction<Task> value;

        public Best(long start, long end) {
            assert(start <= end);
            this.start = start;
            this.end = end;

            if (start == ETERNAL) {
                

                value = (Task t) -> (t.isBeliefOrGoal() ?
                        
                        
                        t.conf()
                        :
                        
                        1 + (t.pri() * t.originality())
                );
            } else {
                

                long dur = (end - start + 1);
                value = t ->
                    t.isBeliefOrGoal() ?
                        
                        TemporalBeliefTable.value(t, start, end, dur)

                        :

                        1 + (t.pri() / (1f + (float)(t.minTimeTo(start, end)/((double)dur)))) //questions
                ;
            }
        }

        @Override
        public float floatValueOf(Task x) {
            return value.floatValueOf(x);
        }

        @Override
        public final FloatFunction<Task> value() {
            return this;
        }

        @Override
        public long timeMin() {
            return start;
        }

        @Override
        public long timeMax() {
            return end;
        }

        @Override
        public int limit() {
            return 1;
        }

    }

    class BestWithFactor extends Best {

        private final FloatFunction<Task> factor;
        private float lowerLimit = Float.NEGATIVE_INFINITY;


        public BestWithFactor(long start, long end, FloatFunction<Task> factor) {
            super(start, end);
            this.factor = factor;
        }


        @Override
        public float floatValueOf(Task x) {

            float p = super.floatValueOf(x);
            if (limit()==1) {
                if (p > lowerLimit)
                    lowerLimit = p;
                else if (p < lowerLimit) {
                    
                    return Float.NEGATIVE_INFINITY;
                }
            }

            return p * factor.floatValueOf(x);
        }
    }
}
