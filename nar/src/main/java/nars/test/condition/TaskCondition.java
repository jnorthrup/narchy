package nars.test.condition;


import jcog.Texts;
import jcog.sort.RankedN;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.control.MetaGoal;
import nars.task.Tasked;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Float.NaN;
import static nars.Op.NEG;

/**
 * TODO evolve this into a generic tool for specifying constraints and conditions
 * on memory (beliefs, and other measurable quantities/qualities).
 * use these to form adaptive runtime hypervisors ensuring optimal and correct operation
 */
abstract public class TaskCondition implements NARCondition, Predicate<Task>, Consumer<Tasked> {


    /**
     * whether to apply meta-feedback to drive the reasoner toward success conditions
     */
    public static final boolean feedback = false;
    public final Set<Task> matches = new UnifiedSet(1);
    private boolean matched;

    private static String rangeStringN2(float min, float max) {
        return '(' + Texts.n2(min) + ',' + Texts.n2(max) + ')';
    }

    /**
     * a heuristic for measuring the difference between terms
     * in range of 0..100%, 0 meaning equal
     */
    private static float termDistance(Term a, Term b, float ifLessThan) {
        if (a.equals(b)) return 0;

        float dist = 0;
        if (a.op() != b.op()) {

            dist += 0.4f;
            if (dist >= ifLessThan) return dist;
        }

        if (a.subs() != b.subs()) {
            dist += 0.3f;
            if (dist >= ifLessThan) return dist;
        }

        if (a.structure() != b.structure()) {
            dist += 0.2f;
            if (dist >= ifLessThan) return dist;
        }

        dist += Texts.levenshteinFraction(
                a.toString(),
                b.toString()) * 0.1f;

        if (a.dt() != b.dt()) {
            dist *= 2;
        }

        return dist;
    }

    abstract public boolean matches(@Nullable Task task);

    @Override
    public final boolean test(Task t) {

        if (!matched && matches(t)) {
            matches.add(t);
            matched = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final void accept(Tasked tasked) {
        if (!matched)
            test(tasked.task());
    }

    @Override
    public long getFinalCycle() {
        return -1;
    }

    @Override
    public final boolean isTrue() {
        return matched;
    }

    @Override
    public void log(boolean condition, Logger logger) {
        String msg = this + "\n" + (condition ? " OK" : "ERR" + '\t' + this);
        if (condition) {
            logger.info(msg);

            if (matches != null && logger.isTraceEnabled()) {
                matches.forEach(s -> logger.trace("\t{}", s));
            }
        } else {
            //assert (matched.isEmpty());

            logger.error(msg);



        }


    }

    public static class DefaultTaskCondition extends TaskCondition {
        private final static int maxSimilars = 2;
        public final float confMin;
        @Deprecated
        protected final RankedN<Task> similar;
        protected final NAR nar;
        private final byte punc;
        private final Term term;
        private final LongLongPredicate time;
        private final float freqMin;
        private final float freqMax;
        private final float confMax;
        private final long creationStart;

        private final long creationEnd;

        public long getFinalCycle() {
            return creationEnd;
        }

        public DefaultTaskCondition(NAR n, long creationStart, long creationEnd, Term term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time) throws RuntimeException {


            if (freqMax < freqMin) throw new RuntimeException("freqMax < freqMin");
            if (confMax < confMin) throw new RuntimeException("confMax < confMin");

            if (creationEnd - creationStart < 1)
                throw new RuntimeException("cycleEnd must be after cycleStart by at least 1 cycle");

            this.nar = n;
            this.time = time;

            this.creationStart = creationStart;
            this.creationEnd = creationEnd;

            this.confMax = Math.min(1.0f, confMax);
            this.confMin = Math.max(0.0f, confMin);
            this.punc = punc;

            this.similar = new RankedN<>(new Task[maxSimilars], this::value);

            if (term.op() == NEG) {
                term = term.unneg();
                freqMax = 1f - freqMax;
                freqMin = 1f - freqMin;
                if (freqMin > freqMax) {
                    float f = freqMin;
                    freqMin = freqMax;
                    freqMax = f;
                }
            }

            this.freqMax = Math.min(1.0f, freqMax);
            this.freqMin = Math.max(0.0f, freqMin);

            this.term = term;

        }

        @Override
        public void log(boolean condition, Logger logger) {
            super.log(condition, logger);

//            if(!condition) {
                if (!similar.isEmpty()) {
                    similar.forEach(s -> {
                        String pattern = "SIM {}\n{}";

                        logger.info(pattern,
                                s.proof(),
                                MetaGoal.proof(s, nar)
                                //s.proof()
                        );
                    });
                }
//            }
        }

        @Override
        public String toString() {
            return term.toString() + ((char) punc) + " %" +
                    rangeStringN2(freqMin, freqMax) + ';' + rangeStringN2(confMin, confMax) + '%' + ' ' +
                    " creation: (" + creationStart + ',' + creationEnd + ')';
        }


        @Override
        public boolean matches(@Nullable Task task) {

            similar.add(task);

            if (task == null)
                return false;

            if (task.punc() != punc)
                return false;

            if (!truthMatches(task))
                return false;

            Term tt = task.term();
            if (!tt.equals(this.term)) {
                if (tt.op() == term.op() && tt.volume() == this.term.volume() && tt.structure() == this.term.structure() && this.term.toString().equals(tt.toString())) {
                    throw new RuntimeException("target construction problem: " + this.term + " .toString() is equal to " + tt + " but inequal otherwise");
                }
                return false;
            }

            return creationTimeMatches() && occurrenceTimeMatches(task);

        }

        private boolean creationTimeMatches() {
            long now = nar.time();
            return (((creationStart == -1) || (now >= creationStart)) &&
                    ((creationEnd == -1) || (now <= creationEnd)));
        }

        private boolean occurrenceTimeMatches(Task t) {
            return time.accept(t.start(), t.end());
        }

        private boolean truthMatches(Truthed task) {
            Truth tt = task.truth();
            if ((punc == Op.BELIEF) || (punc == Op.GOAL)) {

                float co = tt.conf();
                if ((co > confMax) || (co < confMin))
                    return false;

                float fr = tt.freq();
                return (fr <= freqMax && fr >= freqMin);
            } else {
                return tt == null;
            }
        }

        protected float value(Task task, float worstDiffNeg) {

            float worstDiff = -worstDiffNeg;

            float difference = 0;
            if (task.punc() != punc)
                difference += 1000;
            if (difference >= worstDiff)
                return NaN;

            Term tterm = task.term();
            difference +=
                    100 * termDistance(tterm, term, worstDiff);
            if (difference >= worstDiff)
                return NaN;

            if (task.isBeliefOrGoal()) {
                float f = task.freq();
                float freqDiff = Math.min(
                        Math.abs(f - freqMin),
                        Math.abs(f - freqMax));
                difference += 10 * freqDiff;
                if (difference >= worstDiff)
                    return NaN;

                float c = task.conf();
                float confDiff = Math.min(
                        Math.abs(c - confMin),
                        Math.abs(c - confMax));
                difference += 1 * confDiff;
                if (difference >= worstDiff)
                    return NaN;
            }

//        if (difference >= worstDiff)
//            return;

            difference += 0.00001f * ((float) Math.abs(task.hashCode()) / (Integer.MAX_VALUE * 2.0f)); //HACK differentiate by hashcode

            return -difference;

        }


    }
}



     
     
     

























