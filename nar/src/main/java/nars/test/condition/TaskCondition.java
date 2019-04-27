package nars.test.condition;


import jcog.Texts;
import jcog.data.list.FasterList;
import nars.NAL;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.task.Tasked;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.NEG;

public class TaskCondition implements NARCondition, Predicate<Task>, Consumer<Tasked> {


    protected final NAL<NAL<NAR>> NAL;
    private final byte punc;


    private final Term term;

    /**
     * whether to apply meta-feedback to drive the reasoner toward success conditions
     */
    public static final boolean feedback = false;
    private final LongLongPredicate time;

    private boolean succeeded;


    private final float freqMin;
    private final float freqMax;
    public final float confMin;
    private final float confMax;
    private long creationStart, creationEnd;

    /*float tenseCost = 0.35f;
    float temporalityCost = 0.75f;*/

    private final static int maxSimilars = 2;

    public final List<Task> matched = new FasterList(0);

    @Deprecated protected final TreeMap<Float, Task> similar = new TreeMap();


    public TaskCondition(NAL<NAL<NAR>> n, long creationStart, long creationEnd, Term term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time) throws RuntimeException {


        if (freqMax < freqMin) throw new RuntimeException("freqMax < freqMin");
        if (confMax < confMin) throw new RuntimeException("confMax < confMin");

        if (creationEnd - creationStart < 1)
            throw new RuntimeException("cycleEnd must be after cycleStart by at least 1 cycle");

        this.NAL = n;
        this.time = time;

        this.creationStart = creationStart;
        this.creationEnd = creationEnd;

        this.confMax = Math.min(1.0f, confMax);
        this.confMin = Math.max(0.0f, confMin);
        this.punc = punc;



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

            dist += 0.2f;
            if (dist >= ifLessThan) return dist;
        }

        if (a.subs() != b.subs()) {
            dist += 0.2f;
            if (dist >= ifLessThan) return dist;
        }

        if (a.structure() != b.structure()) {
            dist += 0.2f;
            if (dist >= ifLessThan) return dist;
        }


        dist += Texts.levenshteinFraction(
                a.toString(),
                b.toString()) * 0.4f;

        if (a.dt() != b.dt()) {
            dist += 0.2f;
        }

        return dist;
    }

    @Override
    public String toString() {
        return term.toString() + ((char) punc) + " %" +
                rangeStringN2(freqMin, freqMax) + ';' + rangeStringN2(confMin, confMax) + '%' + ' ' +
                " creation: (" + creationStart + ',' + creationEnd + ')';
    }


    public boolean matches(@Nullable Task task) {
        if (task == null)
            return false;

        Term tt = task.term();
        if (!tt.equals(this.term)) {
            if (tt.op() == term.op() && tt.volume() == this.term.volume() && tt.structure() == this.term.structure() && this.term.toString().equals(tt.toString())) {
                throw new RuntimeException("target construction problem: " + this.term + " .toString() is equal to " + tt + " but inequal otherwise");
            }
            return false;
        }

        if (task.punc() != punc)
            return false;

        if (!truthMatches(task))
            return false;

        return creationTimeMatches() && occurrenceTimeMatches(task);
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


    private final boolean creationTimeMatches() {
        long now = NAL.time();
        return (((creationStart == -1) || (now >= creationStart)) &&
                ((creationEnd == -1) || (now <= creationEnd)));
    }

    private boolean occurrenceTimeMatches(Task t) {
        return time.accept(t.start(), t.end());
    }

    @Override
    public final boolean test(Task t) {

        if (matches(t)) {
            matched.add(t);

            succeeded = true;


            return true;
        } else {
            recordSimilar(t);
            return false;
        }
    }

    private void recordSimilar(Task task) {

        final TreeMap<Float, Task> similar = this.similar;



        int sims = similar.size();
        float worstDiff = sims >= maxSimilars ? similar.lastKey() : Float.POSITIVE_INFINITY;

        float difference = 0;
        if (task.punc() != punc)
            difference += 4;
        if (difference >= worstDiff)
            return;

        Term tterm = task.term();
        difference +=
                3 * termDistance(tterm, term, worstDiff);
        if (difference >= worstDiff)
            return;

        if (task.isBeliefOrGoal()) {
            float f = task.freq();
            float freqDiff = Math.min(
                    Math.abs(f - freqMin),
                    Math.abs(f - freqMax));
            difference += 2 * freqDiff;
            if (difference >= worstDiff)
                return;

            float c = task.conf();
            float confDiff = Math.min(
                    Math.abs(c - confMin),
                    Math.abs(c - confMax));
            difference += 1 * confDiff;
            if (difference >= worstDiff)
                return;
        }

//        if (difference >= worstDiff)
//            return;

        difference += 0.00001f * ((float)Math.abs(task.hashCode()) / (Integer.MAX_VALUE*2)); //HACK differentiate by hashcode
        if (similar.put(difference, task)==null) {
            if (sims+1 > maxSimilars) {
                similar.remove(similar.lastEntry().getKey());
            }
        }

    }


    @Override
    public final void accept(Tasked tasked) {
        if (succeeded) return;
        test(tasked.task());
    }


    @Override
    public long getFinalCycle() {
        return creationEnd;
    }


    @Override
    public final boolean isTrue() {
        return succeeded;
    }


    @Override
    public void log(Logger logger) {
        String msg = succeeded ? " OK" : "ERR" + '\t' + this;
        if (succeeded) {
            logger.info(msg);

            if (matched != null && logger.isTraceEnabled()) {
                matched.forEach(s -> logger.trace("\t{}", s));
            }
        } else {
            assert (matched.isEmpty());

            logger.error(msg);


            if (!similar.isEmpty()) {
                similar.values().forEach(s -> {
                    String pattern = "SIM\n{}";
                    logger.info(pattern, s.proof());

                });
            }

        }


    }
}



     
     
     

























