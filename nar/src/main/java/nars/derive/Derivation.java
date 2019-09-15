package nars.derive;

import jcog.Util;
import jcog.WTF;
import jcog.data.ShortBuffer;
import jcog.decide.MutableRoulette;
import jcog.pri.ScalarValue;
import jcog.signal.meter.FastCounter;
import nars.NAL;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.What;
import nars.control.CauseMerge;
import nars.derive.op.Occurrify;
import nars.derive.op.UnifyMatchFork;
import nars.derive.premise.Premise;
import nars.derive.util.DerivationFunctors;
import nars.derive.util.PremiseUnify;
import nars.eval.Evaluation;
import nars.op.Replace;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.anon.AnonWithVarShift;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.functor.AbstractInlineFunctor1;
import nars.term.util.TermTransformException;
import nars.term.util.transform.InstantFunctor;
import nars.truth.MutableTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.func.TruthFunction;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Predicate;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends PreDerivation {

    public static final Atom Task = Atomic.atom("task");
    public static final Atom Belief = Atomic.atom("belief");
    public static final Atom TaskTerm = Atomic.atom("taskTerm");
    public static final Atom BeliefTerm = Atomic.atom("beliefTerm");
    protected final static Logger logger = LoggerFactory.getLogger(Derivation.class);
    private final static int ANON_INITIAL_CAPACITY = 16;
    public static final ThreadLocal<Derivation> derivation = ThreadLocal.withInitial(Derivation::new);
    public static final Atom SUBSTITUTE = Atomic.atom("substitute");
    public static final Atom CONJ_WITHOUT = Atomic.atom("conjWithout");

    public final PremiseUnify premiseUnify = new PremiseUnify();

//    public final Bag<PostDerivation,PostDerivation> post = new PriArrayBag<>(PriMerge.max, 32);

    public final UnifyMatchFork termifier =
            new UnifyMatchFork();
            //new UnifyMatchFork.DeferredUnifyMatchFork();

    public final AnonWithVarShift anon;
    public final UniSubst uniSubstFunctor = new UniSubst(this);



    /**
     * second layer additional substitutions
     */
    public final Map<Term, Term> retransform = new MapAdapter<>(new UnifiedMap<>()) {
        @Nullable @Override public Term put(Term key, Term value) {
            return key.equals(value) ? null : delegate.put(key, value);
        }
    };
    public final Occurrify occ = new Occurrify(this);
//    public final long[] taskBelief_TimeIntersection = new long[2];
    public final Functor polarizeTask = new AbstractInstantFunctor1("polarizeTask") {
        @Override
        protected Term apply1(Term arg) {
            MutableTruth t = Derivation.this.taskTruth;
            return arg.negIf(t.is() ? t.isNegative() : random.nextBoolean());
            //return arg.negIf(t.isNegative());
        }
    };
    public final Functor polarizeBelief = new AbstractInstantFunctor1("polarizeBelief") {
        @Override
        protected Term apply1(Term arg) {
            MutableTruth b = Derivation.this.beliefTruth_at_Belief;
            return arg.negIf(b.is() ? b.isNegative() : random.nextBoolean());
//            return arg.negIf(b.isNegative());
        }
    };
    /**
     * cant be inline since the value will be cached and repeated
     */
//    final Functor polarizeRandom = Functor.f1("polarizeRandom", (arg)->random.nextBoolean() ? arg : arg.neg());
    public final Functor polarizeRandom = new AbstractInlineFunctor1("polarizeRandom") {
        @Override
        protected Term apply1(Term arg) {
            return arg.negIf(random.nextBoolean());
        }
    };
    /**
     * populates retransform map
     */
    public final Replace substituteFunctor = new Replace(Derivation.SUBSTITUTE) {

        @Override
        public @Nullable Term apply(Evaluation e, Subterms xx) {
            Term input = xx.sub(0), replaced = xx.sub(1), replacement = xx.sub(2);

            Term y = Replace.apply(xx, input, replaced, replacement);
            if (y != null)
                retransform.put(replaced, replacement);

            return y;
        }
    };
//    private final TermBuffer termBuilder = new TermBuffer();
//    private final TermBuffer directTermBuilder = new DirectTermBuffer();


    /**
     * current context
     */
    public transient What what = null;
    @Deprecated public NAR nar;
    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    public Predicate<Derivation> forEachMatch;

    public transient float confMin;
    public transient double eviMin;
    public transient int termVolMax;
    /**
     * whether either the task or belief are events and thus need to be considered with respect to time
     */
    public transient boolean temporal;
    public transient int ditherDT;
    public Deriver deriver;

    /**
     * precise time that the task and belief truth are sampled
     */
    public transient long taskStart, taskEnd, beliefStart, beliefEnd; //TODO taskEnd, beliefEnd
    public transient boolean overlapDouble, overlapSingle;

    @Deprecated public transient boolean single;
    @Deprecated public final MutableTruth truth = new MutableTruth();
    @Deprecated public transient byte punc;
    @Deprecated public transient TruthFunction truthFunction;

    public transient long time = TIMELESS;
    public transient float dur = Float.NaN;

    public transient Task _task, _belief;
    public transient Term _beliefTerm;

    public DerivationTransform transformDerived;

    public Predicate<nars.Task> tasklinkTaskFilter =
        (Task t) ->
            !t.isDeleted() && (t.isQuestionOrQuest() || t.evi() >= eviMin);

	private transient short[] parentCause;

    /** evi avg */
    private double eviDouble;
    private double eviSingle;

    private transient long[] stampDouble, stampSingle;
    private transient int taskUniqueAnonTermCount;

    /**
     * these represent the maximum possible priority of the derivation.
     * the maximum constraint is a contract ensuring the range of priority
     * can be predicted for deciding
     * whether to attempt before beginning,
     * or whether to continue deriving during the procedure.
     */
    private transient float priSingle, priDouble;
    private Term _taskTerm;
    private ImmutableMap<Atomic, Term> derivationFunctors;
//    private MethodHandle deriverMH;

    {
        premiseUnify.commonVariables = NAL.premise.PREMISE_UNIFY_COMMON_VARIABLES;
    }

    /**
     * if using this, must setAt: nar, index, random, DerivationBudgeting
     */
    private Derivation() {
        super(
                null
                //VAR_PATTERN
                , null, NAL.unify.UNIFICATION_STACK_CAPACITY
        );

        this.anon = new AnonWithVarShift(ANON_INITIAL_CAPACITY,
            Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_QUERY.bit
            //Op.VAR_DEP.bit | Op.VAR_QUERY.bit
            //Op.VAR_QUERY.bit
            //0
        ) {

            @Override
            public boolean intrin(Atomic x) {
                return
                    //erased types: intern these intrins for maximum premise key re-use
                    !(x instanceof Int) && !(x instanceof Atom.AtomChar)
                    && super.intrin(x);
            }

//            @Override
//            protected boolean intern(Atomic x) {
//                return //!(x instanceof Atom) ||
//                        !derivationFunctors.containsKey(x); //TODO better matcher
//            }

        };
    }

    static private void assertAnon(Term x, @Nullable Term y, @Nullable nars.Task cause) {
        TermTransformException e = null;
        if (y == null)
            e = new TermTransformException(x, null, "invalid Derivation Anon: null");
        else if (y instanceof Bool)
            e = new TermTransformException(x, y, "invalid Derivation Anon: Bool");
        else if (NAL.DEBUG && x instanceof Compound && x.op() != y.op())
            e = new TermTransformException(x, y, "invalid Derivation Anon: Op changed");
        else if (NAL.DEBUG && x.volume() != y.volume())
            e = new TermTransformException(x, y, "invalid Derivation Anon: Volume Changed");

        if (e != null) {
            if (cause != null)
                cause.delete();
            throw e;
        }
    }

    /**
     * setup for a new derivation.
     * returns false if the premise is invalid to derive
     * <p>
     * this is optimized for repeated use of the same task (with differing belief/beliefTerm)
     */
    public void reset(Task nextTask, final Task nextBelief, Term nextBeliefTerm) {
        this.parentCause = null; //invalidate
        this._task = resetTask(nextTask, this._task);
        this._beliefTerm = nextBeliefTerm;
        this._belief = resetBelief(nextBelief, nextBeliefTerm);
    }

    private Task resetBelief(Task nextBelief, final Term nextBeliefTerm) {

        if (nextBelief != null) {
            beliefTruth_at_Belief.set( nextBelief.truth() );

            long nextBeliefStart = nextBelief.start();
            beliefTruth_at_Task.set(
                (taskStart == ETERNAL || nextBeliefStart == ETERNAL) ?
                    beliefTruth_at_Belief : beliefAtTask(nextBelief)
            );

            if (beliefTruth_at_Task.is()) {
                double te = beliefTruth_at_Task.evi(), be = beliefTruth_at_Belief.evi();
                this.beliefTruth_mean_TaskBelief.freq(
                    (beliefTruth_at_Task.freq() * te +  beliefTruth_at_Belief.freq() * be)/(be+te)
                );
                this.beliefTruth_mean_TaskBelief.evi((be+te)/2);
            } else {
                this.beliefTruth_mean_TaskBelief.clear();
            }

            this.beliefStart = nextBelief.start();
            this.beliefEnd = nextBelief.end();

        } else {
            this.beliefTruth_at_Belief.clear();
            this.beliefTruth_at_Task.clear();
            this.beliefTruth_mean_TaskBelief.clear();
            this.beliefStart = this.beliefEnd = TIMELESS;
        }


        //TODO not whether to shift, but which variable (0..n) to shift against

        this.beliefTerm = deriver.beliefTerm(anon, _taskTerm, nextBeliefTerm, random);

        assertAnon(nextBeliefTerm, beliefTerm, nextBelief);

        return nextBelief;
    }



    @Nullable private Truth beliefAtTask(Task nextBelief) {
        @Nullable Truth t = nextBelief.truth(taskStart, taskEnd, dur(), true); //integration-calculated
        return t!=null && t.evi() >= eviMin ? t : null;
        //return t;
    }

    private Task resetTask(final Task nextTask, Task currentTask) {

        Term nextTaskTerm = nextTask.term();

        boolean sameTask = currentTask != null && currentTask.equals(nextTask);
        boolean sameTerm = sameTask || (currentTask != null && currentTask.term().equals(nextTaskTerm));

        if (sameTerm) {

            //roll back only as far as the unique task terms. we can re-use them as-is
            anon.rollback(taskUniqueAnonTermCount);


        } else {
            //have to re-anon completely
            anon.clear();

            this.taskTerm = anon.put(this._taskTerm = nextTaskTerm);

            assertAnon(nextTaskTerm, this.taskTerm, nextTask);

            this.taskUniqueAnonTermCount = anon.uniques();
        }


        if (!sameTask) {
            byte p = nextTask.punc();
            this.taskTruth.set(
                ((((this.taskPunc = p)) == BELIEF) || (p == GOAL)) ?
                    nextTask.truth() : null
            );
        }

        this.taskStart = nextTask.start();
        this.taskEnd = nextTask.end();
        return nextTask;
    }

    public boolean budget(Task task, Task belief) {
        float taskPri = task.priElseZero();
        float priSingle = taskPri;
        float priDouble = belief == null ?
                taskPri :
                NAL.DerivationPri.apply(taskPri, belief.priElseZero());

//        if (Param.INPUT_BUFFER_PRI_BACKPRESSURE && Math.max(priDouble, priSingle) < nar.input.priMin() /* TODO cache */)
//            return false;

        this.priSingle = priSingle;
        this.priDouble = priDouble;
        return true;
    }

    /**
     * called after protoderivation has returned some possible Try's
     */
    void preReady() {

        boolean eternalCompletely = (taskStart == ETERNAL) && (_belief == null || beliefStart == ETERNAL);
//        if (eternalCompletely) {
//            this.taskBelief_TimeIntersection[0] = this.taskBelief_TimeIntersection[1] = ETERNAL;
//        } else if ((_belief != null) && taskStart == ETERNAL) {
//            this.taskBelief_TimeIntersection[0] = beliefStart;
//            this.taskBelief_TimeIntersection[1] = beliefEnd;
//        } else if ((_belief == null) || beliefStart == ETERNAL) {
//            this.taskBelief_TimeIntersection[0] = taskStart;
//            this.taskBelief_TimeIntersection[1] = taskEnd;
//        } else /*if (_belief != null)*/ {
//            if (null == Longerval.intersectionArray(taskStart, taskEnd, beliefStart, beliefEnd, this.taskBelief_TimeIntersection)) {
//                this.taskBelief_TimeIntersection[0] = this.taskBelief_TimeIntersection[1] = TIMELESS; //no intersection
//            }
//        }
        this.temporal = !eternalCompletely || Occurrify.temporal(taskTerm) || Occurrify.temporal(beliefTerm);


        this.overlapSingle = _task.isCyclic();


        this.eviSingle = _task.isBeliefOrGoal() ? _task.evi() : 0;
        if (_belief != null) {
            this.overlapDouble =
                Stamp.overlapAny(this._task, _belief);
                //Stamp.overlap(this._task, _belief);
            this.eviDouble = this.eviSingle + _belief.evi();
        } else {
            this.overlapDouble = false; //N/A
        }

    }

    public void ready(int ttl) {

        this.stampDouble = stampSingle = null;

        setTTL(ttl);
    }


    @Override
    public final boolean match() {
        Predicate<Derivation> f = this.forEachMatch;
        return (f == null) || f.test(this);
    }

    /**
     * update some cached values that will be used for one or more derivation iterations
     */
    public Derivation next(Deriver d, What w) {

        NAR p = this.nar, n = w.nar;
        if (p != n) {
            this.reset(n);
        }

        this.deriver = d;
        this.what = w;
        //this.deriverMH = deriver.rules.what.compile();

        what.derivePri.premise(this);

        ditherDT =
                n.dtDither(); //FINE
                //w.dur(); //COARSE
        this.dtTolerance = uniSubstFunctor.u.dtTolerance = premiseUnify.dtTolerance =
                //n.dtDither(); //FINE
                //Math.round(n.dtDither() * n.unifyTimeToleranceDurs.floatValue()); //COARSE
                Math.round(w.dur() * n.unifyTimeToleranceDurs.floatValue()); //COARSE
                //Math.max(n.dtDither(), Math.round(w.dur() * n.unifyTimeToleranceDurs.floatValue())); //COARSE

        this.confMin = n.confMin.conf();
        this.eviMin = n.confMin.evi();
        this.termVolMax = n.termVolMax.intValue();
        this.time = n.time();
        this.dur = w.dur();
        return this;
    }

    @Nullable
    private long[] evidenceSingle() {
        if (stampSingle == null) {
            stampSingle = _task.stamp();
        }
        return stampSingle;
    }

    @Nullable
    private long[] evidenceDouble() {
        if (stampDouble == null) {
            double te, be;
            if (taskPunc == BELIEF || taskPunc == GOAL) {
                te = taskTruth.evi();
                be = beliefTruth_at_Belief.evi(); //TODO use appropriate beliefTruth projection
            } else {
                te = _task.priElseZero();
                be = _belief.priElseZero();
            }
            if (temporal && taskStart!=ETERNAL && beliefStart!=ETERNAL) {
                te *= _task.range();
                be *= _belief.range();
            }

            double tbe = te + be;
            double tb = tbe < ScalarValue.EPSILON ? 0.5f : te / tbe;

            long[] e = Stamp.merge(_task.stamp(), _belief.stamp(), (float) tb, random);
            if (stampDouble == null || !Arrays.equals(e, stampDouble))
                this.stampDouble = e;
            return e;
        } else {
            return stampDouble;
        }
    }

    @Override
    public String toString() {
        return _task + " " + (_belief != null ? _belief : beliefTerm)
                + ' ' + super.toString();
    }

    /**
     * include any .clear() for data structures in case of emergency we can continue to assume they will be clear on next run()
     */

    private void reset(NAR n) {
        this.nar = n;

        _task = _belief = null;
        taskPunc = 0;
        parentCause = null;
        taskTerm = beliefTerm = null;

        truth.clear();
        taskTruth.clear();
        beliefTruth_at_Task.clear();
        beliefTruth_at_Belief.clear();

        ttl = 0;
        taskUniqueAnonTermCount = 0;
        temporal = false;
//        taskBelief_TimeIntersection[0] = taskBelief_TimeIntersection[1] = TIMELESS;

        //clear();
        //anon.clear();
        //retransform.clear();
        //occ.clear();
        //taskStamp.clear();
        //canCollector.clear();

        this.premiseUnify.random(this.random = n.random());
        this.derivationFunctors = DerivationFunctors.get(Derivation.this);
        this.transformDerived = new DerivationTransform();

    }

    /**
     * resolve a target (ex: task target or belief target) with the result of 2nd-layer substitutions
     */
    public Term retransform(Term x) {
        Term y = x;

        try {
            y = y.replace(retransform); //substitution/unification derivation functors only
        } catch (TermTransformException tte) {
            if (NAL.DEBUG)
                throw tte;
            else
                return x; //ignore
        }

        if (y != x && !y.op().eventable)
            return x; //dont bother
        else
            return y;
    }

    public final void derive(Task t) {
        try (var __ = nar.emotion.derive_F_Remember.time()) {

            what.accept(t);
            nar.emotion.deriveTask.increment();

            use(NAL.derive.TTL_COST_DERIVE_TASK);

        }
    }

    /** returns appropriate Emotion counter representing the result state  */
    public FastCounter derive(Premise p, int deriveTTL) {

        short[] can;

        try (var __ = nar.emotion.derive_C_Pre.time()) {

            can = p.apply(this);
            if (can.length == 0)
                return nar.emotion.premiseUnderivable1;

        }

        int valid = 0, lastValid = -1;

        try (var __ = nar.emotion.derive_D_Truthify.time()) {
            this.preReady();


            DeriveAction[] branch = this.deriver.rules.branch;

            PostDerivable[] post = this.post;
            for (int i = 0; i < can.length; i++) {
                if ((post[i].priSet(branch[can[i]], this)) > Float.MIN_NORMAL) {
                    lastValid = i;
                    valid++;
                }
            }
            if (valid == 0)
                return nar.emotion.premiseUnderivable2;

            if (valid > 1 && valid < can.length) {
                //sort the valid to the first slots for fastest roulette iteration on the contiguous valid subset
                Arrays.sort(post, 0, can.length, sortByPri);
            } //otherwise any order here is valid

            this.ready(deriveTTL); //first come first serve, maybe not ideal
        }

        try (var __ = nar.emotion.derive_E_Run.time()) {

            switch (valid) {
                case 1:
                    //optimized 1-option case
                    //while (post[lastValid].run()) { }
                    post[lastValid].run();
                    break;
                default:

//                int j;
//                do {
//                    j = Roulette.selectRoulette(valid, i -> post[i].pri, d.random);
//                } while (post[j].run());

                    float[] pri = new float[valid];
                    for (int i = 0; i < valid; i++)
                        pri[i] = post[i].pri;
                    MutableRoulette.run(pri, this.random, wi -> 0, i -> post[i].run());
                    break;
            }
            return nar.emotion.premiseRun;
        }
    }



    private static final Comparator<? super PostDerivable> sortByPri = (a, b)->{
        if (a==b) return 0;
        int i = Float.compare(a.pri, b.pri);
        return i != 0 ? -i : Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
    };

    public boolean doubt(float ratio) {
        return Util.equals(ratio, 1f) || concTruthEvi(ratio * truth.evi());
    }

    private boolean concTruthEvi(double e) {
        if (e >= eviMin) {
            this.truth.evi(e);
            return true;
        }
        return false;
    }

    /**
     * punctuation equalizer: value factor for the conclusion punctuation type [0..1.0]
     */
    public final float preAmp(byte concPunc) {
        return what.derivePri.preAmp(concPunc);
    }

    public final float parentPri() {
        return (single ? priSingle : priDouble);
    }

//    public float parentEvi() {
//
//        if (taskEvi!=taskEvi) {
//            this.taskEvi = taskTruth != null ? TruthIntegration.evi(_task, time, dur) : 0;
//        }
//        if (beliefEvi!=beliefEvi) {
//            this.beliefEvi = _belief != null ? TruthIntegration.value(_belief, time, dur) : 0;
//        }
//        return concSingle ? taskEvi : (taskEvi + beliefEvi);
//    }

    @Override
    public final DerivationTransform transform() {
        return this.transformDerived;
    }

    /**
     * current NAR time, set at beginning of derivation
     */
    public final long time() {
        return time;
    }

    public final float dur() {
        return dur;
    }

    public final NAR nar() {
        return nar;
    }

    public long[] evidence() {
        return single ? evidenceSingle() : evidenceDouble();
    }

    public short[] parentCause() {
        if (parentCause == null) {


            int causeCap = NAL.causeCapacity.intValue();
            this.parentCause =
                    CauseMerge.limit(
                            _belief != null ?
                                    CauseMerge.Append.merge(causeCap - 1 /* for channel to be appended */, _task, _belief) :
                                    _task.why(), causeCap - 1);
//        if (parentCause.length >= causeCap)
//            throw new WTF();
        }
        return parentCause;
    }

    @Override
    public ShortBuffer preDerive() {
        canCollector.clear();

        deriver.rules.what.test(this);
//        try {
//            deriverMH.invoke(this);
//        } catch (Throwable throwable) {
//            throwable.printStackTrace();
//        }

        return canCollector;
    }

    public final double evi() {
        return single ? eviSingle : eviDouble;
    }

    public final boolean isBeliefOrGoal() {
        byte p = this.punc;
        return p == BELIEF || p == GOAL;
    }

    abstract static class AbstractInstantFunctor1 extends AbstractInlineFunctor1 implements InstantFunctor<Evaluation> {

        AbstractInstantFunctor1(String atom) {
            super(atom);
        }
    }


    /**
     * should be created whenever a different NAR owns this Derivation instance, if ever
     */
    public final class DerivationTransform extends MyUnifyTransform {

        @Override
        protected final Term resolveVar(nars.term.Variable x) {
            throw new UnsupportedOperationException(/* not used */);
        }

        /**
         * only returns derivation-specific functors.  other functors must be evaluated at task execution time
         */
        @Override
        public final Term applyAtomic(Atomic a) {


            Term b;
            if (a instanceof Variable) {

                b = Derivation.this.resolveTermRecurse(a);

            } else if (a instanceof Atom) {

                if (a == TaskTerm)
                    return taskTerm;
                else if (a == BeliefTerm)
                    return beliefTerm;

                b = derivationFunctors.get(a);

                if (NAL.DEBUG) {
                    if (b == TaskTerm)
                        throw new WTF("should have been detected earlier"); //return taskTerm;
                    else if (b == BeliefTerm)
                        throw new WTF("should have been detected earlier"); //return beliefTerm;
                }

            } else
                return a;

            return b != null ? b : a;
        }

        @Override
        public final boolean evalInline() {
            return true;
        }

    }


}


//            if (nextBeliefStart == ETERNAL) {
//
//                beliefTruth_at_Task.set( beliefTruth_at_Belief ); /* = */
//            } else {
//
//
////                if (NAL.derive.ETERNALIZE_BELIEF_PROJECTION && !nextBelief.equals(_task) &&
////                    (!NAL.derive.ETERNALIZE_BELIEF_PROJECTION_ONLY_IF_SUBTHRESH || !beliefTruth_at_Task.set())) {
////
////                    double eScale = 1;
////                            //(!taskTruth.set()) ? 1 : 1;
////                                //taskTruth.conf();
////                                //Math.min(1, beliefTruth_at_Belief.evi() / taskTruth.evi());
////
////                    Truth beliefTruth_eternalized = beliefTruth_at_Belief.eternalized(eScale, eviMin, null /* dont dither */);
////                    if (beliefTruth_eternalized!=null && beliefTruth_eternalized.evi() >= eviMin) {
////                        if (Truth.stronger(beliefTruth_eternalized, beliefTruth_at_Task) == beliefTruth_eternalized) {
////
////
////                            if (NAL.derive.ETERNALIZE_BELIEF_PROJECTION_AND_ETERNALIZE_BELIEF_TIME) {
////                                nextBeliefStart = nextBeliefEnd = ETERNAL;
////                                //nextBeliefStart = taskStart; nextBeliefEnd = taskEnd;
////                            } else
////                                nextBeliefEnd = nextBelief.end();
////
////                            nextBelief = SpecialTruthAndOccurrenceTask.the(nextBelief,
////                                this.beliefTruth_at_Task.set(beliefTruth_eternalized),
////                                nextBeliefStart, nextBeliefEnd
////							);
////                        }
////                    }
////
////                }
////
//            }
