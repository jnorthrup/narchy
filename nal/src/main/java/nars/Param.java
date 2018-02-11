package nars;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatRangeRounded;
import jcog.math.IntRange;
import jcog.math.Range;
import jcog.pri.Pri;
import jcog.pri.op.PriMerge;
import jcog.util.FloatFloatToFloatFunction;
import nars.task.Tasked;
import nars.task.TruthPolation;
import nars.term.atom.Atom;
import nars.truth.PreciseTruth;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.Op.*;
import static nars.control.MetaGoal.newWants;

/**
 * NAR Parameters
 */
public abstract class Param {


    /**
     * must be big enough to support as many layers of compound terms as exist in an eval
     */
    public static final int MAX_EVAL_RECURSION = 32;

//    /**
//     * rate that integers in integer-containing termlink compounds will be dynamically mutated on activation
//     */
//    public static final float MUTATE_INT_CONTAINING_TERMS_RATE = 0.25f;

    /**
     * allow leaking of internal Term[] arrays for read-only purposes
     */
    public static final boolean TERM_ARRAY_SHARE = true;

    /**
     * pri threshold for emitting task activation events
     */
    @Range(min=0, max=0.5f)
    public static float TASK_ACTIVATION_THRESHOLD = Pri.EPSILON*2;

    public static final boolean ETERNALIZE_EVICTED_TEMPORAL_TASKS = false;


    public static final boolean FILTER_DYNAMIC_MATCHES = true;

    /**
     * experimental increased confidence calculation for composition, taken from the NAL spec but is different from OpenNARS
     */
    public static final boolean STRONG_COMPOSITION = false;



    @Range(min=1, max=32)
    public static int TEMPORAL_SOLVER_ITERATIONS = 16;


    public static final boolean DEBUG_FILTER_DUPLICATE_MATCHES = false;


    /** default bag forget rate */
    public final FloatRange forgetRate = new FloatRange(1f, 0f, 2f);

    /**
     * hard limit to prevent infinite looping
     */
    public static final int MAX_TASK_FORWARD_HOPS = 4;

    /**
     * controls interpolation policy:
     * true: dt values will be interpolated
     * false: dt values will be chosen by weighted random decision
     */
    public final AtomicBoolean dtMergeOrChoose = new AtomicBoolean(false);


    public static final boolean FILTER_SIMILAR_DERIVATIONS = true;
    public static final boolean DEBUG_SIMILAR_DERIVATIONS = false;


    /**
     * use this for advanced error checking, at the expense of lower performance.
     * it is enabled for unit tests automatically regardless of the value here.
     */
    public static boolean DEBUG;
    public static final boolean DEBUG_EXTRA = false;


    //Budget Merging: the sequence listed here is significant

    public static final PriMerge activateMerge = PriMerge.plus;

    public static final PriMerge termlinkMerge =
            //PriMerge.max;
            PriMerge.plus;

    public static final PriMerge tasklinkMerge =
            PriMerge.max;
            //PriMerge.plus;

    //    /**
//     * budgets premises from their links, but isolated from affecting the derivation budgets, which are from the tasks (and not the links)
//     */
    public static final FloatFloatToFloatFunction taskTermLinksToPremise =
            //Util::or;
    Util::and;
//            //UtilityFunctions::aveGeo;
//            //UtilityFunctions::aveAri;
//            //Math::min;
//            //Math::max;


    /**
     * max budget for derivations from the task and optional belief budget
     */
    public static final FloatFloatToFloatFunction TaskBeliefToDerivation =
            Util::and;
    //        Util::or;
    //UtilityFunctions::aveAri;
    //Math::max;


    public static final PriMerge taskMerge = PriMerge.max;


    /**
     * maximum time (in durations) that a signal task can latch its last value before it becomes unknown
     */
    public final static int SIGNAL_LATCH_TIME_MAX =
            //0;
            //2;
            4;
            //8;
            //16;
            //Integer.MAX_VALUE;


    /**
     * NAgent happiness automatic gain control time parameter
     */
    public final static float HAPPINESS_RE_SENSITIZATION_RATE = 0.001f;

    /**
     * 'time to live', unification steps until unification is stopped
     */
    public final IntRange matchTTLmean = new IntRange(620, 0, 1024);

    public static final int TTL_MIN() {
            return Param.TTL_UNIFY * 2 +
                    Param.TTL_DERIVE_TASK_SUCCESS; }

    /**
     * cost of attempting a unification
     */
    @Range(min=0, max=64)
    public static int TTL_UNIFY = 2;

    /**
     * cost of executing a termute permutation
     */
    @Range(min=0, max=64)
    public static int TTL_MUTATE = 2;

    /**
     * cost of a successful task derivation
     */
    @Range(min=0, max=64)
    public static int TTL_DERIVE_TASK_SUCCESS = 37;

    /**
     * cost of a repeat (of another within the premise's batch) task derivation
     */
    @Range(min=0, max=64)
    public static int TTL_DERIVE_TASK_REPEAT = 42;

    /**
     * cost of a task derived, but too similar to one of its parents
     */
    @Range(min=0, max=64)
    public static int TTL_DERIVE_TASK_SAME = 30;

    /**
     * cost of a failed/aborted task derivation
     */
    @Range(min=0, max=64)
    public static int TTL_DERIVE_TASK_FAIL = 28;

    //    /**
//     * number between 0 and 1 controlling the proportion of activation going
//     * forward (compound to subterms) vs. reverse (subterms to parent compound).
//     * when calculated, the total activation will sum to 1.0.
//     * so 0.5 is equal amounts for both, 0 is full backward, 1 is full forward.
//     */
    public final FloatRange termlinkBalance = new FloatRange(0.5f, 0, 1f);

    public final FloatRange conceptActivation = new FloatRange(1f, 0, 1f);

    public final float[] want = newWants();

//    protected void defaultWants() {
//        float[] w = this.want;
//
//        //follows the pos/neg guidelines described in the comment of each MetaGoal
//        Perceive.set(w, -0.005f);
//        Believe.set(w, 0.01f);
//        Desire.set(w, 0.1f);
//        Accurate.set(w, 0.1f);
//        Answer.set(w, 0.1f);
//        Action.set(w, 1f);
//    }

    /**
     * how many durations above which to dither dt relations to dt=0 (parallel)
     * set to zero to disable dithering.  typically the value will be 0..1.0.
     * TODO move this to Time class and cache the cycle value rather than dynamically computing it
     */
    private final MutableFloat dtDither = new MutableFloat(0.5f);

    public void dtDither(float durations) {
        dtDither.set(durations);
    }

    public int dtDitherCycles() {
        return Math.max(1, Math.round(dtDither.floatValue() * dur()));
    }

    abstract int dur();


    /**
     * abs(term.dt()) safety limit for non-dternal/non-xternal temporal compounds
     */
    @Deprecated
    public static int DT_ABS_LIMIT =
            Integer.MAX_VALUE / 1024;
    //Integer.MAX_VALUE / 16384;




    /**
     * absolute limit for constructing terms in any context in which a NAR is not known, which could provide a limit.
     * typically a NAR instance's 'compoundVolumeMax' parameter will be lower than this
     */
    public static final int COMPOUND_VOLUME_MAX = 127;

    /**
     * limited because some subterm paths are stored as byte[]. to be safe, use 7-bits
     */
    public static final int COMPOUND_SUBTERMS_MAX = 127;

    /**
     * how many answers to record per input question task (per each concept's answer bag)
     */
    public static final int ANSWER_BAG_CAPACITY = 8;

    /**
     * max retries for termpolation to produce a valid task content result during revision
     */
    public static final int MAX_TERMPOLATE_RETRIES = 1;

    public static final boolean DEBUG_REPORT_ANSWERS = false;


    /**
     * hard upper-bound limit on Compound term complexity;
     * if this is exceeded it may indicate a recursively
     * malformed term due to a serious inference bug
     */
    public final IntRange termVolumeMax = new IntRange(COMPOUND_VOLUME_MAX, 0, COMPOUND_VOLUME_MAX);


    static Atom randomSelf() {
        return (Atom) $.quote("I_" + Util.uuid64());
    }

    /**
     * Evidential Horizon, the amount of future evidence to be considered
     * Must be >=1.0
     */
    public static final float HORIZON = 1f;



    public static final int MAX_INTERNED_VARS = 16;

    /**
     * how many INT terms are canonically interned/cached. [0..n)
     */
    public final static int MAX_INTERNED_INTS = 64;


    /**
     * Maximum length of the evidental base of the Stamp, a power of 2
     * TODO IntRange
     */
    public static final int STAMP_CAPACITY = 16;

    public static final IntRange causeCapacity = new IntRange(64, 0, 128);

    /**
     * hard limit for cause capacity in case the runtime parameter otherwise disobeyed
     */
    public static final int CAUSE_LIMIT = (int) (causeCapacity.max * 2);


    public final static int UnificationStackMax = 64; //how many assignments can be stored in the 'versioning' maps

//    /** estimate initial capacity for variable unification maps */
//    public static final int UnificationVariableCapInitial = 8;


    //public static final boolean DEBUG_BAG_MASS = false;
    //public static boolean DEBUG_TRACE_EVENTS = false; //shows all emitted events
    //public static boolean DEBUG_DERIVATION_STACKTRACES; //includes stack trace in task's derivation rule string
    //public static boolean DEBUG_INVALID_SENTENCES = true;
    //public static boolean DEBUG_NONETERNAL_QUESTIONS = false;
    public static final boolean DEBUG_TASK_LOG = true; //false disables task history completely


    private float defaultGoalConf, defaultBeliefConf;


    /**
     * internal granularity which truth components are rounded to
     */
    public static final float TRUTH_EPSILON = 0.01f;

    /**
     * how precise unit test results must match expected values to pass
     */
    public static final float TESTS_TRUTH_ERROR_TOLERANCE = TRUTH_EPSILON;


//    /** EXPERIMENTAL  decreasing priority of sibling tasks on temporal task insertion */
//    public static final boolean SIBLING_TEMPORAL_TASK_FEEDBACK = false;

//    /** EXPERIMENTAL enable/disable dynamic tasklink truth revision */
//    public static final boolean ACTION_CONCEPT_LINK_TRUTH = false;


//    /** derivation confidence (by evidence) multiplier.  normal=1.0, <1.0= under-confident, >1.0=over-confident */
//    @NotNull public final FloatParam derivedEvidenceGain = new FloatParam(1f, 0f, 4f);


    /**
     * truth confidence threshold necessary to form tasks
     */
    public final FloatRange confMin = new FloatRange(TRUTH_EPSILON, TRUTH_EPSILON, 1f);

    /**
     * global truth frequency resolution by which reasoning is dithered
     */
    public final FloatRange freqResolution = new FloatRangeRounded(TRUTH_EPSILON, TRUTH_EPSILON, 1f, TRUTH_EPSILON);

    /**
     * global truth confidence resolution by which reasoning is dithered
     */
    public final FloatRange confResolution = new FloatRangeRounded(TRUTH_EPSILON, TRUTH_EPSILON, 1f, TRUTH_EPSILON) {
        @Override
        public void set(float value) {
            super.set(value);
            value = get(); //update for rounding
            if (confMin.floatValue() < value)
                confMin.set(value);
        }
    };


    /**
     * controls the speed (0..+1.0) of budget propagating from compound
     * terms to their subterms
     * 0 momentum means an activation is fired completely and suddenly
     * 1 momentum means it retains all activation
     */
    public final FloatRange momentum = new FloatRange(0.5f, 0, 1f);


    /**
     * tolerance of complexity
     * low values (~0) will penalize complexity in derivations maximally (preferring simplicity)
     * high values (~1) will penalize complexity in deriations minimally (allowing complexity)
     */
    public final FloatRange deep = new FloatRange(0.5f, 0, 1f);

    /**
     * computes the projected evidence at a specific distance (dt) from a perceptual moment evidence
     * with a perceptual duration used as a time constant
     * dt >= 0
     */
    public static double evi(double evi, long dt, long dur) {

        return evi / (1.0 + (((double) dt) / dur)); //inverse linear

        //double ddt = dt;
        //return (float) (evi / (1.0 + ddt * ddt / dur)); //inverse square

        //return evi / Util.sqr( 1f + dt / dur ); //inverse square suck

        //hard linear with half duration on either side of the task -> sum to 1.0 duration
//        float scale = dt / dur;
//        if (scale > 0.5f) return 0;
//        else return evi * (1f - scale*2f);


        //return evi / (1 + ((float) Math.log(1+dt/dur))); //inverse log

        //return evi / (1 + (((float) Math.log(1+dt)) / dur)); //inverse log


        //return evi /( 1 + 2 * (dt/dur) ); //inverse linear * 2 (nyquist recovery period)


        //return evi / (1f + dt / dur ); //first order decay
        //return evi / (1f + (dt*dt) / (dur*dur) ); //2nd order decay

    }

    @Nullable
    public static PreciseTruth truth(@Nullable Task topEternal, long start, long end, int dur, Iterable<? extends Tasked> temporals) {

        assert (dur > 0);

        TruthPolation.TruthPolationBasic t =
                //new TruthPolation.TruthPolationBasic(start, end, dur);
                new TruthPolation.TruthPolationBasic(start, end, dur, temporals, topEternal != null);
        //new TruthPolation.TruthPolationConf(start, end, dur);
        //new TruthPolation.TruthPolationConf(start, end, dur);
        //new TruthPolation.TruthPolationGreedy(start, end, dur, ThreadLocalRandom.current());
        //..SoftMax..
        //new TruthPolation.TruthPolationRoulette(start, end, dur, ThreadLocalRandom.current());
        //new TruthPolationWithVariance(when, dur);


        // Contribution of each task's truth
        // use forEach instance of the iterator(), since HijackBag forEach should be cheaper
        temporals.forEach(t);

        float tempEvi = t.eviSum;
        boolean someEvi = tempEvi > 0f;
        if (topEternal != null) {
            if (!someEvi) {
                return new PreciseTruth(topEternal.truth()); //eternal the only authority
            } else {

                //long totalSpan = Math.max(1, t.spanEnd - t.spanStart);
                long totalCovered = Math.max(1, t.rangeSum); //estimate
                float temporalDensity = ((float) totalCovered) / Math.max(1, end - start);
                float eviDecay = 1 / ((1 + tempEvi * temporalDensity));

                float eteEvi = topEternal.evi();

                t.accept(topEternal.freq(), eteEvi * eviDecay);
            }
        }

        return !someEvi ? null : t.truth();
    }


    public final float confDefault(byte punctuation) {

        switch (punctuation) {
            case BELIEF:
                return defaultBeliefConf;

            case GOAL:
                return defaultGoalConf;

            default:
                throw new RuntimeException("Invalid punctuation " + punctuation + " for a TruthValue");
        }
    }


//    /** no term sharing means faster comparison but potentially more memory usage. TODO determine effects */
//    public static boolean CompoundDT_TermSharing;


    /**
     * Default priority of input judgment
     */
    public float DEFAULT_BELIEF_PRIORITY = 0.5f;

    /**
     * Default priority of input question
     */
    public float DEFAULT_QUESTION_PRIORITY = 0.5f;


    /**
     * Default priority of input judgment
     */
    public float DEFAULT_GOAL_PRIORITY = 0.5f;

    /**
     * Default priority of input question
     */
    public float DEFAULT_QUEST_PRIORITY = 0.5f;


    public float priDefault(byte punctuation) {
        switch (punctuation) {
            case BELIEF:
                return DEFAULT_BELIEF_PRIORITY;

            case QUEST:
                return DEFAULT_QUEST_PRIORITY;

            case QUESTION:
                return DEFAULT_QUESTION_PRIORITY;

            case GOAL:
                return DEFAULT_GOAL_PRIORITY;

            case COMMAND:
                return 0;
        }
        throw new RuntimeException("Unknown punctuation: " + punctuation);
    }

    public void priDefault(byte punctuation, float pri) {
        switch (punctuation) {
            case BELIEF:
                DEFAULT_BELIEF_PRIORITY = pri;
                break;

            case QUEST:
                DEFAULT_QUEST_PRIORITY = pri;
                break;

            case QUESTION:
                DEFAULT_QUESTION_PRIORITY = pri;
                break;

            case GOAL:
                DEFAULT_GOAL_PRIORITY = pri;
                break;

            default:
                throw new RuntimeException("Unknown punctuation: " + punctuation);

        }
    }


    Param() {

        beliefConfidence(0.9f);
        goalConfidence(0.9f);
    }

    /**
     * sets the default input goal confidence
     */
    public void goalConfidence(float c) {
        defaultGoalConf = c;
    }

    /**
     * sets the default input belief confidence
     */
    public void beliefConfidence(float c) {
        defaultBeliefConf = c;
    }


    public static float overlapEvidence(float evi, float overlap) {
        return
                overlap == 0 ? evi : 0; //prevents any overlap
        //evi * Util.sqr(1f-Util.unitize(overlap));
        //evi * (1f-overlap);
    }


}
