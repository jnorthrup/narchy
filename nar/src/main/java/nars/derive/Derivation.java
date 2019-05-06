package nars.derive;

import jcog.Util;
import jcog.WTF;
import jcog.data.set.MetalLongSet;
import jcog.math.Longerval;
import jcog.pri.ScalarValue;
import nars.NAL;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.What;
import nars.control.CauseMerge;
import nars.derive.op.MatchFork;
import nars.derive.op.Occurrify;
import nars.eval.Evaluation;
import nars.op.Subst;
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
import nars.term.functor.AbstractInlineFunctor1;
import nars.term.functor.AbstractInlineFunctor2;
import nars.term.util.TermTransformException;
import nars.term.util.transform.TermTransform;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.func.TruthFunc;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends PreDerivation {

    public static final ThreadLocal<Derivation> derivation = ThreadLocal.withInitial(Derivation::new);
    protected final static Logger logger = LoggerFactory.getLogger(Derivation.class);

    public static final Atomic Task = Atomic.the("task");
    public static final Atomic Belief = Atomic.the("belief");
    public static final Atomic TaskTerm = Atomic.the("taskTerm");
    public static final Atomic BeliefTerm = Atomic.the("beliefTerm");
    private final static int ANON_INITIAL_CAPACITY = 16;


    final UnifyPremise unifyPremise = new UnifyPremise();

    public final MatchFork termBuilder = new MatchFork();

    private long timePrev = Long.MIN_VALUE;

    {
        unifyPremise.commonVariables = NAL.premise.PREMISE_UNIFY_COMMON_VARIABLES;
    }


//    @Deprecated public final ArrayHashSet<Term> atomMatches = new ArrayHashSet();
//    @Deprecated public TopN<TaskLink> atomTangent = new TopN<>(new TaskLink[64], (FloatFunction<TaskLink>) ScalarValue::pri);

    public final AnonWithVarShift anon;

    public final UniSubst uniSubst = new UniSubst(this);

    /** current context */
    public transient What what = null;

    final Functor polarizeTask = new AbstractInlineFunctor1("polarizeTask") {
        @Override
        protected Term apply1(Term arg) {
            Truth t = Derivation.this.taskTruth;
            if (t == null)
                throw new WTF("polarizeTask not applicable without taskTruth"); //return Null;  //TODO WTF
            return t.isPositive() ? arg : arg.neg();
        }
    };

    final Functor polarizeRandom = new AbstractInlineFunctor1("polarizeRandom") {
        @Override
        protected Term apply1(Term arg) {
            return random.nextBoolean() ? arg : arg.neg();
        }
    };

    final Functor polarizeBelief = new AbstractInlineFunctor1("polarizeBelief") {
        @Override
        protected Term apply1(Term arg) {
            Truth t = Derivation.this.beliefTruthBelief;
            if (t == null)
                return Null;  //TODO WTF
            return t.isPositive() ? arg : arg.neg();
        }
    };

    @Deprecated
    final Functor polarizeFunc = new AbstractInlineFunctor2("polarize") {
        @Override
        protected Term apply(Term subterm, Term whichTask) {
            if (subterm instanceof Bool)
                return subterm;

            Truth compared;
            if (whichTask.equals(Task)) {
                compared = taskTruth;
            } else {
                //assert(whichTask.equals(Belief))
                compared = beliefTruthBelief;
            }
            if (compared == null)
                return Null;
            return compared.isPositive() ? subterm : subterm.neg();
        }
    };

    public NAR nar;


    /**
     * second layer additional substitutions
     */
    public final Map<Term, Term> retransform = new UnifiedMap<>() {
        @Override
        public Term put(Term key, Term value) {
            if (key.equals(value))
                return null;

            return super.put(key, value);
        }

    };
    final Subst mySubst = new Subst("substitute") {

        @Override
        public @Nullable Term apply(Evaluation e, Subterms xx) {
            Term input = xx.sub(0);
            Term replaced = xx.sub(1);
            Term replacement = xx.sub(2);
            if (replaced.equals(replacement))
                return input;

            Term y = apply(xx, input, replaced, replacement);

            if (y != null && !(y instanceof Bool)) {
                retransform.put(replaced, replacement);
            }
            return y;
        }
    };


    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    @Deprecated public Predicate<Derivation> forEachMatch;

    public transient float confMin;
    public transient double eviMin;
    public transient int termVolMax;


    public final Occurrify occ = new Occurrify(this);

    /**
     * whether either the task or belief are events and thus need to be considered with respect to time
     */
    public transient boolean temporal, temporalTerms;

    public transient TruthFunc truthFunction;
    public transient int ditherDT;

    public Deriver deriver;



    /**
     * precise time that the task and belief truth are sampled
     */
    public transient long taskStart, taskEnd, beliefStart, beliefEnd; //TODO taskEnd, beliefEnd

    public final long[] taskBeliefTimeIntersects = new long[2];

    private transient Term _beliefTerm;
    private transient long[] evidenceDouble, evidenceSingle;
    private transient int taskUniques;
    private final transient MetalLongSet taskStamp = new MetalLongSet(NAL.STAMP_CAPACITY);
    public transient boolean overlapDouble, overlapSingle;


    /**
     * these represent the maximum possible priority of the derivation.
     * the maximum constraint is a contract ensuring the range of priority
     * can be predicted for deciding
     * whether to attempt before beginning,
     * or whether to continue deriving during the procedure.
     */
    private transient float priSingle, priDouble;


    public transient short[] parentCause;
    public transient boolean concSingle;
    public transient float parentVoluplexitySum;

    @Deprecated public transient Truth concTruth;
    public transient byte concPunc;

    public transient Task _task, _belief;



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
            protected Term putCompound(Compound x) {
                return super.putCompound(x);

                //TODO needs   @Override
                //        protected boolean evalInline() {
                //            return false; //TEMPORARY
                //        }
                //termBuilder.clear();
                //return applyCompoundLazy(x, termBuilder, Op.terms, NAL.term.COMPOUND_VOLUME_MAX);
            }
            @Override
            protected Term getCompound(Compound x) {
                return super.getCompound(x);

                //TODO needs   @Override
                //        protected boolean evalInline() {
                //            return false; //TEMPORARY
                //        }
                //termBuilder.clear();
                //return applyCompoundLazy(x, termBuilder, Op.terms, NAL.term.COMPOUND_VOLUME_MAX);
            }

            @Override
            public boolean evalInline() {
                return false;
            }
        };
    }

    public DerivationTransform transform;


    /**
     * setup for a new derivation.
     * returns false if the premise is invalid to derive
     * <p>
     * this is optimized for repeated use of the same task (with differing belief/beliefTerm)
     */
    public void reset(Task nextTask, final Task nextBelief, Term nextBeliefTerm) {
        this._task = resetTask(nextTask, this._task);
        this._belief = resetBelief(nextBelief, nextBeliefTerm);
        this.occ.clear();
    }

    private Task resetBelief(Task nextBelief, Term nextBeliefTerm) {

        if (nextBelief != null) {

            if (nextBelief.isEternal()) {
                beliefTruthTask = beliefTruthBelief = nextBelief.truth();
            } else {

                this.beliefTruthBelief = nextBelief.truth();

                boolean taskEternal = taskStart == ETERNAL;

                this.beliefTruthTask =
                        taskEternal ?
                                beliefTruthBelief :
                                nextBelief.truth(taskStart, taskEnd, dur());

                if (NAL.ETERNALIZE_BELIEF_PROJECTED_IN_DERIVATION
                        && !taskEternal
                        && beliefTruthBelief!=null
                        && (beliefTruthTask == null || !beliefTruthTask.equals(beliefTruthBelief))) {
                    this.beliefTruthTask = Truth.stronger(
                            beliefTruthTask,
                            beliefTruthBelief.eternalized(1, NAL.truth.TRUTH_EVI_MIN, null /* dont dither */)
                    );
                }
            }

            if (beliefTruthTask == null && beliefTruthBelief == null)
                nextBelief = null;
        }


        if (nextBelief != null) {
            this.beliefStart = nextBelief.start(); this.beliefEnd = nextBelief.end();
            this.beliefTerm = anon.putShift(this._beliefTerm = nextBelief.term(), taskTerm);
        } else {
            this.beliefTruthBelief = this.beliefTruthTask = null;
            this.beliefStart = this.beliefEnd = TIMELESS;
            this._beliefTerm = nextBeliefTerm;
            this.beliefTerm =
                    !(nextBeliefTerm instanceof Variable) ?
                            anon.putShift(nextBeliefTerm, taskTerm) :
                            anon.put(nextBeliefTerm); //unshifted, since the target may be structural
        }
        assertAnon(_beliefTerm, beliefTerm, nextBelief);



        return nextBelief;
    }

    private Task resetTask(Task nextTask, Task currentTask) {

        Term nextTaskTerm = nextTask.term();

        if (currentTask != null && currentTask.term().equals(nextTaskTerm)) {

            //roll back only as far as the unique task terms. we can re-use them as-is
            anon.rollback(taskUniques);

        } else {
            //have to re-anon completely
            anon.clear();

            this.taskTerm = anon.put(nextTaskTerm);

            assertAnon(nextTaskTerm, this.taskTerm, nextTask);

            this.taskUniques = anon.uniques();
        }


        if (currentTask == null || currentTask != nextTask) {



            this.taskStamp.clear(); //force (re-)compute in post-derivation stage

            this.taskPunc = nextTask.punc();
            if ((taskPunc == BELIEF || taskPunc == GOAL)) {
                this.taskTruth = nextTask.truth();

                assert (taskTruth != null);
            } else {
                this.taskTruth = null;
            }

            this.taskStart = nextTask.start();
            this.taskEnd = nextTask.end();
        }

        return nextTask;
    }

    static private void assertAnon(Term x, @Nullable Term y, @Nullable nars.Task cause) {
        TermTransformException e = null;
        if (y == null)
            e = new TermTransformException(x, y, "invalid Derivation Anon: null");
        if (y instanceof Bool)
            e = new TermTransformException(x, y, "invalid Derivation Anon: Bool");
        if (x instanceof Compound && x.op() != y.op())
            e = new TermTransformException(x, y, "invalid Derivation Anon: Op changed");
        if (x.volume() != y.volume())
            e = new TermTransformException(x, y, "invalid Derivation Anon: Volume Changed");
        if (e != null) {
            if (cause!=null)
                cause.delete();
            throw e;
        }
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
    public void ready(short[] can, int ttl) {

        if (taskStart == ETERNAL && (_belief == null || beliefStart == ETERNAL)) {
            this.taskBeliefTimeIntersects[0] = this.taskBeliefTimeIntersects[1] = ETERNAL;
        } else if ((_belief != null) && taskStart == ETERNAL) {
            this.taskBeliefTimeIntersects[0] = beliefStart;
            this.taskBeliefTimeIntersects[1] = beliefEnd;
        } else if ((_belief == null) || beliefStart == ETERNAL) {
            this.taskBeliefTimeIntersects[0] = taskStart;
            this.taskBeliefTimeIntersects[1] = taskEnd;
        } else if (_belief != null) {
            if (null == Longerval.intersectionArray(taskStart, taskEnd, beliefStart, beliefEnd, this.taskBeliefTimeIntersects)) {
                this.taskBeliefTimeIntersects[0] = this.taskBeliefTimeIntersects[1] = TIMELESS; //no intersection
            }
        }

        this.forEachMatch = null;
        this.concTruth = null;
        this.concPunc = 0;
        this.concSingle = false;
        this.truthFunction = null;
        this.evidenceDouble = evidenceSingle = null;

        this.parentVoluplexitySum =
                Util.sum(
                        taskTerm.voluplexity(), beliefTerm.voluplexity()
                );


        this.overlapSingle = _task.isCyclic();

        if (_belief != null) {

            /** to compute the time-discounted truth, find the minimum distance
             *  of the tasks considering their dtRange
             */


            if (taskStamp.isEmpty()) {
                taskStamp.addAll(_task.stamp());
            }

            this.overlapDouble =
                    Stamp.overlaps(this._task, _belief)
                            ||
                            //auto-filter double-premise, with same target and same time
                            taskStart == beliefStart && taskPunc == _belief.punc() && taskTerm.equals(beliefTerm);


        } else {
            this.overlapDouble = false;
        }


        boolean eternalComplete = (taskStart == ETERNAL) && (_belief == null || beliefStart == ETERNAL);
        this.temporalTerms = Occurrify.temporal(taskTerm) || Occurrify.temporal(beliefTerm);
        this.temporal = !eternalComplete || temporalTerms;
//        if ((_belief == null) && (!temporal)) {
//            if (Occurrify.temporal(beliefTerm)) {
//                Term beliefTermEternal = Retemporalize.retemporalizeXTERNALToDTERNAL.transform(beliefTerm); //HACK
//                if (Occurrify.temporal(beliefTermEternal)) {
//                    temporal = true;
//                } else {
//                    beliefTerm = beliefTermEternal;
//                }
//            }
//        }

        int causeCap = NAL.causeCapacity.intValue();
        this.parentCause =
                CauseMerge.limit(
                        _belief != null ?
                                CauseMerge.Append.merge(causeCap - 1 /* for channel to be appended */, _task, _belief) :
                                _task.why(), causeCap - 1);
//        if (parentCause.length >= causeCap)
//            throw new WTF();


        setTTL(ttl);


        what.derivePri.premise(this);

//        try {
//        } catch (Exception e) {
//            reset();
//            throw e;
//        }

    }

    @Override
    public final boolean match() {

        Predicate<Derivation> f = this.forEachMatch;
        return (f == null) || f.test(this);
    }


    /** update some cached values that will be used for one or more derivation iterations */
    public Derivation next(Deriver d, What w) {
        this.what = w;
        //if (this.what!=w) { .. }

        NAR p = this.nar(), n = w.nar;
        if (p != n) {

            this.reset();
            this.nar = n;
            this.random = n.random();
            this.unifyPremise.random(this.random);
            this.transform = new DerivationTransform();
        }

        long now = Tense.dither(n.time(), n);
        if (now != this.timePrev) {
            this.timePrev = now;
            this.ditherDT = n.dtDither();

            uniSubst.u.dtTolerance = unifyPremise.dtTolerance = this.dtTolerance =
                    //Math.round(Param.UNIFY_DT_TOLERANCE_DUR_FACTOR * dur);
                    n.dtDither();

            this.confMin = n.confMin.floatValue();
            this.eviMin = n.confMin.asEvi();

            this.termVolMax = n.termVolumeMax.intValue();
        }

        this.deriver = d;
        //this.anon.mustAtomize(deriver.rules.mustAtomize | Op.Variable); //<- not ready yet
        return this;
    }



    @Nullable
    public long[] evidenceSingle() {
        if (evidenceSingle == null) {
            evidenceSingle = _task.stamp();
        }
        return evidenceSingle;
    }

    @Nullable
    public long[] evidenceDouble() {
        if (evidenceDouble == null) {
            double te, be, tb;
            if (taskPunc == BELIEF || taskPunc == GOAL) {

                te = taskTruth.evi();
                be = beliefTruthBelief != null ? beliefTruthBelief.evi() : 0;
                tb = te / (te + be);
            } else {

                te = _task.priElseZero();
                be = _belief.priElseZero();
                tb = te + be;
                tb = tb < ScalarValue.EPSILON ? 0.5f : te / tb;
            }
            long[] e = Stamp.merge(_task.stamp(), _belief.stamp(), (float)tb, random);
            if (evidenceDouble == null || !Arrays.equals(e, evidenceDouble))
                this.evidenceDouble = e;
            return e;
        } else {
            return evidenceDouble;
        }
    }

    @Override
    public String toString() {
        return _task + " " + (_belief != null ? _belief : _beliefTerm)
                + ' ' + super.toString();
    }

    /**
     * include any .clear() for data structures in case of emergency we can continue to assume they will be clear on next run()
     */

    private Derivation reset() {
        anon.clear();
        timePrev = Long.MIN_VALUE;
        retransform.clear();
        occ.clear();
        _task = _belief = null;
        taskStamp.clear();
        parentCause = null;
        concTruth = null;
        taskTerm = beliefTerm = null;
        taskTruth = beliefTruthTask = beliefTruthBelief = null;
        canCollector.clear();
        ttl = 0;
        taskUniques = 0;
        temporal = temporalTerms = false;
        taskBeliefTimeIntersects[0] = taskBeliefTimeIntersects[1] = TIMELESS;
        nar = null;

        clear();

        return this;
    }



    /**
     * resolve a target (ex: task target or belief target) with the result of 2nd-layer substitutions
     */
    public Term retransform(Term x) {
        Term y = x;

        if (y.hasAny(VAR_DEP.bit | VAR_INDEP.bit | VAR_QUERY.bit))
            y = transform().apply(y);

        if (!retransform.isEmpty())
            y = y.replace(retransform); //retransforms only
        //x.replace(xy).replace(retransform); //avoid functor eval
        //transform(x).replace(retransform);
        //x.replace(retransform);

        if (y != x && !y.op().eventable)
            return x; //dont bother
        else
            return y;
    }

    public final nars.Task add(Task t) {
        return what.put(t);
    }

    public boolean concTruthEviMul(float ratio, boolean eternalize) {
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


    public final float parentPri() {
        return (concSingle ? priSingle : priDouble);
    }


    @Override
    public final TermTransform transform() {
        return this.transform;
    }

    /**
     * current NAR time, set at beginning of derivation
     */
    public final long time() {
        return nar().time();
    }

    public final int dur() {
        return what.dur();
    }

    public NAR nar() {
        return nar;
    }

    public long[] evidence() {
        return concSingle ? evidenceSingle() : evidenceDouble();
    }


    public final class DerivationTransform extends UnifyTransform {

        public transient Function<Variable,Term> xy = null;

        private final Function<Atomic, Term> derivationFunctors = DerivationFunctors.get(Derivation.this);

        @Override
        protected Term resolve(nars.term.Variable x) {
            if (xy != null) {
                Term y = xy.apply(x);
                return y == null ? x : y;
            } else
                return Derivation.this.resolve(x);
        }


        /**
         * only returns derivation-specific functors.  other functors must be evaluated at task execution time
         */
        @Override
        public final Term applyAtomic(Atomic atomic) {

            if (atomic instanceof Variable) {
                return super.applyAtomic(atomic);
            } else if (atomic instanceof Atom) {
                Term f = derivationFunctors.apply(atomic);
                if (f != null)
                    return f;
            }

            return atomic;
        }

        @Override
        public final boolean evalInline() {
            return true;
        }

    }


}


