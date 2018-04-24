package nars;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatRangeRounded;
import jcog.math.IntRange;
import jcog.math.Range;
import jcog.pri.op.PriMerge;
import jcog.util.FloatFloatToFloatFunction;
import nars.concept.util.ConceptBuilder;
import nars.concept.util.DefaultConceptBuilder;
import nars.term.atom.Atom;
import nars.truth.polation.FocusingLinearTruthPolation;
import nars.truth.polation.TruthPolation;
import nars.util.time.Tense;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.Op.*;
import static nars.util.time.Tense.ETERNAL;
import static nars.util.time.Tense.XTERNAL;

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

    /** softmax roulette parameter in trie deriver fork decisions */
    public static final float TRIE_DERIVER_TEMPERATURE = 0.5f;


//    public static final boolean ETERNALIZE_EVICTED_TEMPORAL_TASKS = false;


    public static final boolean FILTER_DYNAMIC_MATCHES = true;

    /**
     * experimental increased confidence calculation for composition, taken from the NAL spec but is different from OpenNARS
     */
    public static final boolean STRONG_COMPOSITION = false;

    /** extends the time all unit tests are allowed to run for.
     *  normally be kept to 1 but for debugging this may be increased to find what tests need more time */
    public static float TEST_TIME_MULTIPLIER = 4;


    @Range(min=1, max=32)
    public static int TEMPORAL_SOLVER_ITERATIONS = 6;

    /** default bag forget rate */
    public final FloatRange forgetRate = new FloatRange(1f, 0f, 2f);

//    /**
//     * hard limit to prevent infinite looping
//     */
//    public static final int MAX_TASK_FORWARD_HOPS = 4;


    public ConceptBuilder conceptBuilder = new DefaultConceptBuilder();

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

    public static final PriMerge activateMerge =
            PriMerge.plus;
            //PriMerge.max;

    public static final PriMerge termlinkMerge =
            //PriMerge.max;
            PriMerge.plus;

    public static final PriMerge tasklinkMerge =
            PriMerge.max;
            //PriMerge.plus;

    /**
     * budget factor for double-premise derivations: depends on the task and belief budget
     */
    public static final FloatFloatToFloatFunction TaskBeliefToDerivation =
            //Util::and;
            //Util::or;
            (t,b)->(t+b); //sum
            //Util::mean;
            //Math::max;

    /**
     * budget factor for single-premise derivations: depends only on the task budget
     */
    public static final FloatToFloatFunction TaskToDerivation = (t) -> t;

    public static final PriMerge taskMerge = PriMerge.max;


    /**
     * maximum time (in durations) that a signal task can latch its last value before it becomes unknown
     */
    public final static int SIGNAL_LATCH_TIME_MAX =
            //0;
            //2;
            //4;
            8;
            //16;
            //Integer.MAX_VALUE;


    /**
     * NAgent happiness automatic gain control time parameter
     * TODO verify this is applied based on time, not iterations
     */
    public final static float HAPPINESS_RE_SENSITIZATION_RATE = 0.0001f;
    public final static float HAPPINESS_RE_SENSITIZATION_RATE_FAST = 0.0002f;



    /** temporal radius (in durations) around the present moment to scan for truth */
    public final FloatRange timeFocus = new FloatRange(0.5f, 0, 10);

    /** creates instance of the default truthpolation implementation */
    public static TruthPolation truth(long start, long end, int dur) {
        return new FocusingLinearTruthPolation(start, end, dur);
    }

    /** provides a start,end pair of time points for the current focus given the current time and duration */
    public final long[] timeFocus() {
        return timeFocus(time());
    }

    public final long[] timeFocus(long when) {
        return timeFocus(when, dur());
    }

    public final long[] timeFocus(long when, float dur) {
        if (when == ETERNAL)
            return Tense.ETERNAL_ETERNAL;

        if (when == XTERNAL) {
            throw new RuntimeException();
        }

        int f = Math.round(dur * timeFocus.floatValue());
        int ditherCycles = dtDitherCycles();
        long from = Tense.dither(when - f, ditherCycles);
        long to = Tense.dither(when + f, ditherCycles);;
        return new long[] {from, to};
    }

    /**
     * 'time to live', unification steps until unification is stopped
     */
    public final IntRange deriveTTL = new IntRange(32, 0, 1024);


    /** estimate */
    public static final int TTL_MIN =
            Param.TTL_UNIFY * 2 +
                    Param.TTL_DERIVE_TASK_SUCCESS + (Param.TTL_BRANCH * 2);

    /**
     * cost of attempting a unification
     */
    @Range(min=0, max=64)
    public static int TTL_UNIFY = 1;

    @Range(min=0, max=64)
    public static final int TTL_BRANCH = 1;

    /**
     * cost of executing a termute permutation
     */
    @Range(min=0, max=64)
    public static int TTL_MUTATE = 1;

    /**
     * cost of a successful task derivation
     */
    @Range(min=0, max=64)
    public static int TTL_DERIVE_TASK_SUCCESS = 4;

    /**
     * cost of a repeat (of another within the premise's batch) task derivation
     */
    @Range(min=0, max=64)
    public static int TTL_DERIVE_TASK_REPEAT = 5;

    @Range(min=0, max=64)
    public static int TTL_DERIVE_TASK_UNPRIORITIZABLE = 5;

    /**
     * cost of a task derived, but too similar to one of its parents
     */
    @Range(min=0, max=64)
    public static int TTL_DERIVE_TASK_SAME = 5;

    /** cost of having insufficient evidence (according to NAR's confMin param) to derive task */
    @Range(min=0, max=64)
    public static int TTL_EVI_INSUFFICIENT = 3;

    /**
     * cost of a failed/aborted task derivation
     */
    @Range(min=0, max=64)
    public static int TTL_DERIVE_TASK_FAIL = 5;

    //    /**
//     * number between 0 and 1 controlling the proportion of activation going
//     * forward (compound to subterms) vs. reverse (subterms to parent compound).
//     * when calculated, the total activation will sum to 1.0.
//     * so 0.5 is equal amounts for both, 0 is full backward, 1 is full forward.
//     */
    public final FloatRange termlinkBalance = new FloatRange(0.5f, 0, 1f);

    //public final FloatRange taskLinkMomentum = new FloatRange(0.25f, 0, 1f);

    public final FloatRange activationRate = new FloatRange(1f, 0, 1f);


    /**
     * how many durations above which to dither dt relations to dt=0 (parallel)
     * set to zero to disable dithering.  typically the value will be 0..1.0.
     * TODO move this to Time class and cache the cycle value rather than dynamically computing it
     */
    public final FloatRange dtDither = new FloatRange(0f, 0f, 8f);

    public int dtDitherCycles() {
        float dd = dtDither.floatValue();
        return dd > 0 ? Math.max(1, Math.round(dd * dur())) : 1;
    }

    abstract int dur();
    abstract long time();


    /**
     * abs(term.dt()) safety limit for non-dternal/non-xternal temporal compounds
     * exists for debugging
     */
    @Deprecated
    public static int DT_ABS_LIMIT =
            Integer.MAX_VALUE / 16384;




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


    public final static int UnificationStackMax = 96; //how many assignments can be stored in the 'versioning' maps

//    /** estimate initial capacity for variable unification maps */
//    public static final int UnificationVariableCapInitial = 8;


    //public static final boolean DEBUG_BAG_MASS = false;
    //public static boolean DEBUG_TRACE_EVENTS = false; //shows all emitted events
    //public static boolean DEBUG_DERIVATION_STACKTRACES; //includes stack trace in task's derivation rule string
    //public static boolean DEBUG_INVALID_SENTENCES = true;
    //public static boolean DEBUG_NONETERNAL_QUESTIONS = false;
    public static final boolean DEBUG_TASK_LOG = true; //false disables task history completely

    /**
     * internal granularity which truth components are rounded to
     */
    public static final float TRUTH_EPSILON = 0.01f;
    public static final float TRUTH_MAX_CONF = 1f - TRUTH_EPSILON;
    public static final float TRUTH_MIN_EVI = Float.MIN_NORMAL;
    public static final float TRUTH_MAX_EVI = Float.MAX_VALUE;


    /**
     * how precise unit test results must match expected values to pass
     */
    public static final float TESTS_TRUTH_ERROR_TOLERANCE = TRUTH_EPSILON*4;


//    /** EXPERIMENTAL  decreasing priority of sibling tasks on temporal task insertion */
//    public static final boolean SIBLING_TEMPORAL_TASK_FEEDBACK = false;

//    /** EXPERIMENTAL enable/disable dynamic tasklink truth revision */
//    public static final boolean ACTION_CONCEPT_LINK_TRUTH = false;


//    /** derivation confidence (by evidence) multiplier.  normal=1.0, <1.0= under-confident, >1.0=over-confident */
//    @NotNull public final FloatParam derivedEvidenceGain = new FloatParam(1f, 0f, 4f);


    /**
     * truth confidence threshold necessary to form tasks
     */
    public final FloatRange confMin = new FloatRange(TRUTH_EPSILON, TRUTH_EPSILON, TRUTH_MAX_CONF);

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


//    /**
//     * tolerance of complexity
//     * low values (~0) will penalize complexity in derivations maximally (preferring simplicity)
//     * high values (~1) will penalize complexity in deriations minimally (allowing complexity)
//     */
//    public final FloatRange deep = new FloatRange(0f, 0, 1f);

    /**
     * computes the projected evidence at a specific distance (dt) from a perceptual moment evidence
     * with a perceptual duration used as a time constant
     * dt >= 0
     */
    public static double evi(double evi, double dt, long dur) {
        assert(Double.isFinite(dt) && dt>=0 && dur > 0);
        return evi / (1.0 + (dt / dur)); //inverse linear

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


    public final float confDefault(byte punctuation) {

        switch (punctuation) {
            case BELIEF:
                return beliefConfDefault.floatValue();

            case GOAL:
                return goalConfDefault.floatValue();

            default:
                throw new RuntimeException("Invalid punctuation " + punctuation + " for a TruthValue");
        }
    }


//    /** no term sharing means faster comparison but potentially more memory usage. TODO determine effects */
//    public static boolean CompoundDT_TermSharing;


    /**
     * Default priority of input judgment
     */
    public final FloatRange beliefPriDefault = new FloatRange(0.5f, 0, 1f);

    /**
     * Default priority of input question
     */
    public final FloatRange questionPriDefault = new FloatRange(0.5f, 0, 1f);

    /**
     * Default priority of input judgment
     */
    public final FloatRange goalPriDefault = new FloatRange(0.5f, 0, 1f);

    /**
     * Default priority of input question
     */
    public final FloatRange questPriDefault = new FloatRange(0.5f, 0, 1f);


    public float priDefault(byte punctuation) {
        switch (punctuation) {
            case BELIEF:
                return beliefPriDefault.floatValue();

            case QUEST:
                return questPriDefault.floatValue();

            case QUESTION:
                return questionPriDefault.floatValue();

            case GOAL:
                return goalPriDefault.floatValue();

            case COMMAND:
                return 1;
        }
        throw new RuntimeException("Unknown punctuation: " + punctuation);
    }


    public final FloatRange beliefConfDefault = new FloatRange(0.9f, Param.TRUTH_EPSILON, 1f-Param.TRUTH_EPSILON);
    public final FloatRange goalConfDefault = new FloatRange(0.9f, Param.TRUTH_EPSILON, 1f-Param.TRUTH_EPSILON);


    /** returns evidence factor corresponding to the amount of overlap */
    public static float overlapFactor(float overlap) {
        return overlap > 0 ? 0 : 1; //prevents any overlap
        //return Util.sqr(1f-Util.unitize(overlap));
        //return (1f-overlap);
    }

    public static float beliefValue(Task beliefOrGoal) {
        //return (1f + beliefOrGoal.conf())/2f;
        return beliefOrGoal.conf();
        //return beliefOrGoal.conf() * (1 + (1f-beliefOrGoal.originality())); //input tasks are 'forced' into the system. derived tasks should seem more valuable, being the result of reasoning effort
    }


}
