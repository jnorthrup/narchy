package nars;

import jcog.Util;
import jcog.math.FloatParam;
import jcog.math.FloatParamRounded;
import jcog.math.MutableInteger;
import jcog.pri.op.PriMerge;
import jcog.util.FloatFloatToFloatFunction;
import nars.control.Derivation;
import nars.task.Tasked;
import nars.task.TruthPolation;
import nars.term.atom.Atom;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import static jcog.Util.unitize;
import static nars.Op.*;
import static nars.control.MetaGoal.*;

/**
 * NAR Parameters
 */
public abstract class Param {


    /**
     * must be big enough to support as many layers of compound terms as exist in an eval
     */
    public static final int MAX_EVAL_RECURSION = 32;

    /**
     * rate that integers in integer-containing termlink compounds will be dynamically mutated on activation
     */
    public static final float MUTATE_INT_CONTAINING_TERMS_RATE = 0.25f;

    /**
     * allow leaking of internal Term[] arrays for read-only purposes
     */
    public static final boolean TERM_ARRAY_SHARE = true;

    /**
     * min ratio of effective priority to input priority necessary for certain novel-only actions
     */
    public static final float ACTIVATION_THRESHOLD = 0.1f;

    public static final boolean ETERNALIZE_EVICTED_TEMPORAL_TASKS = true;



    public static final boolean FILTER_DYNAMIC_MATCHES = true;

    /** experimental increased confidence calculation for composition, taken from the NAL spec but is different from OpenNARS */
    public static boolean STRONG_COMPOSITION = false;

    //    private final static Logger logger = LoggerFactory.getLogger(DeriveTime.class);

    public static final int TEMPORAL_SOLVER_ITERATIONS = 24;


    public static boolean DEBUG_FILTER_DUPLICATE_MATCHES = Param.DEBUG_EXTRA;


    public final FloatParam forgetRate = new FloatParam(0.75f, 0f, 2f);

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

    /**
     * how many INT terms are canonically interned/cached. [0..n)
     */
    public final static int MAX_INTERNED_INTS = 64;


    public static final boolean FILTER_SIMILAR_DERIVATIONS = true;
    public static final boolean DEBUG_SIMILAR_DERIVATIONS = false;


    /**
     * use this for advanced error checking, at the expense of lower performance.
     * it is enabled for unit tests automatically regardless of the value here.
     */
    public static boolean DEBUG;
    public static boolean DEBUG_EXTRA;


    //Budget Merging: the sequence listed here is important

    public static final PriMerge activateMerge = PriMerge.plus;

    public static final PriMerge termlinkMerge =
            PriMerge.max;
            //PriMerge.plus;

    public static final PriMerge tasklinkMerge =
            PriMerge.max;
    //PriMerge.plus; //not safe to plus without enough headroom

    //    /**
//     * budgets premises from their links, but isolated from affecting the derivation budgets, which are from the tasks (and not the links)
//     */
    public static final FloatFloatToFloatFunction taskTermLinksToPremise =
            Util::or;
    //Util::and;
//            //UtilityFunctions::aveGeo;
//            //UtilityFunctions::aveAri;
//            //Math::min;
//            //Math::max;



    /**
     * max budget for derivations from the task and optional belief budget
     */
    public static final FloatFloatToFloatFunction TaskBeliefDerivation =
            //Math::max;
            Util::or;
    //Util::and;
    //UtilityFunctions::aveAri;


    public static final PriMerge taskMerge = PriMerge.max;


    /**
     * maximum time (in durations) that a signal task can latch its last value before it becomes unknown
     */
    public final static int SIGNAL_LATCH_TIME_MAX =
            //0;
            8;
            //Integer.MAX_VALUE;
            //8;
            //32;

    /** happiness automatic gain control time parameter */
    public final static float HAPPINESS_RE_SENSITIZATION_RATE = 0.001f;

    /**
     * 'time to live', unification steps until unification is stopped
     */
    public final MutableInteger matchTTLmean = new MutableInteger(64);

    /**
     * how much percent of a premise's allocated TTL can be used in the belief matching phase.
     */
    public static final float BELIEF_MATCH_TTL_FRACTION = 0.1f;

    public static final int TTL_MIN =
            Param.TTL_UNIFY * 2 +
                    Param.TTL_DERIVE_TASK_SUCCESS;

    /**
     * cost of attempting a unification
     */
    public static final int TTL_UNIFY = 1;

    /**
     * cost of executing a termute permutation
     */
    public static final int TTL_MUTATE = 1;

    /**
     * cost of a successful task derivation
     */
    public static final int TTL_DERIVE_TASK_SUCCESS = 4;

    /**
     * cost of a repeat (of another within the premise's batch) task derivation
     */
    public static final int TTL_DERIVE_TASK_REPEAT = TTL_DERIVE_TASK_SUCCESS * 2;

    /**
     * cost of a task derived, but too similar to one of its parents
     */
    public static final int TTL_DERIVE_TASK_SAME = 2;

    /**
     * cost of a failed/aborted task derivation
     */
    public static final int TTL_DERIVE_TASK_FAIL = 1;

    //    /**
//     * number between 0 and 1 controlling the proportion of activation going
//     * forward (compound to subterms) vs. reverse (subterms to parent compound).
//     * when calculated, the total activation will sum to 1.0.
//     * so 0.5 is equal amounts for both, 0 is full backward, 1 is full forward.
//     */
    public final FloatParam termlinkBalance = new FloatParam(0.5f, 0, 1f);

    public final FloatParam conceptActivation = new FloatParam(1f, 0, 1f);

    public final float[] want = newWants();

    protected void defaultWants() {
        float[] w = this.want;

        //follows the pos/neg guidelines described in the comment of each MetaGoal
        Perceive.set(w, -0.005f);
        Believe.set(w, 0.01f);
        Desire.set(w, 0.1f);
        Accurate.set(w, 0.1f);
        Answer.set(w, 0.1f);
        Action.set(w, 1f);
    }

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


    public static float derivationPriority(Task t, Derivation d) {

        float discount = 1f;

        //t.volume();

        {
            //relative growth compared to parent complexity
//        int pCompl = d.parentComplexity;
//        float relGrowth =
//                unitize(((float) pCompl) / (pCompl + dCompl));
//        discount *= (relGrowth);
        }

        {
            //absolute size relative to limit
            //float p = 1f / (1f + ((float)t.complexity())/termVolumeMax.floatValue());
        }

        Truth derivedTruth = t.truth();
        {

            float dCompl = t.voluplexity();
            //float increase = (dCompl-d.parentComplexityMax);
            //if (increase > Pri.EPSILON) {
            int penalty = 1;
            float change = penalty + Math.abs(dCompl-d.parentComplexityMax); //absolute change: penalize drastic complexification or simplification, relative to parent task(s) complexity

                //relative increase in complexity
                //calculate the increases proportion to the "headroom" remaining for term expansion
                //ie. as the complexity progressively grows toward the limit, the discount accelerates
                float complexityHeadroom = Math.max(1, d.termVolMax - d.parentComplexityMax);
                float headroomConsumed = Util.unitize(change /* increase */ / complexityHeadroom );
                float headroomRemain = 1f - headroomConsumed;

                //note: applies more severe discount for questions/quest since the truth deduction can not apply
                discount *= (derivedTruth != null) ? headroomRemain : Util.sqr(headroomRemain);

        }


        if (/* belief or goal */ derivedTruth != null) {

            //loss of relative confidence: prefer confidence, relative to the premise which formed it
            float parentEvi = d.single ? d.premiseEviSingle : d.premiseEviDouble;
            if (parentEvi > 0) {
                discount *= unitize(derivedTruth.evi() / parentEvi );
            }

            //optional: prefer polarized
            //c *= (1f + p * (0.5f - Math.abs(t.freq()-0.5f)));
        }

        return discount * d.pri;

        //return Util.lerp(1f-t.originality(),discount, 1) * d.premisePri; //more lenient derivation budgeting priority reduction in proportion to lack of originality
    }


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
     * how many answers to record per input question task (in its concept's answer bag)
     */
    public static final int MAX_INPUT_ANSWERS = 8;

    /**
     * max retries for termpolation to produce a valid task content result during revision
     */
    public static final int MAX_TERMPOLATE_RETRIES = 1;


//    /** determines if an input goal or command operation task executes */
//    public static float EXECUTION_THRESHOLD = 0.666f;

    public static boolean ANSWER_REPORTING = true;


//    public static final boolean DERIVATION_TRANSFORM_CACHE = false;
//
//    /** -1 for softref */
//    public static final int DERIVATION_TRANSFORM_CACHE_SIZE_PER_THREAD =
//            //-1; //softref
//            32 * 1024;

    /**
     * hard upper-bound limit on Compound term complexity;
     * if this is exceeded it may indicate a recursively
     * malformed term due to a serious inference bug
     */
    public final MutableInteger termVolumeMax = new MutableInteger(COMPOUND_VOLUME_MAX);

    //public static final boolean ARITHMETIC_INDUCTION = false;


//    /** whether derivation's concepts are cross-termlink'ed with the premise concept */
//    public static boolean DERIVATION_TERMLINKED;
//    public static boolean DERIVATION_TASKLINKED;


    //    //TODO use 'I' for SELf, it is 3 characters shorter
//    public static final Atom DEFAULT_SELF = (Atom) $.the("I");
    static Atom randomSelf() {
        return (Atom) $.quote("I_" + Util.uuid64());
    }


    /**
     * Evidential Horizon, the amount of future evidence to be considered
     * Must be >=1.0
     */
    public static final float HORIZON = 1f;

    public static final int MAX_VARIABLE_CACHED_PER_TYPE = 16;
    /**
     * Maximum length of the evidental base of the Stamp, a power of 2
     */
    public static final int STAMP_CAPACITY = 16;

    public static final FloatParam causeCapacity = new FloatParam(32, 0, 128);

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
    public final FloatParam confMin = new FloatParam(TRUTH_EPSILON, TRUTH_EPSILON, 1f);

    /**
     * global truth frequency resolution by which reasoning is dithered
     */
    public final FloatParam freqResolution = new FloatParamRounded(TRUTH_EPSILON, TRUTH_EPSILON, 1f, TRUTH_EPSILON);

    /**
     * global truth confidence resolution by which reasoning is dithered
     */
    public final FloatParam confResolution = new FloatParamRounded(TRUTH_EPSILON, TRUTH_EPSILON, 1f, TRUTH_EPSILON) {
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
    public final FloatParam momentum = new FloatParam(0.5f, 0, 1f);

    /**
     * computes the projected evidence at a specific distance (dt) from a perceptual moment evidence
     * with a perceptual duration used as a time constant
     * dt >= 0
     */
    public static double evi(float evi, long dt, long dur) {

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
                new TruthPolation.TruthPolationBasic(start, end, dur, temporals, topEternal!=null);
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
                float temporalDensity = ((float)totalCovered)/Math.max(1, end-start);
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
