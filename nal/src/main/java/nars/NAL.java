package nars;

import jcog.Skill;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatRangeRounded;
import jcog.math.IntRange;
import jcog.pri.ScalarValue;
import jcog.pri.op.PriMerge;
import jcog.thing.Thing;
import jcog.util.FloatFloatToFloatFunction;
import jcog.util.Range;
import nars.attention.PriNode;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.util.builder.MemoizingTermBuilder;
import nars.term.util.transform.Conceptualization;
import nars.term.util.transform.Retemporalize;
import nars.time.Time;
import nars.truth.proj.LinearTruthProjection;
import nars.truth.proj.TruthProjection;
import nars.truth.util.ConfRange;
import nars.util.Timed;

import java.util.Random;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static fucknutreport.config.NodeConfig.configIs;
import static nars.Op.*;
import static nars.truth.func.TruthFunctions.c2wSafe;

/**
 * NAR Parameters
 */
public abstract class NAL<W> extends Thing<W, Term> implements Timed {


    public static final Retemporalize conceptualization =
            Conceptualization.FlattenAndDeduplicateConj
            //Conceptualization.FlattenAndDeduplicateAndUnnegateConj //untested
            //Conceptualization.DirectXternal;
            //Conceptualization.PreciseXternal;
            ;
    public static final boolean SHUFFLE_TERMUTES= configIs("SHUFFLE_TERMUTES");
    public static final boolean DT_DITHER_LOGARITHMICALLY= configIs("DT_DITHER_LOGARITHMICALLY");

    public static final float DEFAULT_CURIOSITY_RATE = 0.05f;

    public static final int NEG_CACHE_VOL_THRESHOLD = 4;

    public static final boolean OCCURRIFY_STRICT = true;

    /** opportunity for interning term builder to memoize parallel conjunction terms */
    public static final boolean CONJ_COMMUTIVE_LOOPBACK = true;


    /**
     * return <= 0 to disable
     */
    @Deprecated
    public static final float TASKLINK_GENERATED_QUESTION_PRI_RATE = 0;
    public static final boolean REVISION_ALLOW_OVERLAP_IF_DISJOINT_TIME= configIs("REVISION_ALLOW_OVERLAP_IF_DISJOINT_TIME");


    /** can be unfair to new inputs if true, so for fairness leave false */
    public static final boolean DYNAMIC_TRUTH_STAMP_OVERLAP_FILTER= configIs("DYNAMIC_TRUTH_STAMP_OVERLAP_FILTER");


    public static final boolean VOLMAX_RESTRICTS_INPUT= configIs("VOLMAX_RESTRICTS_INPUT"); //input tasks
    public static final boolean VOLMAX_RESTRICTS= configIs("VOLMAX_RESTRICTS"); //all tasks


    public static final int TERM_BUFFER_VOL_MIN = Integer.MAX_VALUE;



//    public static final boolean OVERLAP_DOUBLE_SET_CYCLIC = configIs("OVERLAP_DOUBLE_SET_CYCLIC");


    /**
     * TODO needs tested whether recursive Unification inherits TTL
     */
    public static final int TASK_EVALUATION_TTL = 16;
    public static final int TASK_EVAL_FORK_LIMIT = 8;

    //    /** can produce varieties of terms with dt below the dithered threshold time */
//    public static final boolean ALLOW_UNDITHERED_DT_IF_DITHERED_FAILS= !configIs("DISABLE_ALLOW_UNDITHERED_DT_IF_DITHERED_FAILS");
    public static final int TASK_EVAL_FORK_ATTEMPT_LIMIT = NAL.TASK_EVAL_FORK_LIMIT * 2;

    /**
     * >= 1  - maximum # of Answer attempts per Answer capacity.
     * so 2 means 2 tasks are tried for each Answer task slot in its capacity
     * lower values will result in less confident but slower evidential stamp capacity consumption
     */
    public static final float ANSWER_COMPLETENESS =
            //0.25f;
            //0.333f;
            0.5f;
            //0.75f;
            //1f;
            //2f;

    public static final boolean DEBUG_SIMILAR_DERIVATIONS= false;
    /**
     * should be monotonically increasing at most
     */
    public static final PriMerge tasklinkMerge =
            PriMerge.plus;
            //PriMerge.or;


    /**
     * budget factor for combining 2 tasks in derivation
     * ex: double-premise derivations which depends on the task and belief budget
     * priority calculation here currently depends on a commutive and associaive function
     */
    public static final FloatFloatToFloatFunction DerivationPri =
        (t, b) -> Util.unitize(t + b); //plus, max=1
        //Util::and;
        //Util::or;

//    /** durs surrounding a derived temporal goal with one eternal (of two) parent tasks */
//    public static final float GOAL_PROJECT_TO_PRESENT_RADIUS_DURS = 1;
    /**
     * Evidential Horizon, the amount of future evidence to be considered
     */
    public static final float HORIZON = 1f;

//    /** within how many durations a difference in dt is acceptable for target unification */
//    public static final float UNIFY_DT_TOLERANCE_DUR_FACTOR = 1f;

//    public static final boolean LINK_VARIABLE_UNIFIED_PREMISE= !configIs("DISABLE_LINK_VARIABLE_UNIFIED_PREMISE");
    /**
     * Maximum length of the evidental base of the Stamp, a power of 2
     * TODO IntRange
     */
    public static final int STAMP_CAPACITY = 16;
    /**
     * TODO make this NAR-specific
     */
    public static final int CAUSE_MAX = 32;
    public static final IntRange causeCapacity = new IntRange(32, 1, NAL.CAUSE_MAX);


    //    /**
//     * warning: can interfere with expected test results
//     */
//    public static boolean ETERNALIZE_FORGOTTEN_TEMPORALS = false;
    public static final int CURIOSITY_CAPACITY = NAL.STAMP_CAPACITY / 2;
    public static final long CURIOSITY_TASK_RANGE_DURS = 3;
    public static final boolean DEBUG_TASK_LOG= configIs("DEBUG_TASK_LOG");
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



    public static final boolean ANSWER_TASK_TIME_DITHERING= configIs("ANSWER_TASK_TIME_DITHERING");

    /**
     * should be enough to account for an expected evidence integration error rate
     */
    public static final float PROJECTION_EVIDENCE_INFLATION_PCT_TOLERANCE = 10000; //0.1f;
    public static final boolean PROJECTION_EVIDENCE_INFLATION_DETECT = false;

    public static final int DYN_TASK_MATCH_MODE = 1;


    /**
     * if false, will use pri=ScalarValue.EPSILON
     */
    public static final boolean DELETE_PROXY_TASK_TO_DELETED_TASK= configIs("DELETE_PROXY_TASK_TO_DELETED_TASK");

    public static final boolean FORK_JOIN_EXEC_ASYNC_MODE= configIs("FORK_JOIN_EXEC_ASYNC_MODE");

    /**
     * might help with overall throughput by reducing lock and synchronization contention by grouping sequences of tasks by the destination concept
     */
    public static final boolean PRE_SORT_TASK_INPUT_BATCH = configIs("PRE_SORT_TASK_INPUT_BATCH");
    public static final int WHATS_CAPACITY = 128;
    public static final int HOWS_CAPACITY = 128;

    /** divisor for dividing the table's range of held beliefs in determining a 'table duration' for comparison of relative task strength */
    public static final long TEMPORAL_BELIEF_TABLE_DUR_DIVISOR =
            2;

    /**
     * enables higher-order (recursive) implication statements
     *
     * if true: allows recursive implication constructions
     * if false: reduce to Null as an invalid statement
     *      ex: (((a==>b) && x) ==> y)
     */
    public static final boolean IMPLICATION_SUBJECT_CAN_CONTAIN_IMPLICATION = true;

    /** override to allow all belief evidence overlap */
    public static final boolean OVERLAP_ALLOW_BELIEF = false;

    /** override to allow all goal evidence overlap */
    public static final boolean OVERLAP_ALLOW_GOAL = false;

    /** if true then tasklink targets are named by the concept and not a raw term (which could be temporal or not normalized)
     * */
    public static final boolean TASKLINK_TARGET_CONCEPT = false;

    /** various post-processing of contained variables prior to use in Task content */
    public static final boolean TASK_COMPOUND_POST_NORMALIZE = true;

    /** controls the temporal range of Time:Compose conclusions
     *  TODO DiluteUnion will require reprojection of the task and the belief to their temporal midpoint
     *  this requires changes to Derivation
     * */
    public static final boolean OCCURRIFY_COMPOSE_UNION_DILUTE = false;


    protected static final boolean DYNAMIC_CONCEPT_TRANSIENT = false;

    public static boolean ETERNALIZE_BELIEF_PROJECTED_IN_DERIVATION = true;
    public static boolean ETERNALIZE_BELIEF_PROJECTED_IN_DERIVATION_AND_ETERNALIZE_BELIEF_TIME = true;

    /** TODO make these dynamic parameters of a NALTruth implementation */
    public static class nal_truth {
        public static final boolean STRONG_DECOMPOSE = false;

        public static final boolean STRONG_COMPOSITION = false;
    }

    /**
     * use this for advanced error checking, at the expense of lower performance.
     * it is enabled for unit tests automatically regardless of the value here.
     */
    public static boolean DEBUG;

    static {
        terms =
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
     * probably values <= ~0.5 are safe as this limits stretching within 'octaves'
     */
    public final FloatRange intermpolationRangeLimit = new FloatRange(
            //0.5f
            //0.618f
            1f
            //2f
            , 0, 4);

    @Deprecated
    public final FloatRange questionForgetRate = new FloatRange(0.5f, 0, 1);
    public final IntRange premiseUnifyTTL = new IntRange(6, 1, 32);
    public final IntRange deriveBranchTTL = new IntRange(4 * NAL.derive.TTL_MIN, NAL.derive.TTL_MIN, 64 * NAL.derive.TTL_MIN);
    /**
     * how many cycles above which to dither dt and occurrence time
     * TODO move this to Time class and cache the cycle value rather than dynamically computing it
     */
    public final IntRange dtDither = new IntRange(1, 1, 1024);
    /**
     * hard upper-bound limit on Compound target complexity;
     * if this is exceeded it may indicate a recursively
     * malformed target due to a bug or unconstrained combinatorial explosion
     */
    public final IntRange termVolMax = new IntRange(64, 0, 512 /*COMPOUND_VOLUME_MAX*/);
    /**
     * truth confidence threshold necessary to form tasks
     */
    public final ConfRange confMin = new ConfRange();
    /**
     * global truth frequency resolution by which reasoning is dithered
     */
    public final FloatRange freqResolution = new FloatRangeRounded(NAL.truth.TRUTH_EPSILON, NAL.truth.TRUTH_EPSILON, 1f, NAL.truth.TRUTH_EPSILON);
    /**
     * global truth confidence resolution by which reasoning is dithered
     */
    public final FloatRange confResolution = new FloatRangeRounded(NAL.truth.TRUTH_EPSILON, NAL.truth.TRUTH_EPSILON, 1f, NAL.truth.TRUTH_EPSILON) {
        @Override
        public void set(float value) {
            super.set(value);
            value = this.get();
            if (NAL.this.confMin.floatValue() < value)
                NAL.this.confMin.set(value);
        }
    };
    /**
     * Default priority of input question
     */


    /**
     * Default priority of input question
     */

    public final ConfRange beliefConfDefault = new ConfRange(0.9f);
    public final ConfRange goalConfDefault = new ConfRange(0.9f);

    /** HACK use PriNode.amp(..) to set these.  will figure this out.  pri wont work right, as this is the actual value vs. the advised (user provided) */
    public final PriNode beliefPriDefault = PriNode.constant("pri.", 1).amp(0.5f);
    public final PriNode goalPriDefault = PriNode.constant("pri!", 1).amp(0.5f);
    public final PriNode questionPriDefault = PriNode.constant("pri?", 1).amp( 0.5f);
    public final PriNode questPriDefault = PriNode.constant("pri@", 1).amp( 0.5f);

    public final Time time;

    protected final Supplier<Random> random;

    protected NAL(final Executor exe, Time time, Supplier<Random> rng) {
        super(exe);
        this.random = rng;
        (this.time = time).reset();
    }



    /**
     * creates a new evidence stamp
     */
    public final long[] evidence() {
        return new long[]{time.nextStamp()};
    }
    /**
     * priority of sensor task, with respect to how significantly it changed from a previous value
     */
    public static float signalSurprise(final Task prev, final Task next, int dur) {

        final boolean NEW = prev == null;
        if (!NEW) {

            final boolean stretched = prev == next;
            if (stretched)
                return 0;

            final boolean latched = !stretched &&
                    Math.abs(next.start() - prev.end()) < NAL.belief.signal.SIGNAL_LATCH_LIMIT_DURS * dur;

            //decrease priority by similarity to previous truth
            if (latched) {

                //TODO abstract this frequence response curve
                final float deltaFreq = prev != next ? Math.abs(prev.freq() - next.freq()) : 0; //TODO use a moving average or other anomaly/surprise detection
                if (deltaFreq > Float.MIN_NORMAL) {
                    final float perceived = 0.01f + 0.99f * (float) Math.pow(deltaFreq, 1 / 2f /* etc*/);
                    return perceived;
                }
                //p *= Util.lerp(deltaFreq, perceived, 1);
            } else
                return 1;
        }

        return 0;
    }

    static Atom randomSelf() {
        return $.uuid(/*"I_"*/);
    }

    /**
     * computes the projected evidence at a specific distance (dt) from a perceptual moment evidence
     * with a perceptual duration used as a time constant
     * dt >= 0
     *
     * @param dt  > 0
     * @param dur > 0
     *            <p>
     *            evi(baseEvidence, dt, dur)
     *            many functions will work here, provided:
     *            <p>
     *            evidential limit
     *            integral(evi(x, 0, d), evi(x, infinity, d)) is FINITE (convergent integral for t>=0)
     *            <p>
     *            temporal identity; no temporal difference,
     *            evi(x, 0, d) = 1
     *            <p>
     *            no duration, point-like
     *            evi(x, v, 0) = 0
     *            <p>
     *            monotonically decreasing
     *            for A >= B: evi(x, A, d) >= evi(x, B, d)
     *            since dt>=0, dur
     *            <p>
     *            see:
     *            https://en.wikipedia.org/wiki/List_of_definite_integrals
     *            https://en.wikipedia.org/wiki/Template:Series_(mathematics)
     *            <p>
     *            TODO integrate with EvidenceEvaluator
     */
    public static double evi(final double evi, final long dt, final int dur) {

        //assert(dur > 0);
        assert (dur > 0 && dt > 0);


        //inverse linear decay
        final double falloffDurs =
                //0.5f;
                //1;
                //1.618f; //phi
                2; //nyquist / horizon
                //4;
                //dur;
                //8;


        final double decayTime = falloffDurs * dur;
        double e;

        //quadratic decay: integral finite from to infinity, see: https://en.wikipedia.org/wiki/List_of_definite_integrals
        e = (evi / (1.0 + Util.sqr(dt / decayTime)));
            //e = (float)(evi / (1.0 + Util.sqr(((double)dt) / dur ) / falloffDurs));

        //cartoon quadratic decay
//        float edge = dur / 2;
//        e = (evi / (1.0 + Util.sqr(Math.max(0, dt - edge) / decayTime)));

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
        //double ee = TruthFunctions.eternalize(evi);
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

    public final TruthProjection projection(final long start, final long end, final int dur) {
        return new LinearTruthProjection(start, end, dur);
    }

    /**
     * number of time units (cycles) to dither into
     */
    public final int dtDither() {
        return this.dtDither.intValue();
    }

    /**
     * cycles per duration
     */
    abstract public int dur();

    abstract public long time();

    public final float confDefault(final byte punctuation) {

        switch (punctuation) {
            case BELIEF:
                return this.beliefConfDefault.floatValue();

            case GOAL:
                return this.goalConfDefault.floatValue();

            default:
                throw new RuntimeException("Invalid punctuation " + punctuation + " for a TruthValue");
        }
    }

    public final float priDefault(final byte punctuation) {
        PriNode p;
        switch (punctuation) {
            case BELIEF: p = this.beliefPriDefault; break;
            case GOAL: p = this.goalPriDefault; break;
            case QUESTION: p = this.questionPriDefault; break;
            case QUEST: p = this.questPriDefault; break;
            case COMMAND: return 1;
            default: throw new RuntimeException("Unknown punctuation: " + punctuation);
        }
        return p.asFloat();
    }


    /**
     * provides a Random number generator
     */
    @Override
    public final Random random() { return random.get(); }

    public enum truth { ;

        /**
         * internal granularity which truth components are rounded to
         */
        public static final float TRUTH_EPSILON = 0.01f;
        public static final float CONF_MAX = 1f - NAL.truth.TRUTH_EPSILON;
        public static final float EVI_MAX = c2wSafe(NAL.truth.CONF_MAX);
        public static final double EVI_MIN =
                //c2wSafe(TRUTH_EPSILON);
                //ScalarValue.EPSILON;
                //Float.MIN_NORMAL;
                Double.MIN_NORMAL;

        /** switch between soft and hard pre decision truth function */
        public static final boolean preSoft = true;
    }

    /**
     * how the subjective perception of objective truth changes through time
     */
    public enum projection {
    }

    /**
     * task evidence
     */
    public enum evidence {
    }


    public enum premise {
        ;
        public static final boolean PREMISE_FOCUS_TIME_DITHER= configIs("PREMISE_FOCUS_TIME_DITHER");

        /**
         * disable common variables for the query variables matched in premise formation; since the task target is not transformed like the belief target is.
         */
        public static final boolean PREMISE_UNIFY_COMMON_VARIABLES = configIs("PREMISE_UNIFY_COMMON_VARIABLES");

        /** TODO use Gödel numbering not this HACK */
        public static final boolean PREMISE_KEY_DITHER= configIs("PREMISE_KEY_DITHER");
    }


    public enum belief {
        ;

        /**
         * true will filter sub-confMin revision results.  false will not, allowing sub-confMin
         * results to reside in the belief table (perhaps combinable with something else that would
         * eventually raise to above-confMin).  generally, false should be more accurate with a tradeoff
         * for overhead due to increased belief table churn.
         */
        public static final boolean REVISION_MIN_EVI_FILTER= configIs("REVISION_MIN_EVI_FILTER");
        public static final boolean DYNAMIC_TRUTH_TASK_STORE= configIs("DYNAMIC_TRUTH_TASK_STORE");
        public static final boolean DYNAMIC_TRUTH_TASK_LINK= false;
        public static final boolean DYNAMIC_TRUTH_TASK_EMIT= false;
        /**
         * perceptible priority increase that warrants automatic reactivation.
         * used during Remember's merge repeat suppression filter
         */
        public static final float REMEMBER_REPEAT_PRI_THRESHOLD = ScalarValue.EPSILONcoarse;
        /**
         * memory reconsolidation period - time period for a memory to be refreshed as new
         * useful as a novelty threshold:
         * >=0, higher values decrease the rate at which repeated tasks can be reactivated
         */
        public static int REMEMBER_REPEAT_THRESH_DITHERS = 1;



        /**
         * maximum span of a Task, in cycles.
         * beyond a certain length, evidence integration precision suffers accuracy diminishes and may become infinite
         */
        public static long TASK_RANGE_LIMIT = (1L << 61) /* estimate */;


        /**
         * SignalTask's
         */
        public enum signal {
            ;

            public static final boolean SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS= configIs("SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS");
            public static final int SIGNAL_BELIEF_TABLE_SERIES_SIZE = 512;
            /**
             * maximum time (in durations) that a signal task can stretch the same value
             * until a new task (with new evidence) is created (seamlessly continuing it afterward)
             * <p>
             * TODO make this a per-sensor implementation cdecision
             */
            public static final float SIGNAL_STRETCH_LIMIT_DURS = 16;
            /**
             * maximum time between signal updates to stretch an equivalently-truthed data point across.
             * stretches perception across some amount of lag
             */
            public static final float SIGNAL_LATCH_LIMIT_DURS =/*0.5f;*/
                    2f;
                    //1f;

            /** max tasked matched by series table, in case the answer limit is higher.  this reduces the number of redundant non-exact matches freeing evidential capacity for non-signal tasks from other tables of the concept */
            public static final float SERIES_MATCH_ADDITIONAL_RATE_PER_DUR = 1f/SIGNAL_STRETCH_LIMIT_DURS;
            public static final int SERIES_MATCH_MIN = 2;

            //public static final boolean SIGNAL_TASK_OCC_DITHER = true;
        }
    }


    public enum term {
        ;

        /**
         * whether INT atoms can name a concept directly
         */
        public static final boolean INT_CONCEPTUALIZABLE = configIs("INT_CONCEPTUALIZABLE");

        /**
         * EXPERIMENTAL logical closure for relations of negations
         *
         * applies certain reductions to INH and SIM terms when one or both of their immediate subterms
         * are negated.  in assuming a "closed-boolean-world" in which there is one and only one
         * opposite for any truth frequency in 0..1,
         * <p>
         * then (some of) the following statements should be equivalent:
         * <p>
         * INH
         * (x --> --y)    |-  --(x --> y)
         * (--x --> y)    |-  --(x --> y)
         * (--x --> --y)  |-    (x --> y)
         * <p>
         * SIM (disabled)
         * (x <-> --y)    |-  --(x <-> y)
         * (--x <-> --y)  |-    (x <-> y)
         */
        @Skill({"List_of_dualities", "Nondualism", "Möbius_strip"})
        public static final boolean INH_CLOSED_BOOLEAN_DUALITY_MOBIUS_PARADIGM= configIs("INH_CLOSED_BOOLEAN_DUALITY_MOBIUS_PARADIGM");

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
        public static final int ANON_INT_MAX = Byte.MAX_VALUE;

        public static final boolean INH_IMAGE_RECURSION = false;
        public static final int TERM_BUFFER_MIN_REPLACE_AHEAD_SPAN = 2;
        public static final int TERM_BUFFER_MIN_INTERN_VOL = 2;


        /** prevent variable introduction from erasing negative compounds,
         *  though content within negatives can be var introduced as normal. */
        public static boolean VAR_INTRODUCTION_NEG_FILTER = true;
    }

    public enum test {
        ;

        /**
         * for NALTest's: extends the time all unit tests are allowed to run for.
         * normally be kept to 1 but for debugging this may be increased to find what tests need more time
         */
        public static final float TIME_MULTIPLIER = 1f;
        /**
         * how precise unit test results must match expected values to pass
         */
        public static final float TRUTH_ERROR_TOLERANCE = NAL.truth.TRUTH_EPSILON * 2;
        public static boolean DEBUG_EXTRA = false;
        public static boolean DEBUG_ENSURE_DITHERED_TRUTH = DEBUG_EXTRA;
        public static boolean DEBUG_ENSURE_DITHERED_OCCURRENCE= DEBUG_EXTRA;
        public static boolean DEBUG_ENSURE_DITHERED_DT = DEBUG_EXTRA;
    }

    public enum derive {
        ;

        /**
         * may cause unwanted "sticky" event conflation. may only be safe when the punctuation of the task in which the event contained is the same
         */
        public static final boolean TIMEGRAPH_ABSORB_CONTAINED_EVENT= configIs("TIMEGRAPH_ABSORB_CONTAINED_EVENT");
        /**
         * if false, keeps intersecting timegraph events separate.
         * if true, it merges them to one event. may cause unwanted "sticky" event conflation
         * may only be safe when the punctuation of the task in which the event contained is the same
         */
        public static final boolean TIMEGRAPH_MERGE_INTERSECTING_EVENTS= configIs("TIMEGRAPH_MERGE_INTERSECTING_EVENTS");
        /**
         * whether timegraph should not return solutions with volume significantly less than the input's.
         * set 0 to disable the filter
         */
        public static final float TIMEGRAPH_IGNORE_DEGENERATE_SOLUTIONS_FACTOR = 0f;
        /**
         * whether to dither events as they are represented internally.  output events are dithered for the NAR regardless.
         */
        public static final boolean TIMEGRAPH_DITHER_EVENTS_INTERNALLY= configIs("TIMEGRAPH_DITHER_EVENTS_INTERNALLY");
        public static final int TTL_CONJ_BEFORE_AFTER = NAL.derive.TTL_UNISUBST_MAX;


        @Range(min = 1, max = 32)
        public static final int TIMEGRAPH_ITERATIONS = 2;
        /**
         * TTL = 'time to live'
         */
        public static final int TermutatorSearchTTL = 4;




        public static final int TTL_UNISUBST_MAX = 8;


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
                        (NAL.derive.TTL_COST_BRANCH * 1) + NAL.derive.TTL_COST_DERIVE_TASK_SUCCESS;
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
        public static final int TTL_COST_DERIVE_TASK_UNPRIORITIZABLE = NAL.derive.TTL_COST_DERIVE_TASK_FAIL;
        public static final boolean DERIVE_FILTER_SIMILAR_TO_PARENTS= configIs("DERIVE_FILTER_SIMILAR_TO_PARENTS");

        /**
         * attempt to create a question/quest task from an invalid belief/goal (probably due to missing or unsolved temporal information
         * in some cases, forming the question may be answered by a dynamic truth calculation later
         */
        public static final boolean DERIVE_QUESTION_FROM_AMBIGUOUS_BELIEF_OR_GOAL = configIs("DERIVE_QUESTION_FROM_AMBIGUOUS_BELIEF_OR_GOAL");


        public static final float TERM_BUFFER_VOLMAX_SCRATCH_FACTOR = 8f;



    }

    public enum unify {
        ;

        /**
         * max variable unification recursion depth as a naive cyclic filter
         * includes indirections through common variables so should be at least 3.
         */
        public static final int UNIFY_VAR_RECURSION_DEPTH_LIMIT = 6;
        public static final int UNIFY_COMMON_VAR_MAX = UNIFY_VAR_RECURSION_DEPTH_LIMIT;
        public static final int UNIFICATION_STACK_CAPACITY = 128;
    }

}
