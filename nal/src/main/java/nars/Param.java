package nars;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatRangeRounded;
import jcog.math.IntRange;
import jcog.math.Range;
import jcog.pri.ScalarValue;
import jcog.pri.op.PriMerge;
import jcog.util.FloatFloatToFloatFunction;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.util.builder.MemoizingTermBuilder;
import nars.term.util.transform.Conceptualization;
import nars.term.util.transform.Retemporalize;
import nars.truth.polation.LinearTruthPolation;
import nars.truth.polation.TruthPolation;

import java.util.function.Predicate;

import static nars.Op.*;
import static nars.truth.func.TruthFunctions.c2wSafe;

/**
 * NAR Parameters
 */
public abstract class Param {




    static {
        Op.terms =
                //HeapTermBuilder.the;

                //new InterningTermBuilder();

                new MemoizingTermBuilder();

//                new VerifyingTermBuilder(
//                    new MemoizingTermBuilder(),
//                    new VerifyingTermBuilder(
//                            new MemoizingTermBuilder() //new InterningTermBuilder()
//                            ,
//                            HeapTermBuilder.the
//                    )
//                );
    }

    public static final Retemporalize conceptualization =
            Conceptualization.FlattenAndDeduplicateConj
            //Conceptualization.FlattenAndDeduplicateAndUnnegateConj //untested
            ;

    public static final boolean FILTER_SIGNAL_TABLE_TEMPORAL_TASKS = true;



    public static final boolean SHUFFLE_TERMUTES = true; //this may be dangerous



    public static final boolean ALLOW_REVISION_OVERLAP_IF_DISJOINT_TIME = false;
//    public static final boolean INPUT_PREMISE_ANSWER_BELIEF = false;

    public static final boolean DYNAMIC_TRUTH_STAMP_OVERLAP_FILTER = true;




    /**
     * when merging dt's, ratio of the maximum difference in dt allowed
     * ratio of the dt difference compared to the smaller dt of the two being merged
     * probably values less than 0.5 are safe
     */
    public final FloatRange intermpolationRangeLimit = new FloatRange(
            //0.5f
            1f
            , 0, 1);



//    public static final int TERM_BYTE_KEY_CACHED_BELOW_VOLUME = 8;
    //public static final int SUBTERM_BYTE_KEY_CACHED_BELOW_VOLUME = 10; //TODO

    public static final int SIGNAL_BELIEF_TABLE_SERIES_SIZE = 512;





//    /** can produce varieties of terms with dt below the dithered threshold time */
//    public static final boolean ALLOW_UNDITHERED_DT_IF_DITHERED_FAILS = false;

    public static final boolean VOLMAX_RESTRICTS_INPUT = true; //input tasks
    public static final boolean VOLMAX_RESTRICTS = false; //all tasks


    public static boolean ETERNALIZE_BELIEF_PROJECTED_IN_DERIVATION = false;

    /** if ETERNALIZE_BELIEF_PROJECTED_IN_DERIVATION is true, then this is tested: */
    public static final Predicate<Op> eternalizeInDerivation = (o) -> {
        return true;
        //return o == IMPL;
    };

//    public static final boolean ETERNALIZE_BELIEF_PROJECTED_FOR_GOAL_DERIVATION = false;

    /** whether INT atoms can name a concept directly */
    public static final boolean INT_CONCEPTUALIZABLE = false;


    public static final boolean OVERLAP_DOUBLE_SET_CYCLIC = true;

//    /** durs surrounding a derived temporal goal with one eternal (of two) parent tasks */
//    public static final float GOAL_PROJECT_TO_PRESENT_RADIUS_DURS = 1;

    /** TODO needs tested whether recursive Unification inherits TTL */
    public static final int EVALUATION_TTL = 32;

//    /** within how many durations a difference in dt is acceptable for target unification */
//    public static final float UNIFY_DT_TOLERANCE_DUR_FACTOR = 1f;

//    public static final boolean LINK_VARIABLE_UNIFIED_PREMISE = false;

    public static final int TASK_EVAL_FORK_LIMIT = 8;
    public static final int TASK_EVAL_TRY_LIMIT = TASK_EVAL_FORK_LIMIT*2;

    /** >= 1  - maximum # of Answer attempts per Answer capacity.  so 2 means 2 tasks are tried for each Answer task slot in its capacity */
    public static final float ANSWER_COMPLETENESS = 2f;

//    public static final boolean DERIVE_AUTO_IMAGE_NORMALIZE = true;


//    /**
//     * warning: can interfere with expected test results
//     */
//    public static boolean ETERNALIZE_FORGOTTEN_TEMPORALS = false;


    public static boolean STRONG_COMPOSITION = false;

    /** attempt to create a question/quest task from an invalid belief/goal (probably due to missing or unsolved temporal information */
    public static final boolean INVALID_DERIVATION_TRY_QUESTION = true;


    public static final boolean FILTER_SIMILAR_DERIVATIONS = true;
    public static final boolean DEBUG_SIMILAR_DERIVATIONS = false;


    /**
     * use this for advanced error checking, at the expense of lower performance.
     * it is enabled for unit tests automatically regardless of the value here.
     */
    public static boolean DEBUG;
    public static boolean DEBUG_EXTRA = false;
    public static boolean DEBUG_ENSURE_DITHERED_TRUTH = false;
    public static boolean DEBUG_ENSURE_DITHERED_OCCURRENCE = false;
    public static boolean DEBUG_ENSURE_DITHERED_DT = false;

    public static final PriMerge tasklinkMerge =
            //PriMerge.max;
            PriMerge.plus;
            //PriMerge.or;
            //PriMerge.avgGeoFast;


    /**
     * budget factor for combining 2 tasks in derivation
     * ex: double-premise derivations which depends on the task and belief budget
     * priority calculation here currently depends on a commutive and associaive function
     */
    public static final FloatFloatToFloatFunction DerivationPri =
        (t,b)->Util.unitize(t+b);
        //Math::max;
        //Math::min;
        //Util::and;
        //Util::or;
        //Util::mean;



    public static final PriMerge taskMerge = PriMerge.max;

    /** perceptible priority increase that warrants automatic reactivation.
     * used during Remember's merge repeat suppression filter */
    public static final float REMEMBER_REPEAT_PRI_THRESHOLD = ScalarValue.EPSILONsqrt;

    /** novelty threshold: >=0; higher values decrease the rate at which repeated tasks can be reactivated */
    public static final float REMEMBER_REPEAT_THRESH_DURS = 2f;

    /** restrains revision's ability to stretch evidence across time:
     * as a factor of the maximum of the ranges of the tasks involved in the revision */
    public static final float TASK_REVISION_STRETCH_LIMIT_PROPORTION =
            //1;
            1.5f;

    /** maximum span of a Task, in cycles.
     *  beyond a certain length, evidence integration precision suffers accuracy diminishes and may become infinite */
    public static long TASK_RANGE_LIMIT = (1L << 61) /* estimate */;

    /**
     * maximum time (in durations) that a signal task can stretch the same value
     * until a new task (with new evidence) is created (seamlessly continuing it afterward)
     */
    public final static float SIGNAL_STRETCH_LIMIT_DURS = 8;

    /** maximum time between signal updates to stretch an equivalently-truthed data point across.
     * stretches perception across some amount of lag
     * */
    public final static float SIGNAL_LATCH_LiMIT_DURS =
            //0.5f;
            //1f;
            //1.5f;
            2f;




    /** may cause unwanted "sticky" event conflation */
    public static final boolean TIMEGRAPH_ABSORB_CONTAINED_EVENT = false;

    /** if false, keeps intersecting timegraph events separate.  if true, it merges them to one event. may cause unwanted "sticky" event conflation */
    public static final boolean TIMEGRAPH_MERGE_INTERSECTING_EVENTS = false;

    /** whether timegraph should not return solutions with volume significantly less than the input's */
    public static final boolean TIMEGRAPH_IGNORE_DEGENERATE_SOLUTIONS = false;

    /** max variable unification recursion depth as a naive cyclic filter */
    public static final int UNIFY_VAR_RECURSION_DEPTH_LIMIT = 8;


    /** (unsafe) true should theoreticaly be faster,
     * at the risk of budgeting inaccuracies and unfairly neglected lost derivations */
    public static final boolean INPUT_BUFFER_PRI_BACKPRESSURE = false;

    /**
     * creates instance of the default truthpolation implementation
     */
    public static TruthPolation truth(long start, long end, int dur) {
        return new LinearTruthPolation(start, end, dur);
        //return new FocusingLinearTruthPolation(start, end, dur);
    }

//    /**
//     * provides a start,end pair of time points for the current focus given the current time and duration
//     */
//    public final long[] timeFocus() {
//        return timeFocus(time());
//    }
//
//    public final long[] timeFocus(long when) {
//        return timeFocus(when, dur());
//    }
//
//    //TODO void timeFocus(when, dur, long[] store)
//
//    public final long[] timeFocus(long when, float dur) {
//        if (when == ETERNAL)
//            return new long[]{ETERNAL, ETERNAL};
//
//        if (when == XTERNAL) {
//            throw new RuntimeException();
//        }
//
//        int f = Math.round(dur * timeFocus.floatValue());
//        int ditherCycles = dtDither();
//        long from = Tense.dither(when - f, ditherCycles);
//        long to = Tense.dither(when + f, ditherCycles);
//        return new long[]{from, to};
//    }

    /**
     * TTL = 'time to live'
     */
    public final IntRange deriveBranchTTL = new IntRange(16 * TTL_MIN, TTL_MIN, 64 * TTL_MIN );
    public final IntRange subUnifyTTLMax = new IntRange( 4, 1, 32);
    public final IntRange matchTTL = new IntRange(4, 1, 32);

    public static final int TTL_CONJ_BEFORE_AFTER = 4;

    /**
     * for NALTest's: extends the time all unit tests are allowed to run for.
     * normally be kept to 1 but for debugging this may be increased to find what tests need more time
     */
    public static final float TEST_TIME_MULTIPLIER = 2f;


    @Range(min = 1, max = 32)
    public static final int TEMPORAL_SOLVER_ITERATIONS = 2;


    /**
     * cost of attempting a unification
     */
    @Range(min = 0, max = 64)
    public static final int TTL_UNIFY = 1;

    @Range(min = 0, max = 64)
    public static final int TTL_BRANCH = 1;

    /**
     * cost of executing a termute permutation
     */
    @Range(min = 0, max = 64)
    public static final int TTL_MUTATE = 1;

    /**
     * cost of a successful task derivation
     */
    @Range(min = 0, max = 64)
    public static final int TTL_DERIVE_TASK_SUCCESS = 5;

    /**
     * cost of a repeat (of another within the premise's batch) task derivation
     */
    @Range(min = 0, max = 64)
    public static final int TTL_DERIVE_TASK_REPEAT = 2;


    /**
     * cost of a task derived, but too similar to one of its parents
     */
    @Range(min = 0, max = 64)
    public static final int TTL_DERIVE_TASK_SAME = 2;

    /**
     * cost of a failed/aborted task derivation
     */
    @Range(min = 0, max = 64)
    public static final int TTL_DERIVE_TASK_FAIL = 1;
    @Range(min = 0, max = 64)
    public static final int TTL_DERIVE_TASK_UNPRIORITIZABLE = TTL_DERIVE_TASK_FAIL;


    /**
     * estimate
     */
    @Deprecated
    public static final int TTL_MIN =
            (Param.TTL_UNIFY * 2) +
                    (Param.TTL_BRANCH * 1) + Param.TTL_DERIVE_TASK_SUCCESS;



    public static final int TaskLinkSpreadDefault =
            //16;
            //12;
            //10;
            8;
            //7;
            //6;
            //5;
            //4;
            //3;
            //2;
            //1;

    /** termlink template layers depth. TODO this will be abstracted to more flexible weighted activation heuristic */
    @Deprecated public static final int TERMLINK_TEMPLATE_DEPTH_min = 2;
    @Deprecated public static final int TERMLINK_TEMPLATE_DEPTH = 3 /* 2..4 tested to work ok */;

    /**
     * includes the host as layer 0, so if this returns 1 it will only include the host
     */
    public static int termlinkTemplateLayers(Term root) {
//        if (root.op().statement)
//            return Param.TERMLINK_TEMPLATE_DEPTH;
//        else
            return Param.TERMLINK_TEMPLATE_DEPTH_min;

//        switch (root.op()) {
//            case PROD:
//            case CONJ:
//                return TERMLINK_TEMPLATE_DEPTH_min;
//            default:
//                return Param.TERMLINK_TEMPLATE_DEPTH;
//        }

//
//            case SETe:
//            case SETi:
//                return 2;
//
//            case SECTi:
//            case SECTe:
//                return 2;
//
//            case DIFFe:
//            case DIFFi:
//                return 2;
//
//
//            case SIM: {
//
//
////                if (x.subterms().OR(xx -> xx.unneg().isAny(SetBits | Op.SectBits | Op.PROD.bit)))
////                    return 3;
////                else
//                    return 2;
//            }
//
//            case INH: {
////                if (x.subterms().OR(xx -> xx.unneg().isAny(Op.SetBits | Op.SectBits
////                        | Op.PROD.bit
////                        )))
////                    return 3;
//
//                return 2;
//            }
//
//            case IMPL:
////                if (x./*subterms().*/hasAny(Op.CONJ.bit))
////                    return 3;
////                else
//                    return 2;
////                }
//
//
//            case CONJ:
////                if (x.hasAny(Op.IMPL))
////                    return 3;
//                return 2;
//
//
//            default:
//                /** atomics,etc */
//                return 0;
//
//        }
    }


    /**
     * how many durations above which to dither dt relations to dt=0 (parallel)
     * set to zero to disable dithering.  typically the value will be 0..1.0.
     * TODO move this to Time class and cache the cycle value rather than dynamically computing it
     */
    public final IntRange timeResolution = new IntRange(1, 1, 1024);

    /**
     * number of time units (cycles) to dither into
     */
    public int dtDither() {
        return timeResolution.intValue();
    }

    abstract int dur();


    abstract long time();


//    /**
//     * abs(target.dt()) safety limit for non-dternal/non-xternal temporal compounds
//     * exists for debugging
//     */
//    @Deprecated
//    public static int DT_ABS_LIMIT =
//            Integer.MAX_VALUE / 16384;


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
     * hard upper-bound limit on Compound target complexity;
     * if this is exceeded it may indicate a recursively
     * malformed target due to a serious inference bug
     */
    public final IntRange termVolumeMax = new IntRange(COMPOUND_VOLUME_MAX, 0, COMPOUND_VOLUME_MAX);


    static Atom randomSelf() {
        return (Atom) $.quote(/*"I_" + */Util.uuid64());
    }

    /**
     * Evidential Horizon, the amount of future evidence to be considered
     */
    public static final float HORIZON = 1f;


    public static final int MAX_INTERNED_VARS = 32;

    /**
     * how many INT terms are canonically interned/cached. [0..n)
     */
    public final static int MAX_INTERNED_INTS = 64;


    /**
     * Maximum length of the evidental base of the Stamp, a power of 2
     * TODO IntRange
     */
    public static final int STAMP_CAPACITY = 16;

    /** TODO make this NAR-specific */
    public static final int CAUSE_MAX = 32;
    public static final IntRange causeCapacity = new IntRange(32, 1, CAUSE_MAX);

    public static final int CURIOSITY_CAPACITY = STAMP_CAPACITY/2;

    public final static int UnificationStackMax = 128;


    public static final boolean DEBUG_TASK_LOG = true;

    /**
     * internal granularity which truth components are rounded to
     */
    public static final float TRUTH_EPSILON = 0.01f;
    public static final float TRUTH_CONF_MAX = 1f - TRUTH_EPSILON;
    public static final float TRUTH_EVI_MAX = c2wSafe(Param.TRUTH_CONF_MAX);
    public static final float TRUTH_EVI_MIN =
                        //c2wSafe(TRUTH_EPSILON);
                        //ScalarValue.EPSILON;
                        Float.MIN_NORMAL;


    /**
     * how precise unit test results must match expected values to pass
     */
    public static final float TESTS_TRUTH_ERROR_TOLERANCE = TRUTH_EPSILON * 2;


    /**
     * truth confidence threshold necessary to form tasks
     */
    public final FloatRange confMin = new FloatRange(TRUTH_EPSILON, TRUTH_EPSILON, TRUTH_CONF_MAX);

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
            value = get();
            if (confMin.floatValue() < value)
                confMin.set(value);
        }
    };


    /**
     * computes the projected evidence at a specific distance (dt) from a perceptual moment evidence
     * with a perceptual duration used as a time constant
     * dt >= 0
     */
    public static float evi(float evi, long dt, long dur) {

        assert(dur > 0);

        //inverse linear decay
        int falloffDurs =
                1;
                //2; //nyquist
                //4;
                //dur;
                //8;

        return evi / (1.0f + (((float)dt) / (falloffDurs * dur)));

//        //eternal noise floor
//        float ee = TruthFunctions.eternalize(evi);
//                       // / STAMP_CAPACITY;
//        return ee +  (evi - ee) / (1.0f + (((float)dt) / (falloffDurs * dur)));


        //return evi / (1.0f +    Util.sqr(((float)dt) / (falloffDurs * dur)));
        //return evi / (1.0f +    Util.sqr(((float)dt) / dur)/falloffDurs);

        //return evi * Math.max(0, (1.0f - ((float)dt) / (falloffDurs * dur))); //constant time linear decay



        //return evi / (1.0f + ( Math.max(0,(dt-dur/2f)) / (dur)));

        //return evi / (1.0f + ( Math.max(0f,(dt-dur)) / (dur)));



        //return evi * 1/sqrt(Math.log(1+(Math.pow((dt/dur),3)*2))*(dt/dur)+1); //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLyhsb2coMSsoeCp4KngqMikpKih4KSsxKV4wLjUiLCJjb2xvciI6IiMyMTE1QUIifSx7InR5cGUiOjAsImVxIjoiMS8oMSt4KSIsImNvbG9yIjoiIzAwMDAwMCJ9LHsidHlwZSI6MTAwMCwid2luZG93IjpbIi0xLjg4NDM2OTA0NzQ3Njc5OTgiLCI4LjUxNTYzMDk1MjUyMzE2OCIsIi0yLjMxMTMwMDA4MTI0NjM4MTgiLCI0LjA4ODY5OTkxODc1MzU5OCJdLCJzaXplIjpbNjQ2LDM5Nl19XQ--
        //return (float) (evi / (1.0 + Util.sqr(((double)dt) / dur)));
        //return evi * 1/(Math.log(1+((dt/dur)*0.5))*(dt/dur)+1); //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLyhsb2coMSsoeCowLjUpKSooeCkrMSleMC41IiwiY29sb3IiOiIjMjExNUFCIn0seyJ0eXBlIjowLCJlcSI6IjEvKDEreCkiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjEwMDAsIndpbmRvdyI6WyIyLjYzMDEyOTMyODgxMzU2ODUiLCIxOC44ODAxMjkzMjg4MTM1MzUiLCItMy45NTk4NDE5MDg3NzE5MTgiLCI2LjA0MDE1ODA5MTIyODA1NyJdLCJzaXplIjpbNjQ4LDM5OF19XQ--
        //return evi * (Util.tanhFast((-(((float)dt)/dur)+2))+1)/2; //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIodGFuaCgteCsyKSsxKS8yIiwiY29sb3IiOiIjMjExNUFCIn0seyJ0eXBlIjowLCJlcSI6IjEvKDEreCkiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjEwMDAsInNpemUiOls2NDgsMzk4XX1d

        //return (float) (evi / (1.0 + Math.log(1 + ((double)dt) / dur)));




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


    /**
     * Default priority of input judgment
     */
    public final FloatRange beliefPriDefault = new FloatRange(0.5f, ScalarValue.EPSILONsqrt, 1f);

    /**
     * Default priority of input question
     */
    public final FloatRange questionPriDefault = new FloatRange(0.5f, ScalarValue.EPSILONsqrt, 1f);

    /**
     * Default priority of input judgment
     */
    public final FloatRange goalPriDefault = new FloatRange(0.5f, ScalarValue.EPSILONsqrt, 1f);

    /**
     * Default priority of input question
     */
    public final FloatRange questPriDefault = new FloatRange(0.5f, ScalarValue.EPSILONsqrt, 1f);


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


    public final FloatRange beliefConfDefault = new FloatRange(0.9f, Param.TRUTH_EPSILON, 1f - Param.TRUTH_EPSILON);
    public final FloatRange goalConfDefault = new FloatRange(0.9f, Param.TRUTH_EPSILON, 1f - Param.TRUTH_EPSILON);



}
