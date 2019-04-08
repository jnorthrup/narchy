package nars.derive;

import jcog.Util;
import jcog.WTF;
import jcog.data.set.ArrayHashSet;
import jcog.data.set.MetalLongSet;
import jcog.math.Longerval;
import jcog.pri.ScalarValue;
import jcog.random.SplitMix64Random;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.attention.What;
import nars.control.CauseMerge;
import nars.derive.op.Occurrify;
import nars.eval.Evaluation;
import nars.op.Subst;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.task.ITask;
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
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;
import static nars.truth.func.TruthFunctions.c2wSafe;


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


    final UnifyPremise unifyPremise = new UnifyPremise(); {
        unifyPremise.commonVariables = false; //disable common variables for the query variables matched in premise formation; since the task target is not transformed like the belief target is.
    }
    public final Collection<Premise> premiseBuffer =
            new ArrayHashSet();
    //new LinkedHashSet();
    //new SortedList<>();

//    @Deprecated public final ArrayHashSet<Term> atomMatches = new ArrayHashSet();
//    @Deprecated public TopN<TaskLink> atomTangent = new TopN<>(new TaskLink[64], (FloatFunction<TaskLink>) ScalarValue::pri);

    public final AnonWithVarShift anon;

    public final UniSubst uniSubst = new UniSubst(this);

    /** current context */
    public transient What what = null;

    protected final Functor polarizeTask = new AbstractInlineFunctor1("polarizeTask") {
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

    protected final Functor polarizeBelief = new AbstractInlineFunctor1("polarizeBelief") {
        @Override
        protected Term apply1(Term arg) {
            Truth t = Derivation.this.beliefTruthBelief;
            if (t == null)
                return Null;  //TODO WTF
            return t.isPositive() ? arg : arg.neg();
        }
    };

    @Deprecated
    protected final Functor polarizeFunc = new AbstractInlineFunctor2("polarize") {
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

    /**
     * current NAR time, set at beginning of derivation
     */
    public transient long time = ETERNAL;
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
    private final transient MetalLongSet taskStamp = new MetalLongSet(Param.STAMP_CAPACITY);
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

    @Deprecated public transient long[] concOcc;
    @Deprecated public transient Truth concTruth;
    public transient byte concPunc;

    public transient Task _task, _belief;

    public transient int dur;


    /**
     * if using this, must setAt: nar, index, random, DerivationBudgeting
     */
    public Derivation() {
        super(
                null
                //VAR_PATTERN
                , null, Param.UnificationStackMax
        );

        this.random = new SplitMix64Random.SplitMix64RandomFull();
        this.anon = new AnonWithVarShift(ANON_INITIAL_CAPACITY, Op.VAR_DEP.bit | Op.VAR_QUERY.bit);
    }

    public DerivationTransform transform;


    private void init(NAR nar) {

        this.reset();

        this.nar = nar;

        //this.random = nar.random();
        this.random.setSeed(nar.random().nextLong());

        this.unifyPremise.random(this.random);
        this.transform = new DerivationTransform();
        //this.random.setSeed(nar.random().nextLong());

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
                                nextBelief.truth(taskStart, taskEnd, dur);

                if (Param.ETERNALIZE_BELIEF_PROJECTED_IN_DERIVATION
                        && !taskEternal
                        && beliefTruthBelief!=null
                        && (beliefTruthTask == null || !beliefTruthTask.equals(beliefTruthBelief))) {
                    this.beliefTruthTask = Truth.stronger(
                            beliefTruthTask,
                            beliefTruthBelief.eternalized(1, Param.TRUTH_EVI_MIN, null /* dont dither */)
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
            this.beliefTerm =
                    !(nextBeliefTerm instanceof Variable) ?
                            anon.putShift(this._beliefTerm = nextBeliefTerm, taskTerm) :
                            anon.put(this._beliefTerm = nextBeliefTerm); //unshifted, since the target may be structural
        }

        if (Param.DEBUG) {
            if ((beliefTerm instanceof Bool) || ((beliefTerm instanceof Compound && _beliefTerm instanceof Compound) && (beliefTerm.op() != _beliefTerm.op())))
                throw new WTF(_beliefTerm + " could not be anon, result: " + beliefTerm);

            assert (beliefTerm != null) : (nextBeliefTerm + " could not be anonymized");
            //assert (!(beliefTerm instanceof Bool));
            assert (beliefTerm.op() != NEG) : nextBelief + " , " + nextBeliefTerm + " -> " + beliefTerm + " is invalid NEG op";
        }

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
            if (taskTerm instanceof Bool || ((taskTerm instanceof Compound && nextTaskTerm instanceof Compound) && taskTerm.op() != nextTaskTerm.op())) //(!taskTerm.op().taskable)
                throw new WTF(nextTaskTerm + " could not be anon, result: " + taskTerm);
            this.taskUniques = anon.uniques();
        }


        if (currentTask == null || currentTask != nextTask) {


            assert (taskTerm != null) : (nextTask + " could not be anonymized: " + nextTaskTerm.anon() + " , " + taskTerm);

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

    protected float pri(Task t) {
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
                Param.DerivationPri.apply(taskPri, pri(belief));

//        if (Param.INPUT_BUFFER_PRI_BACKPRESSURE && Math.max(priDouble, priSingle) < nar.input.priMin() /* TODO cache */)
//            return false;

        this.priSingle = priSingle;
        this.priDouble = priDouble;
        return true;
    }

    /**
     * called after protoderivation has returned some possible Try's
     */
    public void derive(int ttl, short[] can) {

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

        int causeCap = Param.causeCapacity.intValue();
        this.parentCause =
                CauseMerge.limit(
                        _belief != null ?
                                CauseMerge.Append.merge(causeCap - 1 /* for channel to be appended */, _task, _belief) :
                                _task.why(), causeCap - 1);
        if (parentCause.length >= causeCap)
            throw new WTF();


        setTTL(ttl);


        what.derivePri.premise(this);

//        try {
        deriver.rules.run(this, can);
//        } catch (Exception e) {
//            reset();
//            throw e;
//        }

    }

    @Override
    public final void tryMatch() {

        Predicate<Derivation> f = this.forEachMatch;
        if (f!=null)
            f.test(this);

    }



    public Derivation next(Deriver d, What w) {
        next(d, w.nar);
        this.what = w;
        return this;
    }

    private void next(Deriver d, NAR n) {
        NAR pnar = this.nar;

        if (pnar != n) {
            init(n);
            time = TIMELESS;
        }

        long now = Tense.dither(n.time(), n);
        if (now != this.time) {
            this.time = now;

            this.dur = n.dur();
            this.ditherDT = n.dtDither();

            uniSubst.u.dtTolerance = unifyPremise.dtTolerance = this.dtTolerance =
                    //Math.round(Param.UNIFY_DT_TOLERANCE_DUR_FACTOR * dur);
                    n.dtDither();

            this.eviMin = c2wSafe(this.confMin = n.confMin.floatValue());

            this.termVolMax = n.termVolumeMax.intValue();
        }

        this.deriver = d;
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

    public Derivation reset() {
        anon.clear();
        time = ETERNAL;
        premiseBuffer.clear();

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
        time = TIMELESS;
        temporal = temporalTerms = false;
        taskBeliefTimeIntersects[0] = taskBeliefTimeIntersects[1] = TIMELESS;

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

    public final ITask add(Task t) {
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

    public final class DerivationTransform extends UnifyTransform {

        public transient Function<Variable,Term> xy = null;

        private final Function<Atomic, Term> derivationFunctors = DerivationFunctors.get(Derivation.this);

        @Override
        protected Term resolve(nars.term.Variable x) {
            if (xy != null) {
                Term y = xy.apply(x); if (y == null) return x; else return y;
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


