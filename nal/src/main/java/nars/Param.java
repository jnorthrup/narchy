package nars;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatRangeRounded;
import jcog.math.IntRange;
import jcog.math.Range;
import jcog.pri.op.PriMerge;
import jcog.util.FloatFloatToFloatFunction;
import nars.term.atom.Atom;
import nars.time.Tense;
import nars.truth.polation.FocusingLinearTruthPolation;
import nars.truth.polation.TruthPolation;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * NAR Parameters
 */
public abstract class Param {


    public static final boolean FILTER_DYNAMIC_MATCHES = true;

    /**
     * experimental increased confidence calculation for composition, taken from the NAL spec but is different from OpenNARS
     */
    public static final boolean STRONG_COMPOSITION = false;


    public static final boolean SHUFFLE_TERMUTES = true;


    /**
     * size of buffer for tasks that have been derived (and are being de-duplicated) but not yet input.
     * input may happen concurrently (draining the bag) while derivations are inserted from another thread.
     */
    public static final int DerivedTaskBagCapacity = 2048;
    public static final float DerivedTaskBagDrainRate =
            //1f;
            0.5f;
            //0.33f;

    /** TODO make an abstract TermLinkStrategy class responsible for this */
    @Deprecated public static final int TermLinkFanoutMax =
            8;

    public static final int TERM_BYTE_KEY_CACHED_BELOW_VOLUME = 5;
    public static final int SUBTERM_BYTE_KEY_CACHED_BELOW_VOLUME = 10; //TODO

    public static final int SIGNAL_BELIEF_TABLE_SERIES_SIZE = 256;
    public static final int CURIOSITY_BELIEF_TABLE_SERIES_SIZE = 128;


    //5;


    /**
     * warning: can interfere with expected test results
     */
    public static boolean ETERNALIZE_FORGOTTEN_TEMPORALS = false;

    public static final boolean ETERNALIZE_BELIEF_PROJECTED_IN_DERIVATION = true;
    public static final boolean ETERNALIZE_BELIEF_PROJECTED_FOR_GOAL_DERIVATION = true;


    /**
     * default bag forget rate
     */
    public final FloatRange forgetRate = new FloatRange(1f, 0f, 2f);


    public static final boolean FILTER_SIMILAR_DERIVATIONS = true;
    public static final boolean DEBUG_SIMILAR_DERIVATIONS = false;


    /**
     * use this for advanced error checking, at the expense of lower performance.
     * it is enabled for unit tests automatically regardless of the value here.
     */
    public static boolean DEBUG;
    public static final boolean DEBUG_EXTRA = false;

    public static final PriMerge conceptMerge =
            PriMerge.plus;


    public static final PriMerge termlinkMerge =
            //PriMerge.max;
            PriMerge.plus;
    //PriMerge.or;

    public static final PriMerge tasklinkMerge =
            //PriMerge.avgGeoFast;
            PriMerge.max;
    //PriMerge.plus;
    //PriMerge.or;

    /**
     * for equivalent tasks
     */
    public static final PriMerge taskEquivalentMerge =
            PriMerge.max;
    //PriMerge.avg;
    //PriMerge.avgGeoSlow;

    /**
     * budget factor for double-premise derivations: depends on the task and belief budget
     */
    public static final FloatFloatToFloatFunction TaskBeliefToDerivation =
            (t, b) ->
                    //il.aveAri(t, b);
                    //Util.aveGeo(t, b);
                    //Util.and(t, b);
                    //Util.or(t, b);
                    (t+b);

    /**
     * budget factor for single-premise derivations: depends only on the task budget
     */
    public static final FloatToFloatFunction TaskToDerivation = (t) -> t;

    public static final PriMerge taskMerge =
            PriMerge.max;
    //PriMerge.plus;


    /**
     * maximum time (in durations) that a signal task can latch its last value before it becomes unknown
     */
    @Deprecated
    public final static int SIGNAL_LATCH_TIME_MAX = 8;


    /**
     * NAgent happiness automatic gain control time parameter
     * TODO verify this is applied based on time, not iterations
     */
    public final static float HAPPINESS_RE_SENSITIZATION_RATE = 0.0002f;
    public final static float HAPPINESS_RE_SENSITIZATION_RATE_FAST = 0.0004f;


    /**
     * temporal radius (in durations) around the present moment to scan for truth
     */
    public final FloatRange timeFocus = new FloatRange(0.5f, 0, 100);

    /**
     * when merging dt's, maximum difference in dt in which interpolation is applied to the merge.
     * otherwise temporal information is discarded because it would be too inaccurate to simply
     * choose an intermediate dt (ex: average value, or at random)
     */
    public final FloatRange intermpolationDurLimit = new FloatRange(2, 0, 128 /* no practical upper limit really */);

    /**
     * creates instance of the default truthpolation implementation
     */
    public static TruthPolation truth(long start, long end, int dur) {
        //return new LinearTruthPolation(start, end, dur);
        return new FocusingLinearTruthPolation(start, end, dur);
    }

    /**
     * provides a start,end pair of time points for the current focus given the current time and duration
     */
    public final long[] timeFocus() {
        return timeFocus(time());
    }

    public final long[] timeFocus(long when) {
        return timeFocus(when, dur());
    }

    //TODO void timeFocus(when, dur, long[] store)

    public final long[] timeFocus(long when, float dur) {
        if (when == ETERNAL)
            return new long[]{ETERNAL, ETERNAL};

        if (when == XTERNAL) {
            throw new RuntimeException();
        }

        int f = Math.round(dur * timeFocus.floatValue());
        int ditherCycles = dtDither();
        long from = Tense.dither(when - f, ditherCycles);
        long to = Tense.dither(when + f, ditherCycles);
        return new long[]{from, to};
    }

    /**
     * TTL = 'time to live'
     */

    public final IntRange deriveBranchTTL = new IntRange(2 * TTL_MIN, 0, TTL_MIN * 16);


    /**
     * extends the time all unit tests are allowed to run for.
     * normally be kept to 1 but for debugging this may be increased to find what tests need more time
     */
    public static float TEST_TIME_MULTIPLIER = 3f;


    @Range(min = 1, max = 32)
    public static int TEMPORAL_SOLVER_ITERATIONS = 3;


    /**
     * cost of attempting a unification
     */
    @Range(min = 0, max = 64)
    public static int TTL_UNIFY = 1;

    @Range(min = 0, max = 64)
    public static final int TTL_BRANCH = 1;

    /**
     * cost of executing a termute permutation
     */
    @Range(min = 0, max = 64)
    public static int TTL_MUTATE = 1;

    /**
     * cost of a successful task derivation
     */
    @Range(min = 0, max = 64)
    public static int TTL_DERIVE_TASK_SUCCESS = 5;

    /**
     * cost of a repeat (of another within the premise's batch) task derivation
     */
    @Range(min = 0, max = 64)
    public static int TTL_DERIVE_TASK_REPEAT = 3;

    @Range(min = 0, max = 64)
    public static int TTL_DERIVE_TASK_UNPRIORITIZABLE = 3;

    /**
     * cost of a task derived, but too similar to one of its parents
     */
    @Range(min = 0, max = 64)
    public static int TTL_DERIVE_TASK_SAME = 3;


    /**
     * cost of a failed/aborted task derivation
     */
    @Range(min = 0, max = 64)
    public static int TTL_DERIVE_TASK_FAIL = 4;


    /**
     * estimate
     */
    @Deprecated
    public static final int TTL_MIN =
            (Param.TTL_UNIFY * 2) +
                    (Param.TTL_BRANCH * 1) + Param.TTL_DERIVE_TASK_SUCCESS;


    public final FloatRange termlinkBalance = new FloatRange(0.5f, 0, 1f);


    public final FloatRange activateConceptRate = new FloatRange(1f, 0, 1f);


    /**
     * how many durations above which to dither dt relations to dt=0 (parallel)
     * set to zero to disable dithering.  typically the value will be 0..1.0.
     * TODO move this to Time class and cache the cycle value rather than dynamically computing it
     */
    public final IntRange dtDither = new IntRange(1, 1, 1024);

    /**
     * number of time units (cycles) to dither into
     */
    public int dtDither() {
        return dtDither.intValue();
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
     * hard upper-bound limit on Compound term complexity;
     * if this is exceeded it may indicate a recursively
     * malformed term due to a serious inference bug
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

    public static final IntRange causeCapacity = new IntRange(32, 0, 128);

    /**
     * hard limit for cause capacity in case the runtime parameter otherwise disobeyed
     */
    public static final int CAUSE_LIMIT = (causeCapacity.max * 2);


    public final static int UnificationStackMax = 96;


    public static final boolean DEBUG_TASK_LOG = true;

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
    public static final float TESTS_TRUTH_ERROR_TOLERANCE = TRUTH_EPSILON * 4;


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
    public static double evi(double evi, double dt, long dur) {


        int falloffDurs =
                1;
                //4;
                //dur;
        return evi / (1.0 + (dt / (falloffDurs * dur)));

        //return evi / (1.0 + ( Math.max(0,(dt-dur/2)) / (dur)));

//        return9+ evi / (1.0 + ( Math.max(0,(dt-dur)) / (dur)));



        //return evi * 1/sqrt(Math.log(1+(Math.pow((dt/dur),3)*2))*(dt/dur)+1); //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLyhsb2coMSsoeCp4KngqMikpKih4KSsxKV4wLjUiLCJjb2xvciI6IiMyMTE1QUIifSx7InR5cGUiOjAsImVxIjoiMS8oMSt4KSIsImNvbG9yIjoiIzAwMDAwMCJ9LHsidHlwZSI6MTAwMCwid2luZG93IjpbIi0xLjg4NDM2OTA0NzQ3Njc5OTgiLCI4LjUxNTYzMDk1MjUyMzE2OCIsIi0yLjMxMTMwMDA4MTI0NjM4MTgiLCI0LjA4ODY5OTkxODc1MzU5OCJdLCJzaXplIjpbNjQ2LDM5Nl19XQ--
        //return evi / (1.0 + Util.sqr(dt / dur));
        //return evi * 1/(Math.log(1+((dt/dur)*0.5))*(dt/dur)+1); //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLyhsb2coMSsoeCowLjUpKSooeCkrMSleMC41IiwiY29sb3IiOiIjMjExNUFCIn0seyJ0eXBlIjowLCJlcSI6IjEvKDEreCkiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjEwMDAsIndpbmRvdyI6WyIyLjYzMDEyOTMyODgxMzU2ODUiLCIxOC44ODAxMjkzMjg4MTM1MzUiLCItMy45NTk4NDE5MDg3NzE5MTgiLCI2LjA0MDE1ODA5MTIyODA1NyJdLCJzaXplIjpbNjQ4LDM5OF19XQ--
        //return evi * (Util.tanhFast((float) (-(dt/dur)+2))+1)/2; //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIodGFuaCgteCsyKSsxKS8yIiwiY29sb3IiOiIjMjExNUFCIn0seyJ0eXBlIjowLCJlcSI6IjEvKDEreCkiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjEwMDAsInNpemUiOls2NDgsMzk4XX1d

        //return evi / (1.0 + Math.log(1 + dt / dur));


        //int falloffDurs = 64;
        //int falloffDurs = 1;
        //return evi * Math.max(0, (1.0 - (dt / (falloffDurs *dur)))); //constant time decay

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


    public final FloatRange beliefConfDefault = new FloatRange(0.9f, Param.TRUTH_EPSILON, 1f - Param.TRUTH_EPSILON);
    public final FloatRange goalConfDefault = new FloatRange(0.9f, Param.TRUTH_EPSILON, 1f - Param.TRUTH_EPSILON);


    public static float beliefValue(Task beliefOrGoal) {

        return beliefOrGoal.conf();

    }


}
