package nars;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatRangeRounded;
import jcog.math.FloatSupplier;
import jcog.math.IntRange;
import jcog.pri.ScalarValue;
import jcog.pri.op.PriMerge;
import jcog.service.Parts;
import jcog.util.FloatFloatToFloatFunction;
import jcog.util.Range;
import nars.attention.PriNode;
import nars.exe.Exec;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.util.builder.MemoizingTermBuilder;
import nars.term.util.transform.Conceptualization;
import nars.term.util.transform.Retemporalize;
import nars.truth.polation.LinearTruthProjection;
import nars.truth.polation.TruthProjection;
import nars.truth.util.ConfRange;

import static java.lang.Float.NaN;
import static nars.Op.*;
import static nars.truth.func.TruthFunctions.c2wSafe;

/**
 * NAR Parameters
 */
public abstract class Param extends Parts<Term,NAR> {



    public static final Retemporalize conceptualization =
            Conceptualization.FlattenAndDeduplicateConj
            //Conceptualization.DirectXternal;
            //Conceptualization.PreciseXternal;
            //Conceptualization.FlattenAndDeduplicateAndUnnegateConj //untested
            ;
    public static final boolean SHUFFLE_TERMUTES = false;
    public static final boolean DT_DITHER_LOGARITHMICALLY = false;
    /** return <= 0 to disable */
    @Deprecated public static final float TASKLINK_GENERATED_QUESTION_PRI_RATE =
            0;
    public static final boolean REVISION_ALLOW_OVERLAP_IF_DISJOINT_TIME = false;
    public static final boolean DYNAMIC_TRUTH_STAMP_OVERLAP_FILTER = true;
            //0.75f;
    public static final boolean VOLMAX_RESTRICTS_INPUT = true; //input tasks
    public static final boolean VOLMAX_RESTRICTS = false; //all tasks
    public static final boolean TERMIFY_TRANSFORM_LAZY = true;
    public static final boolean OVERLAP_DOUBLE_SET_CYCLIC = true;



//    public static final int TERM_BYTE_KEY_CACHED_BELOW_VOLUME = 8;
    //public static final int SUBTERM_BYTE_KEY_CACHED_BELOW_VOLUME = 10; //TODO
    /** TODO needs tested whether recursive Unification inherits TTL */
    public static final int TASK_EVALUATION_TTL = 16;
    public static final int TASK_EVAL_FORK_LIMIT = 8;

//    /** can produce varieties of terms with dt below the dithered threshold time */
//    public static final boolean ALLOW_UNDITHERED_DT_IF_DITHERED_FAILS = false;
    public static final int TASK_EVAL_FORK_ATTEMPT_LIMIT = TASK_EVAL_FORK_LIMIT*2;
    /** >= 1  - maximum # of Answer attempts per Answer capacity.  so 2 means 2 tasks are tried for each Answer task slot in its capacity */
    public static final float ANSWER_COMPLETENESS =
            1f;
            //2f;
            //0.5f;

    public static final boolean DEBUG_SIMILAR_DERIVATIONS = false;
    /** should be monotonically increasing at most */
    public static final PriMerge tasklinkMerge = PriMerge.or;

//    public static final boolean ETERNALIZE_BELIEF_PROJECTED_FOR_GOAL_DERIVATION = false;
    /**
     * budget factor for combining 2 tasks in derivation
     * ex: double-premise derivations which depends on the task and belief budget
     * priority calculation here currently depends on a commutive and associaive function
     */
    public static final FloatFloatToFloatFunction DerivationPri =
        (t,b)->Util.unitize(t+b); //plus, max=1

//    /** durs surrounding a derived temporal goal with one eternal (of two) parent tasks */
//    public static final float GOAL_PROJECT_TO_PRESENT_RADIUS_DURS = 1;
    /**
     * Evidential Horizon, the amount of future evidence to be considered
     */
    public static final float HORIZON = 1f;

//    /** within how many durations a difference in dt is acceptable for target unification */
//    public static final float UNIFY_DT_TOLERANCE_DUR_FACTOR = 1f;

//    public static final boolean LINK_VARIABLE_UNIFIED_PREMISE = false;
    /**
     * Maximum length of the evidental base of the Stamp, a power of 2
     * TODO IntRange
     */
    public static final int STAMP_CAPACITY = 16;
    /** TODO make this NAR-specific */
    public static final int CAUSE_MAX = 32;
    public static final IntRange causeCapacity = new IntRange(32, 1, CAUSE_MAX);


//    /**
//     * warning: can interfere with expected test results
//     */
//    public static boolean ETERNALIZE_FORGOTTEN_TEMPORALS = false;
    public static final int CURIOSITY_CAPACITY = STAMP_CAPACITY/2;
    public static final long CURIOSITY_TASK_RANGE_DURS = 3;
    public static final boolean DEBUG_TASK_LOG = true;
    //PriMerge.plus;
            //PriMerge.max;
    //Util::mean;
        //tasklinkMerge::merge;
        //Util::or;

        //Util::or;
        //Math::max;
        //Util::and;


    //1.5f;
            //2f;

//    /** 0..1.0: how much to reduce a signal which hasnt changed (in proportion to change significance) */
//    public static final float SIGNAL_UNSURPRISING_FACTOR = 0.1f;
    public static final boolean DYNAMIC_TRUTH_TASK_TIME_DITHERING = false;
    /** should be enough to account for an expected evidence integration error rate */
    public static final float PROJECTION_EVIDENCE_INFLATION_PCT_TOLERANCE = 0.1f;

    public static final int DYN_TASK_MATCH_MODE = 1;

    /** if false, will use pri=ScalarValue.EPSILON */
    public static final boolean DELETE_PROXY_TASK_TO_DELETED_TASK = false;

    public static final boolean FORK_JOIN_EXEC_ASYNC_MODE = false;

    protected static final boolean DYNAMIC_CONCEPT_TRANSIENT = false;

    public static final int WHATS_CAPACITY = 128;
    public static final int HOWS_CAPACITY = 128;

    public static boolean ETERNALIZE_BELIEF_PROJECTED_IN_DERIVATION = false;
    public static boolean STRONG_COMPOSITION = false;
    /**
     * use this for advanced error checking, at the expense of lower performance.
     * it is enabled for unit tests automatically regardless of the value here.
     */
    public static boolean DEBUG;


    public enum truth { ;

        /**
         * internal granularity which truth components are rounded to
         */
        public static final float TRUTH_EPSILON = 0.01f;
        public static final float TRUTH_CONF_MAX = 1f - TRUTH_EPSILON;
        public static final float TRUTH_EVI_MAX = c2wSafe(TRUTH_CONF_MAX);
        public static final double TRUTH_EVI_MIN =
                            //c2wSafe(TRUTH_EPSILON);
                            //ScalarValue.EPSILON;
                            //Float.MIN_NORMAL;
                            Double.MIN_NORMAL;
    }



    /** how the subjective perception of objective truth changes through time */
    public enum projection {
    }

    /** task evidence  */
    public enum evidence {
    }

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

    /**
     * when merging dt's, ratio of the maximum difference in dt allowed
     * ratio of the dt difference compared to the smaller dt of the two being merged
     * probably values less than 0.5 are safe
     */
    public final FloatRange intermpolationRangeLimit = new FloatRange(
            //0.5f
            1f
            , 0, 1);
    @Deprecated public final FloatRange questionForgetRate = new FloatRange(0.5f, 0, 1);
    public final IntRange premiseUnifyTTL = new IntRange(16, 1, 32);
    public final IntRange deriveBranchTTL = new IntRange(6 * derive.TTL_MIN, derive.TTL_MIN, 64 * derive.TTL_MIN );
    /**
     * how many cycles above which to dither dt and occurrence time
     * TODO move this to Time class and cache the cycle value rather than dynamically computing it
     */
    public final IntRange dtDither = new IntRange(1, 1, 1024);
    /**
     * hard upper-bound limit on Compound target complexity;
     * if this is exceeded it may indicate a recursively
     * malformed target due to a serious inference bug
     */
    public final IntRange termVolumeMax = new IntRange(64, 0, 128 /*COMPOUND_VOLUME_MAX*/);
    /**
     * truth confidence threshold necessary to form tasks
     */
    public final ConfRange confMin = new ConfRange();
    /**
     * global truth frequency resolution by which reasoning is dithered
     */
    public final FloatRange freqResolution = new FloatRangeRounded(truth.TRUTH_EPSILON, truth.TRUTH_EPSILON, 1f, truth.TRUTH_EPSILON);
    /**
     * global truth confidence resolution by which reasoning is dithered
     */
    public final FloatRange confResolution = new FloatRangeRounded(truth.TRUTH_EPSILON, truth.TRUTH_EPSILON, 1f, truth.TRUTH_EPSILON) {
        @Override
        public void set(float value) {
            super.set(value);
            value = get();
            if (confMin.floatValue() < value)
                confMin.set(value);
        }
    };
    /**
     * Default priority of input question
     */
    @Deprecated public final FloatRange questionPriDefault = new FloatRange(0.5f, ScalarValue.EPSILON, 1f);
    /**
     * Default priority of input question
     */
    public final FloatRange questPriDefault = new FloatRange(0.5f, ScalarValue.EPSILON, 1f);
    public final FloatRange beliefConfDefault = new FloatRange(0.9f, truth.TRUTH_EPSILON, 1f - truth.TRUTH_EPSILON);
    public final FloatRange goalConfDefault = new FloatRange(0.9f, truth.TRUTH_EPSILON, 1f - truth.TRUTH_EPSILON);
    public final PriNode beliefPriDefault = new PriNode.ConstPriNode("beliefPriDefault", 0.5f);
    public final PriNode goalPriDefault = new PriNode.ConstPriNode("goalPriDefault", 0.5f);


    protected Param(Exec exe) {
        super(exe);
    }

    /** priority of sensor task, with respect to how significantly it changed from a previous value */
    public static float signalSurprise(Task prev, Task next, FloatSupplier pri, NAR n) {

        float p = pri.asFloat();
        if (p != p)
            return NaN;

        boolean NEW = prev==null;

        boolean stretched = !NEW && prev==next;

        boolean latched = !NEW && !stretched &&
                Math.abs(next.start() - prev.end()) < belief.signal.SIGNAL_LATCH_LIMIT_DURS * n.dur();

        //decrease priority by similarity to previous truth
        if (prev!=null && (stretched || latched)) {

            //TODO abstract this frequence response curve
            float deltaFreq = prev!=next? Math.abs(prev.freq() - next.freq()) : 0; //TODO use a moving average or other anomaly/surprise detection
            if (deltaFreq > Float.MIN_NORMAL) {
                float perceived = 0.01f + 0.99f * (float) Math.pow(deltaFreq, 1 / 2f /* etc*/);
                p *= perceived;
            }
            //p *= Util.lerp(deltaFreq, perceived, 1);
        }

        return p;
    }

    static Atom randomSelf() {
        return $.uuid(/*"I_"*/);
    }

    /**
     * computes the projected evidence at a specific distance (dt) from a perceptual moment evidence
     * with a perceptual duration used as a time constant
     * dt >= 0
     *
     * @param dt > 0
     * @param dur > 0
     *
     * evi(baseEvidence, dt, dur)
     *   many functions will work here, provided:
     *
     *      evidential limit
     *        integral(evi(x, 0, d), evi(x, infinity, d)) is FINITE (convergent integral for t>=0)
     *
     *      temporal identity; no temporal difference,
     *        evi(x, 0, d) = 1
     *
     *      no duration, point-like
     *        evi(x, v, 0) = 0
     *
     *      monotonically decreasing
     *        for A >= B: evi(x, A, d) >= evi(x, B, d)
     *          since dt>=0, dur
     *
     * see:
     *      https://en.wikipedia.org/wiki/List_of_definite_integrals
     *      https://en.wikipedia.org/wiki/Template:Series_(mathematics)
     *
     * TODO integrate with EvidenceEvaluator
     */
    public static double evi(double evi, long dt, int dur) {

        //assert(dur > 0);
        assert(dur > 0 && dt > 0);

        double e;

        //inverse linear decay
        double falloffDurs =
                //0.5f;
                //1;
                //1.618f; //phi
                //2; //nyquist / horizon
                4;
                //dur;
                //8;
                //64;

        double decayTime = falloffDurs * dur;

        //quadratic decay: integral finite from to infinity, see: https://en.wikipedia.org/wiki/List_of_definite_integrals
        e = (evi / (1.0 + Util.sqr(dt / decayTime )));
        //e = (float)(evi / (1.0 + Util.sqr(((double)dt) / dur ) / falloffDurs));

        //exponential decay: see https://en.wikipedia.org/wiki/Exponential_integral
        //TODO

        //constant duration linear decay ("trapezoidal")
        //e = (float) (evi * Math.max(0, (1.0 - dt / decayTime)));

        //constant duration quadratic decay (sharp falloff)
        //e = evi * Math.max(0, (float) (1.0 - Math.sqrt(dt / decayTime)));

        //constant duration quadratic discharge (slow falloff)
        //e = evi * Math.max(0, 1.0 - Util.sqr(dt / decayTime));

        //linear decay WARNING - not finite integral
        //e = (float) (evi / (1.0 + dt / decayTime));

        //---------

        //eternal noise floor (post-filter)
        //float ee = TruthFunctions.eternalize(evi);
        //     // / STAMP_CAPACITY;
        //e = ee + ((e - ee) / (1.0 + (((float)dt) / (falloffDurs * dur))));

        return e;

        //return evi / (1.0f +    Util.sqr(((float)dt) / (falloffDurs * dur)));
        //return evi / (1.0f +    Util.sqr(((float)dt) / dur)/falloffDurs);


        //return evi / (1.0f + ( Math.max(0,(dt-dur/2f)) / (dur)));

        //return evi / (1.0f + ( Math.max(0f,(dt-dur)) / (dur)));



        //return evi * 1/sqrt(Math.log(1+(Math.pow((dt/dur),3)*2))*(dt/dur)+1); //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLyhsb2coMSsoeCp4KngqMikpKih4KSsxKV4wLjUiLCJjb2xvciI6IiMyMTE1QUIifSx7InR5cGUiOjAsImVxIjoiMS8oMSt4KSIsImNvbG9yIjoiIzAwMDAwMCJ9LHsidHlwZSI6MTAwMCwid2luZG93IjpbIi0xLjg4NDM2OTA0NzQ3Njc5OTgiLCI4LjUxNTYzMDk1MjUyMzE2OCIsIi0yLjMxMTMwMDA4MTI0NjM4MTgiLCI0LjA4ODY5OTkxODc1MzU5OCJdLCJzaXplIjpbNjQ2LDM5Nl19XQ--
        //return (float) (evi / (1.0 + Util.sqr(((double)dt) / dur)));
        //return evi * 1/(Math.log(1+((dt/dur)*0.5))*(dt/dur)+1); //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLyhsb2coMSsoeCowLjUpKSooeCkrMSleMC41IiwiY29sb3IiOiIjMjExNUFCIn0seyJ0eXBlIjowLCJlcSI6IjEvKDEreCkiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjEwMDAsIndpbmRvdyI6WyIyLjYzMDEyOTMyODgxMzU2ODUiLCIxOC44ODAxMjkzMjg4MTM1MzUiLCItMy45NTk4NDE5MDg3NzE5MTgiLCI2LjA0MDE1ODA5MTIyODA1NyJdLCJzaXplIjpbNjQ4LDM5OF19XQ--
        //return evi * (Util.tanhFast((-(((float)dt)/dur)+2))+1)/2; //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIodGFuaCgteCsyKSsxKS8yIiwiY29sb3IiOiIjMjExNUFCIn0seyJ0eXBlIjowLCJlcSI6IjEvKDEreCkiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjEwMDAsInNpemUiOls2NDgsMzk4XX1d

        //return (float) (evi / (1.0 + Math.log(1 + ((double)dt) / dur)));

    }

    /**
     * provides an instance of the default truthpolation implementation
     */
    public TruthProjection projection(long start, long end, int dur) {
        return new LinearTruthProjection(start, end, dur);
        //return new FocusingLinearTruthPolation(start, end, dur);
    }

    /**
     * number of time units (cycles) to dither into
     */
    public int dtDither() {
        return dtDither.intValue();
    }

    /** cycles per duration */
    abstract int dur();

    abstract long time();

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

    public float priDefault(byte punctuation) {
        switch (punctuation) {
            case BELIEF:
                return beliefPriDefault.asFloat();

            case GOAL:
                return goalPriDefault.asFloat();

            case QUEST:
                return questPriDefault.floatValue();

            case QUESTION:
                return questionPriDefault.floatValue();


            case COMMAND:
                return 1;
        }
        throw new RuntimeException("Unknown punctuation: " + punctuation);
    }


    public enum premise { ;
        public static final boolean PREMISE_FOCUS_TIME_DITHER = true;

        /** disable common variables for the query variables matched in premise formation; since the task target is not transformed like the belief target is.*/
        public static final boolean PREMISE_UNIFY_COMMON_VARIABLES = false;

        public static final boolean PREMISE_KEY_DITHER = true;
    }



    public enum belief { ;

        /** true will filter sub-confMin revision results.  false will not, allowing sub-confMin
         * results to reside in the belief table (perhaps combinable with something else that would
         * eventually raise to above-confMin).  generally, false should be more accurate with a tradeoff
         * for overhead due to increased belief table churn.
         */
        public static final boolean REVISION_MIN_EVI_FILTER = false;
        public static final boolean DYNAMIC_TRUTH_TASK_STORE = true;
        /** perceptible priority increase that warrants automatic reactivation.
         * used during Remember's merge repeat suppression filter */
        public static final float REMEMBER_REPEAT_PRI_THRESHOLD = ScalarValue.EPSILONcoarse;
        /** memory reconsolidation period - time period for a memory to be refreshed as new
         *  useful as a novelty threshold:
         *          >=0, higher values decrease the rate at which repeated tasks can be reactivated */
        public static int REMEMBER_REPEAT_THRESH_DURS = 1;
        /** maximum span of a Task, in cycles.
         *  beyond a certain length, evidence integration precision suffers accuracy diminishes and may become infinite */
        public static long TASK_RANGE_LIMIT = (1L << 61) /* estimate */;


        /** SignalTask's */
        public enum signal {;

            public static final boolean SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS = true;
            public static final int SIGNAL_BELIEF_TABLE_SERIES_SIZE = 512;
            /**
             * maximum time (in durations) that a signal task can stretch the same value
             * until a new task (with new evidence) is created (seamlessly continuing it afterward)
             *
             * TODO make this a per-sensor implementation cdecision
             */
            public final static float SIGNAL_STRETCH_LIMIT_DURS = 8;
            /** maximum time between signal updates to stretch an equivalently-truthed data point across.
             * stretches perception across some amount of lag
             * */
            public final static float SIGNAL_LATCH_LIMIT_DURS =
                    //0.5f;
                    1f;
        }
    }



    public enum term { ;

        /** whether INT atoms can name a concept directly */
        public static final boolean INT_CONCEPTUALIZABLE = false;
        /** applies certain reductions to INH and SIM terms when one or both of their immediate subterms
         *  are negated.  in assuming a "closed-boolean-world" in which there is one and only one
         *  opposite for any truth frequency in 0..1,
         *
         *  then the following statements should be equivalent:
         *
         *  INH
         *  (x --> --y)    |-  --(x --> y)
         *  (--x --> y)    |-  --(x --> y)
         *  (--x --> --y)  |-    (x --> y)
         *
         *  SIM
         *  (x <-> --y)    |-  --(x <-> y)
         *  (--x <-> --y)  |-    (x <-> y)
         *
         * */
        public static final boolean INH_SIM_CLOSED_BOOLEAN_DUALITY_MOBIUS_PARADIGM = true;
        /**
         * absolute limit for constructing terms in any context in which a NAR is not known, which could provide a limit.
         * typically a NAR instance's 'compoundVolumeMax' parameter will be lower than this
         */
        public static final int COMPOUND_VOLUME_MAX = Short.MAX_VALUE;
        /**
         * limited because some subterm paths are stored as byte[]. to be safe, use 7-bits
         */
        public static final int SUBTERMS_MAX = Byte.MAX_VALUE;
        public static final int MAX_INTERNED_VARS = 32;
        /**
         * how many INT terms are canonically interned/cached. [0..n)
         */
        public final static int MAX_INTERNED_INTS = 64;
    }

    public enum test { ;

        /**
         * for NALTest's: extends the time all unit tests are allowed to run for.
         * normally be kept to 1 but for debugging this may be increased to find what tests need more time
         */
        public static final float TIME_MULTIPLIER = 3f;
        /**
         * how precise unit test results must match expected values to pass
         */
        public static final float TRUTH_ERROR_TOLERANCE = truth.TRUTH_EPSILON * 2;
        public static boolean DEBUG_EXTRA = false;
        //public static final int TTL_MUTATE_COMPONENT = 0;
        public static boolean DEBUG_ENSURE_DITHERED_TRUTH = false;
        public static boolean DEBUG_ENSURE_DITHERED_OCCURRENCE = false;
        public static boolean DEBUG_ENSURE_DITHERED_DT = false;
    }

    public enum derive { ;

        /** may cause unwanted "sticky" event conflation. may only be safe when the punctuation of the task in which the event contained is the same */
        public static final boolean TIMEGRAPH_ABSORB_CONTAINED_EVENT = false;
        /** if false, keeps intersecting timegraph events separate.
         *  if true, it merges them to one event. may cause unwanted "sticky" event conflation
         *  may only be safe when the punctuation of the task in which the event contained is the same
         * */
        public static final boolean TIMEGRAPH_MERGE_INTERSECTING_EVENTS = false;
        /** whether timegraph should not return solutions with volume significantly less than the input's.
         *  set 0 to disable the filter */
        public static final float TIMEGRAPH_IGNORE_DEGENERATE_SOLUTIONS_FACTOR = 0f;
        /** whether to dither events as they are represented internally.  output events are dithered for the NAR regardless. */
        public static final boolean TIMEGRAPH_DITHER_EVENTS_INTERNALLY = false;
        /**
         * TTL = 'time to live'
         */
        public static final int TermutatorSearchTTL = 4;
        public static final int TermUnifyForkMax = 2;


        public static final int TTL_UNISUBST_MAX = 5;
        public static final int TTL_CONJ_BEFORE_AFTER = TTL_UNISUBST_MAX;


        @Range(min = 1, max = 32)
        public static final int TIMEGRAPH_ITERATIONS = 2;

        @Range(min = 0, max = 64)
        public static final int TTL_COST_BRANCH = 1;
        /**
         * cost of executing a termute permutation
         */
        @Range(min = 0, max = 64)
        public static final int TTL_COST_MUTATE = 1;
        /**
         * cost of a successful task derivation
         */
        @Range(min = 0, max = 64)
        public static final int TTL_COST_DERIVE_TASK_SUCCESS = 5;
        /**
         * estimate
         */
        @Deprecated
        public static final int TTL_MIN =
                (2) +
                        (TTL_COST_BRANCH * 1) + TTL_COST_DERIVE_TASK_SUCCESS;
        /**
         * cost of a repeat (of another within the premise's batch) task derivation
         */
        @Range(min = 0, max = 64)
        public static final int TTL_COST_DERIVE_TASK_REPEAT = 3;
        /**
         * cost of a task derived, but too similar to one of its parents
         */
        @Range(min = 0, max = 64)
        public static final int TTL_COST_DERIVE_TASK_SAME = 2;
        /**
         * cost of a failed/aborted task derivation
         */
        @Range(min = 0, max = 64)
        public static final int TTL_COST_DERIVE_TASK_FAIL = 1;
        @Range(min = 0, max = 64)
        public static final int TTL_COST_DERIVE_TASK_UNPRIORITIZABLE = TTL_COST_DERIVE_TASK_FAIL;
        public static final boolean DERIVE_FILTER_SIMILAR_TO_PARENTS = true;

        /** attempt to create a question/quest task from an invalid belief/goal (probably due to missing or unsolved temporal information */
        public static final boolean DERIVATION_FORM_QUESTION_FROM_AMBIGUOUS_BELIEF_OR_GOAL = false;

        public static final float TERMIFY_TERM_VOLMAX_FACTOR = 2f;
    }

    public enum unify { ;

        /** max variable unification recursion depth as a naive cyclic filter */
        public static final int UNIFY_VAR_RECURSION_DEPTH_LIMIT = 4;
        public static final int UNIFY_COMMON_VAR_MAX = 6;
        public final static int UNIFICATION_STACK_CAPACITY = 64;
    }

}
