package nars.derive.model;

import jcog.Util;
import jcog.math.Longerval;
import jcog.pri.ScalarValue;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.What;
import nars.control.CauseMerge;
import nars.derive.Deriver;
import nars.derive.op.Occurrify;
import nars.derive.op.UnifyMatchFork;
import nars.derive.premise.PremiseSource;
import nars.derive.premise.PremiseUnify;
import nars.eval.Evaluation;
import nars.op.Replace;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.task.proxy.SpecialTruthAndOccurrenceTask;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.anon.AnonWithVarShift;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.buffer.DirectTermBuffer;
import nars.term.buffer.TermBuffer;
import nars.term.functor.AbstractInlineFunctor1;
import nars.term.util.TermTransformException;
import nars.term.util.transform.InstantFunctor;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.func.TruthFunc;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
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
    /**
     * short-term premise buffer with novelty filter
     */
    public final PremiseSource premises =
            //new PremiseBuffer();
            new PremiseSource.DefaultPremiseSource();

    public final AnonWithVarShift anon;
    public final UniSubst uniSubstFunctor = new UniSubst(this);
    /**
     * second layer additional substitutions
     */
    public final Map<Term, Term> retransform = new MapAdapter<>(new UnifiedMap<>()) {
        @Override
        public Term put(Term key, Term value) {
            return key.equals(value) ? null : delegate.put(key, value);
        }
    };
    public final Occurrify occ = new Occurrify(this);
    public final long[] taskBelief_TimeIntersection = new long[2];
    final Functor polarizeTask = new AbstractInstantFunctor1("polarizeTask") {
        @Override
        protected Term apply1(Term arg) {
            Truth t = Derivation.this.taskTruth;
//            return arg.negIf(t!=null ? t.isNegative() : random.nextBoolean());
            if (t == null)
                throw new NullPointerException();
            return arg.negIf(t.isNegative());
        }
    };
    final Functor polarizeBelief = new AbstractInstantFunctor1("polarizeBelief") {
        @Override
        protected Term apply1(Term arg) {
            Truth b = Derivation.this.beliefTruth_at_Belief;
            return arg.negIf(b!=null ? b.isNegative() : random.nextBoolean());
//            if (b == null)
//                throw new NullPointerException();
//            return arg.negIf(b.isNegative());
        }
    };
    /**
     * cant be inline since the value will be cached and repeated
     */
//    final Functor polarizeRandom = Functor.f1("polarizeRandom", (arg)->random.nextBoolean() ? arg : arg.neg());
    final Functor polarizeRandom = new AbstractInlineFunctor1("polarizeRandom") {
        @Override
        protected Term apply1(Term arg) {
            return arg.negIf(random.nextBoolean());
        }
    };
    /**
     * populates retransform map
     */
    final Replace substituteFunctor = new Replace(Derivation.SUBSTITUTE) {

        @Override
        public @Nullable Term apply(Evaluation e, Subterms xx) {
            Term input = xx.sub(0), replaced = xx.sub(1), replacement = xx.sub(2);

            Term y = Replace.apply(xx, input, replaced, replacement);
            if (y != null)
                retransform.put(replaced, replacement);

            return y;
        }
    };
    private final TermBuffer termBuilder = new TermBuffer();
    private final TermBuffer directTermBuilder = new DirectTermBuffer();


    /**
     * current context
     */
    public transient What what = null;
    public NAR nar;
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

    @Deprecated public transient boolean concSingle;
    @Deprecated public transient Truth concTruth;
    @Deprecated public transient byte concPunc;
    @Deprecated public transient TruthFunc truthFunction;

    public transient Task _task, _belief;
    public DerivationTransform transformDerived;
    private transient short[] parentCause;
    private transient long[] evidenceDouble, evidenceSingle;
    private transient int taskUniqueAnonTermCount;

    /**
     * these represent the maximum possible priority of the derivation.
     * the maximum constraint is a contract ensuring the range of priority
     * can be predicted for deciding
     * whether to attempt before beginning,
     * or whether to continue deriving during the procedure.
     */
    private transient float priSingle, priDouble;

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

        this.anon = new AnonWithVarShift(ANON_INITIAL_CAPACITY, Op.VAR_DEP.bit | Op.VAR_QUERY.bit) {

            @Override
            protected boolean intern(Atomic x) {
                return !(x instanceof Atom) ||
                        transformDerived.derivationFunctors.apply(x)==null; //TODO better matcher
            }

            @Override
            protected final Term putCompound(Compound x) {
                putOrGet = true;
                return x.transform(this, /*directTermBuilder*/  termBuilder , NAL.term.COMPOUND_VOLUME_MAX);
            }

            @Override
            protected final Term getCompound(Compound x) {
                putOrGet = false;
                return x.transform(this, termBuilder, NAL.term.COMPOUND_VOLUME_MAX);
            }

        };
    }

    static private void assertAnon(Term x, @Nullable Term y, @Nullable nars.Task cause) {
        TermTransformException e = null;
        if (y == null)
            e = new TermTransformException("invalid Derivation Anon: null", x, y);
        else if (y instanceof Bool)
            e = new TermTransformException("invalid Derivation Anon: Bool", x, y);
        else if (NAL.DEBUG && x instanceof Compound && x.op() != y.op())
            e = new TermTransformException("invalid Derivation Anon: Op changed", x, y);
        else if (NAL.DEBUG && x.volume() != y.volume())
            e = new TermTransformException("invalid Derivation Anon: Volume Changed", x, y);

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
        this._task = resetTask(nextTask, this._task);
        this._belief = resetBelief(nextBelief, nextBeliefTerm);
    }

    private Task resetBelief(Task nextBelief, final Term nextBeliefTerm) {

        beliefTruth_at_Task = beliefTruth_at_Belief = null;

        long nextBeliefEnd;

        if (nextBelief != null) {
            long nextBeliefStart = nextBelief.start();
            if (nextBeliefStart == ETERNAL) {
                beliefTruth_at_Task = beliefTruth_at_Belief = nextBelief.truth();
            } else {

                this.beliefTruth_at_Belief = nextBelief.truth();
                if (beliefTruth_at_Belief == null)
                    throw new NullPointerException("null belief truth");

                boolean taskEternal = taskStart == ETERNAL;
                if (taskEternal) {

//                    if (!nextBelief.isEternal() && nextBelief.start() < time()) {
//                        long now = time();
//                        float d = dur();
//                        long presentStart = Math.round(now - d / 2);
//                        long presentEnd = Math.round(now + d / 2);
//                        _task = new SpecialOccurrenceTask(_task, taskStart = presentStart, taskEnd = presentEnd);
//                        this.beliefTruth_at_Task = beliefAtTask(nextBelief);
//                    } else
                        this.beliefTruth_at_Task = beliefTruth_at_Belief;


                } else {
                    this.beliefTruth_at_Task = beliefAtTask(nextBelief);
                }

                if (NAL.derive.ETERNALIZE_BELIEF_PROJECTION && !nextBelief.equals(_task) && (!NAL.derive.ETERNALIZE_BELIEF_PROJECTION_ONLY_IF_SUBTHRESH || beliefTruth_at_Task==null)) {

                    Truth beliefTruth_eternalized = beliefTruth_at_Belief.eternalized(1, eviMin, null /* dont dither */);
                    if (beliefTruth_eternalized!=null && beliefTruth_eternalized.evi() > eviMin) {
                        if (Truth.stronger(beliefTruth_eternalized, beliefTruth_at_Task) == beliefTruth_eternalized) {


                            if (NAL.derive.ETERNALIZE_BELIEF_PROJECTION_AND_ETERNALIZE_BELIEF_TIME)
                                nextBeliefStart = nextBeliefEnd = ETERNAL;
                            else
                                nextBeliefEnd = nextBelief.end();

                            nextBelief = new SpecialTruthAndOccurrenceTask(nextBelief, nextBeliefStart, nextBeliefEnd,
                                    false,
                                    this.beliefTruth_at_Task = beliefTruth_eternalized
                            );
                        }
                    }

                }

            }

            if (beliefTruth_at_Task == null && beliefTruth_at_Belief == null)
                nextBelief = null;

        }


        Term _beliefTerm;
        if (nextBelief != null) {
            this.beliefStart = nextBelief.start();
            this.beliefEnd = nextBelief.end();
            this.beliefTerm = anon.putShift(_beliefTerm = nextBelief.term(), taskTerm);
        } else {
            this.beliefTruth_at_Belief = this.beliefTruth_at_Task = null;

//            this.taskStart = _task.start();
//            this.taskEnd = _task.end(); //HACK reset task start in case it was changed
            this.beliefStart = this.beliefEnd = TIMELESS;

            _beliefTerm = nextBeliefTerm;
            this.beliefTerm =
                    !(nextBeliefTerm instanceof Variable) ?
                            anon.putShift(nextBeliefTerm, taskTerm) :
                            anon.put(nextBeliefTerm); //unshifted, since the target may be structural
        }

        assertAnon(_beliefTerm, beliefTerm, nextBelief);


        return nextBelief;
    }

    @Nullable
    private Truth beliefAtTask(Task nextBelief) {
        Truth t = !NAL.derive.BELIEF_PROJECTION_CLASSIC ?
            nextBelief.truth(taskStart, taskEnd, dur()) //integration-calculated
            :
            nextBelief.truth(time(), _task) //classic opennars projection
        ;
        return t.evi() < eviMin ? null : t;
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

            this.taskTerm = anon.put(nextTaskTerm);

            assertAnon(nextTaskTerm, this.taskTerm, nextTask);

            this.taskUniqueAnonTermCount = anon.uniques();
        }


        if (!sameTask) {


            this.taskPunc = nextTask.punc();
            if ((taskPunc == BELIEF || taskPunc == GOAL)) {
                this.taskTruth = nextTask.truth();

                assert (taskTruth != null);
            } else {
                this.taskTruth = null;
            }

        }

        this.taskStart = nextTask.start();
        this.taskEnd = nextTask.end();

        return nextTask;
    }

    private float pri(Task t) {
        float p = t.priElseZero();
        return
                p;
        //t.isEternal() ? p : Param.evi(p, t.minTimeTo(nar.time()), nar.dur());
    }

    public boolean budget(Task task, Task belief) {
        float taskPri = pri(task);
        float priSingle = taskPri;
        float priDouble = belief == null ?
                taskPri :
                NAL.DerivationPri.apply(taskPri, pri(belief));

//        if (Param.INPUT_BUFFER_PRI_BACKPRESSURE && Math.max(priDouble, priSingle) < nar.input.priMin() /* TODO cache */)
//            return false;

        this.priSingle = priSingle;
        this.priDouble = priDouble;
        return true;
    }

    /**
     * called after protoderivation has returned some possible Try's
     */
    public void preReady() {

        boolean eternalCompletely = (taskStart == ETERNAL) && (_belief == null || beliefStart == ETERNAL);
        if (eternalCompletely) {
            this.taskBelief_TimeIntersection[0] = this.taskBelief_TimeIntersection[1] = ETERNAL;
        } else if ((_belief != null) && taskStart == ETERNAL) {
            this.taskBelief_TimeIntersection[0] = beliefStart;
            this.taskBelief_TimeIntersection[1] = beliefEnd;
        } else if ((_belief == null) || beliefStart == ETERNAL) {
            this.taskBelief_TimeIntersection[0] = taskStart;
            this.taskBelief_TimeIntersection[1] = taskEnd;
        } else if (_belief != null) {
            if (null == Longerval.intersectionArray(taskStart, taskEnd, beliefStart, beliefEnd, this.taskBelief_TimeIntersection)) {
                this.taskBelief_TimeIntersection[0] = this.taskBelief_TimeIntersection[1] = TIMELESS; //no intersection
            }
        }
        this.temporal = !eternalCompletely || Occurrify.temporal(taskTerm) || Occurrify.temporal(beliefTerm);


        this.overlapSingle = _task.isCyclic();


        if (_belief != null) {
            this.overlapDouble = Stamp.overlap(this._task, _belief);
        } else {
            this.overlapDouble = false; //N/A
        }

    }

    public void ready(short[] can, int ttl) {

        this.parentCause = null; //invalidate

        this.forEachMatch = null;
        this.concTruth = null;
        this.concPunc = 0;
        this.concSingle = false;
        this.truthFunction = null;
        this.evidenceDouble = evidenceSingle = null;

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
            this.reset();
            this.nar = n;
            this.premiseUnify.random(this.random = n.random());
            this.transformDerived = new DerivationTransform();
        }

        this.deriver = d;
        this.what = w;

        what.derivePri.premise(this);

        ditherDT =
                n.dtDither(); //FINE
                //w.dur(); //COARSE
        this.dtTolerance = uniSubstFunctor.u.dtTolerance = premiseUnify.dtTolerance =
                //n.dtDither(); //FINE
                Math.round(w.dur() * n.intermpolationRangeLimit.floatValue()); //COARSE

        this.confMin = n.confMin.floatValue();
        this.eviMin = n.confMin.evi();
        this.termVolMax = n.termVolMax.intValue();
        return this;
    }

    @Nullable
    private long[] evidenceSingle() {
        if (evidenceSingle == null) {
            evidenceSingle = _task.stamp();
        }
        return evidenceSingle;
    }

    @Nullable
    private long[] evidenceDouble() {
        if (evidenceDouble == null) {
            double te, be, tb;
            if (taskPunc == BELIEF || taskPunc == GOAL) {

                te = taskTruth.evi();
                be = beliefTruth_at_Belief.evi(); //TODO use appropriate beliefTruth projection
                tb = te / (te + be);
            } else {

                te = _task.priElseZero();
                be = _belief.priElseZero();
                double tbe = te + be;
                tb = tbe < ScalarValue.EPSILON ? 0.5f : te / tbe;
            }
            long[] e = Stamp.merge(_task.stamp(), _belief.stamp(), (float) tb, random);
            if (evidenceDouble == null || !Arrays.equals(e, evidenceDouble))
                this.evidenceDouble = e;
            return e;
        } else {
            return evidenceDouble;
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

    private Derivation reset() {
        _task = _belief = null;
        parentCause = null;
        concTruth = null;
        taskTerm = beliefTerm = null;
        taskTruth = beliefTruth_at_Task = beliefTruth_at_Belief = null;

        ttl = 0;
        taskUniqueAnonTermCount = 0;
        temporal = false;
        taskBelief_TimeIntersection[0] = taskBelief_TimeIntersection[1] = TIMELESS;
        nar = null;

        //clear();
        //anon.clear();
        //retransform.clear();
        //occ.clear();
        //taskStamp.clear();
        //canCollector.clear();

        return this;
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

    public final nars.Task add(Task t) {
        return what.put(t);
    }

    public boolean concTruthEviMul(float ratio, boolean eternalize) {
//        if (concTruth == null)
//            return true; //not belief/goal

        if (Util.equals(ratio, 1f))
            return true; //no change

        double e = ratio * concTruth.evi();
        if (eternalize)
            e = Math.max(concTruth.eviEternalized(), e);
        return concTruthEvi(e);
    }

    private boolean concTruthEvi(double e) {
        return e >= eviMin && (this.concTruth = PreciseTruth.byEvi(concTruth.freq(), e)) != null;
    }

    /**
     * punctuation equalizer: value factor for the conclusion punctuation type [0..1.0]
     */
    public final float preAmp(byte concPunc) {
        return what.derivePri.preAmp(concPunc);
    }

    public final float parentPri() {
        return (concSingle ? priSingle : priDouble);
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
        return nar().time();
    }

    public final float dur() {
        return what.dur();
    }

    public NAR nar() {
        return nar;
    }

    public long[] evidence() {
        return concSingle ? evidenceSingle() : evidenceDouble();
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
    public short[] preDerive() {
        if (!canCollector.isEmpty()) canCollector.clear();
        deriver.rules.what.test(this);
        return canCollector.isEmpty() ? ArrayUtil.EMPTY_SHORT_ARRAY : canCollector.toArray();
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

        private final Function<Atomic, Term> derivationFunctors = DerivationFunctors.get(Derivation.this);
//        public transient Function<Variable, Term> xy = null;

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

                b = Derivation.this.resolveTerm(a, true);
                //b = resolveVar((Variable)a);

            } else if (a instanceof Atom) {

                b = derivationFunctors.apply(a);

                if (b == TaskTerm)
                    return taskTerm;
                else if (b == BeliefTerm)
                    return beliefTerm;

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


