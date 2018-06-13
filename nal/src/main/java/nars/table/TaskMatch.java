package nars.table;

import nars.Task;
import nars.task.Revision;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static nars.time.Tense.ETERNAL;

/** query object used for selecting tasks */
public interface TaskMatch  {

    /** max values to return */
    int limit();
    
    long start();
    long end();

    /** value ranking function */
    FloatFunction<Task> value();

    /** the RNG used if to apply random sampling, null to disable sampling (max first descending) */
    default @Nullable Random random() {
        return null;
    }

    /** answer the strongest available task within the specified range */
    static TaskMatch best(long start, long end) {
        return new Best(start, end);
    }

    static TaskMatch best(long start, long end, Term template, Random rng) {
        assert(template!=null);
        return best(start, end, t-> 1 / (1 + Revision.dtDiff(template, t.term())), rng);
    }

    static TaskMatch best(long start, long end, Random rng) {
        return best(start, end, t-> 1, rng);
    }
    static TaskMatch best(long start, long end, FloatFunction<Task> factor, Random rng) {
        return new BestWithFactor(start, end, factor, rng);
    }

    /** gets one task, sampled fairly from the available tasks in the
     * given range according to strength */
    static TaskMatch sampled(long start, long end, @Nullable Term template, @Nullable Random random) {
        return
                template == null || (!template.isTemporal()) ?
                        TaskMatch.best(start, end, random) :
                        TaskMatch.best(start, end, template, random);

    }

    /** prefilter */
    default boolean filter(Task task) {
        return true;
    }

    class Best implements TaskMatch, FloatFunction<Task> {
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
                

                int dur = (int)(end - start + 1);
                value = (Task t) -> (t.isBeliefOrGoal() ?
                        
                        TemporalBeliefTable.value(t, start, end, dur)
                        :
                        
                        1 + (t.pri() / (1f + (t.minTimeTo(start, end)/((float)dur))))
                );
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
        public long start() {
            return start;
        }

        @Override
        public long end() {
            return end;
        }

        @Override
        public int limit() {
            return 1;
        }

    }

    class BestWithFactor extends Best {

        private final FloatFunction<Task> factor;
        @Nullable private final Random random;
        private float lowerLimit = Float.NEGATIVE_INFINITY;

        public BestWithFactor(long start, long end, FloatFunction<Task> factor) {
            this(start, end, factor, null);
        }

        public BestWithFactor(long start, long end, FloatFunction<Task> factor, @Nullable Random random) {
            super(start, end);
            this.factor = factor;
            this.random = random;
        }

        @Override
        public @Nullable Random random() {
            return random;
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
