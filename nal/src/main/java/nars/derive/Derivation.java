package nars.derive;

import jcog.Util;
import jcog.data.byt.DynBytes;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.data.set.MetalLongSet;
import jcog.math.Longerval;
import jcog.pri.ScalarValue;
import nars.*;
import nars.concept.Concept;
import nars.control.Cause;
import nars.derive.op.Occurrify;
import nars.derive.premise.PreDerivation;
import nars.eval.Evaluation;
import nars.link.TaskLink;
import nars.op.SubIfUnify;
import nars.op.Subst;
import nars.subterm.Subterms;
import nars.task.proxy.SpecialTermTask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Variable;
import nars.term.anon.Anon;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.PREDICATE;
import nars.term.util.Image;
import nars.term.util.transform.Retemporalize;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.func.TruthFunc;
import nars.truth.polation.TruthIntegration;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static nars.Op.*;
import static nars.Param.TTL_UNIFY;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;
import static nars.truth.TruthFunctions.w2cSafe;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends PreDerivation {

    protected final static Logger logger = LoggerFactory.getLogger(Derivation.class);

    public static final Atomic Task = Atomic.the("task");
    public static final Atomic Belief = Atomic.the("belief");
    public static final Atomic TaskTerm = Atomic.the("taskTerm");
    public static final Atomic BeliefTerm = Atomic.the("beliefTerm");
    private final static int ANON_INITIAL_CAPACITY = 16;


//    //    private static final Atomic _tlRandom = (Atomic) $.the("termlinkRandom");
    public final FasterList<Premise> premiseBuffer =
        new FasterList<>(1024);
//            new ArrayHashSet<>(256) {
//                @Override
//                public Set<Premise> newSet() {
//                    return new UnifiedSet<>(256, 0.99f);
//                }
//            };

    public final Anon.AnonWithVarShift anon;


    private final SubIfUnify mySubIfUnify = new SubIfUnify(this);
    private final Functor polarizeFunc = new Functor.AbstractInlineFunctor2("polarize") {
        @Override
        protected Term apply(Term subterm, Term whichTask) {
            if (subterm instanceof Bool)
                return subterm;

            Truth compared;
            if (whichTask.equals(Task)) {
                compared = taskTruth;
            } else {
                //assert(whichTask.equals(Belief))
                compared = beliefTruthRaw;
            }
            if (compared == null)
                return Bool.Null;
            return compared.isPositive() ? subterm : subterm.neg();
        }
    };
    public NAR nar;
//    private final Functor.LambdaFunctor termlinkRandomProxy;
    /**
     * temporary un-transform map
     */
    public final Map<Term, Term> untransform = new UnifiedMap<>() {
        @Override
        public Term put(Term key, Term value) {
            if (key.equals(value))
                return null;

            return super.put(key, value);
        }
    };
    private final Subst mySubst = new Subst("substitute") {

        @Override
        public @Nullable Term apply(Evaluation e, Subterms xx) {
            Term input = xx.sub(0);
            Term replaced = xx.sub(1);
            Term replacement = xx.sub(2);
            if (replaced instanceof Atom) {

                replaced = anon.put(replaced);
            }

            Term y = apply(xx, input, replaced, replacement);

            use(TTL_UNIFY); //substitute actually

            if (y != null && !(y instanceof Bool)) {


                untransform.put(y, input);


            }
            return y;
        }
    };


    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    public PREDICATE<Derivation> forEachMatch;
    /**
     * current NAR time, set at beginning of derivation
     */
    public transient long time = ETERNAL;
    public transient float confMin;
    public transient int termVolMax;


    /**
     * the base priority determined by the task and/or belief (tasks) of the premise.
     * note: this is not the same as the premise priority, which is determined by the links
     * and has already affected the selection of those links to create derived premises.
     * instead, the derived tasks are budgeted according to the priorities of the
     * parent task(s) NOT the links.  this allows the different budget 'currencies' to remain
     * separate.
     */
    public final Occurrify occ = new Occurrify(this);
    /**
     * whether either the task or belief are events and thus need to be considered with respect to time
     */

    @Deprecated public transient boolean temporal;
    public transient TruthFunc truthFunction;
    public transient int ditherTime;

    public Deriver deriver;
    public final DynBytes tmpPremiseKey = new DynBytes(256);

    /** temporary storage buffer for recently fired tasklinks */
    public final ArrayHashSet<TaskLink> firedTaskLinks = new ArrayHashSet<>(32);
    /** temporary storage buffer for recently activated concepts */
    public final ArrayHashSet<Concept> firedConcepts = new ArrayHashSet<>(32);

    private ImmutableMap<Term, Termed> derivationFunctors;

    /**
     * precise time that the task and belief truth are sampled
     */
    public transient long taskStart, taskEnd, beliefStart, beliefEnd; //TODO taskEnd, beliefEnd
    public transient boolean taskBeliefTimeIntersects;
    private transient Term _beliefTerm;
    private transient long[] evidenceDouble, evidenceSingle;
    private transient int taskUniques;
    private transient MetalLongSet taskStamp;
    public transient boolean overlapDouble, overlapSingle;
    public transient float pri;
    public transient short[] parentCause;
    public transient boolean concSingle;
    public transient float parentComplexitySum;
    public transient float taskEvi, beliefEvi;
    public transient long[] concOcc;
    public transient Truth concTruth;
    public transient byte concPunc;
    public transient Term concTerm;
    public transient Task _task, task, _belief, belief;
    public transient int dur;

    /**
     * if using this, must set: nar, index, random, DerivationBudgeting
     */
    public Derivation() {
        super(
                null
                , null, Param.UnificationStackMax
        );

        this.anon = new Anon.AnonWithVarShift(ANON_INITIAL_CAPACITY);
    }

    @Override
    public final boolean evalInline() {
        return true;
    }

    private void init(NAR nar) {

        this.clear();

        this.nar = nar;

        {

            Map<Term, Termed> m = new HashMap<>();

            for (Termed s : Builtin.statik)
                if (s instanceof Functor.InlineFunctor)
                    m.put(s.term(), s);

            Termed[] derivationFunctors = new Termed[]{
                    mySubIfUnify,
                    mySubst,
                    polarizeFunc,
//                    termlinkRandomProxy,
                    Image.imageExt,
                    Image.imageInt,
                    Image.imageNormalize,
                    nar.concept("union"),
                    nar.concept("differ"),
                    nar.concept("intersect"),
                    nar.concept("equal"),
                    nar.concept("conjWithout"),
                    nar.concept("conjWithoutAll"),
                    nar.concept("conjWithoutPosOrNeg"),
                    nar.concept("conjDropIfEarliest"),
                    //nar.concept("conjDropIfEarliestFiltered"),
                    nar.concept("conjDropIfLatest"),
                    //nar.concept("conjDropIfLatestFiltered"),
                    nar.concept("dropAnySet"),
                    nar.concept("dropAnyEvent"),
                    nar.concept("without"),
                    nar.concept("withoutPosOrNeg"),
            };

            for (Termed x : derivationFunctors) //override any statik's
                m.put(x.term(), x);


            this.derivationFunctors = Maps.immutable.ofMap(m);
        }

//        {
//            Map<Term, Termed> n = new HashMap<>(Builtin.statik.length);
//            for (Termed s : Builtin.statik) {
//                if (s instanceof Functor.InlineFunctor)
//                    n.put(s.term(), s);
//            }
//            this.staticFunctors = Maps.immutable.ofMap(n);
//        }

    }


    /**
     * setup for a new derivation.
     * returns false if the premise is invalid to derive
     * <p>
     * this is optimized for repeated use of the same task (with differing belief/beliefTerm)
     */
    public void reset(Task nextTask, final Task nextBelief, Term nextBeliefTerm) {


        if (taskUniques > 0 && this._task != null && this._task.term().equals(nextTask.term())) {


            anon.rollback(taskUniques);


        } else {
            anon.clear();

            anon.unshift();
            this.taskTerm = anon.put(nextTask.term());



            this.taskUniques = anon.uniques();
        }

        assert (taskTerm != null) : (nextTask + " could not be anonymized: " + nextTask.term().anon() + " , " + taskTerm);


        if (this._task == null || !Arrays.equals(this._task.stamp(), nextTask.stamp())) {
            this.taskStamp = null; //force (re-)compute in post-derivation stage
        }
        if (this._task == null || this._task != nextTask) {
            this.task = new SpecialTermTask(taskTerm, nextTask);
        }




        this._task = nextTask;

        this.taskPunc = nextTask.punc();
        if ((taskPunc == BELIEF || taskPunc == GOAL)) {
            this.taskTruth = nextTask.truth();
            assert (taskTruth != null);
        } else {
            this.taskTruth = null;
        }

        long taskStart = nextTask.start();
        this.taskStart = taskStart;
        this.taskEnd = _task.end();

        long taskEnd = nextTask.end();
        if (nextBelief != null) {
            this.beliefTruthRaw = nextBelief.truth();
            this.beliefStart = nextBelief.start();
            this.beliefEnd = nextBelief.end();

            this.beliefTruthProjectedToTask = nextBelief.truth(taskStart, taskEnd, dur);

            if (Param.ETERNALIZE_BELIEF_PROJECTED_IN_DERIVATION && !(beliefStart == ETERNAL && !beliefTruthProjectedToTask.equals(beliefTruthRaw))) {
                if (Param.eternalizeInDerivation.test(nextBelief.op())) {
                    this.beliefTruthProjectedToTask = Truth.stronger(
                            beliefTruthProjectedToTask,
                            beliefTruthRaw.eternalized(1, Param.TRUTH_MIN_EVI, null /* dont dither */)
                    );
                }
            }

            this._belief = beliefTruthRaw != null || beliefTruthProjectedToTask != null ? nextBelief : null;
        } else {
            this._belief = null;
        }

        if (this._belief != null) {

            beliefTerm = anon.putShift(this._beliefTerm = nextBelief.term(), taskTerm);
            this.belief = new SpecialTermTask(beliefTerm, nextBelief);
        } else {

            boolean shiftBeliefTerm = !(nextBeliefTerm instanceof Variable);
            this.beliefTerm =
                    shiftBeliefTerm ?
                        anon.putShift(this._beliefTerm = nextBeliefTerm, taskTerm) :
                        anon.put(this._beliefTerm = nextBeliefTerm); //unshifted, since the term may be structural

            this.belief = null;
            this.beliefStart = this.beliefEnd = TIMELESS;
            this.beliefTruthRaw = this.beliefTruthProjectedToTask = null;
        }


        assert (beliefTerm != null) : (nextBeliefTerm + " could not be anonymized");
        assert (beliefTerm.op() != NEG) : nextBelief + " , " + nextBeliefTerm + " -> " + beliefTerm + " is invalid NEG op";

    }

    public static boolean fatal(RuntimeException w) {
        return fatal(null, w);
    }
    public static boolean fatal(@Nullable Task problemTask, RuntimeException w) {
        if (problemTask!=null)
            problemTask.delete();
        if (Param.DEBUG)
            throw w;
        else {
            logger.warn("{}", w.getMessage());
            return false;
        }
    }

    /**
     * called after protoderivation has returned some possible Try's
     */
    public void derive(int ttl) {

        reset();

        this.taskBeliefTimeIntersects =
                this.belief == null
                        ||
                        this.belief.intersects(taskStart, taskEnd);

        this.forEachMatch = null;
        this.concTruth = null;
        this.concPunc = 0;
        this.concTerm = null;
        this.concSingle = false;
        this.truthFunction = null;
        this.evidenceDouble = evidenceSingle = null;

        this.parentComplexitySum =
                Util.sum(
                        taskTerm.voluplexity(), beliefTerm.voluplexity()
                );

        this.taskStamp = Stamp.toSet(_task);

        this.overlapSingle = _task.isCyclic();

        if (_belief != null) {

            /** to compute the time-discounted truth, find the minimum distance
             *  of the tasks considering their dtRange
             */


            this.overlapDouble = Stamp.overlapsAny(this.taskStamp, _belief.stamp());


        } else {
            this.overlapDouble = false;
        }


        boolean eternal = (taskStart == ETERNAL) && (_belief == null || beliefStart==ETERNAL);
        this.temporal = !eternal || Occurrify.temporal(taskTerm);
        if ((_belief == null) && (!temporal)) {
            if (Occurrify.temporal(beliefTerm)) {
                Term beliefTermEternal = Retemporalize.retemporalizeXTERNALToDTERNAL.transform(beliefTerm); //HACK
                if (Occurrify.temporal(beliefTermEternal)) {
                    temporal = true;
                } else {
                    beliefTerm = beliefTermEternal;
                }
            }
        }

        this.parentCause = _belief != null ?
                Cause.merge(Param.causeCapacity.intValue(), _task, _belief) :
                _task.cause();

        float taskPri = _task.priElseZero();
        this.pri =
                _belief == null ?
                        Param.TaskToDerivation.valueOf(taskPri) :
                        Param.TaskBeliefToDerivation.apply(taskPri, _belief.priElseZero());


        long[] t = belief!=null && taskStart!=ETERNAL && beliefStart != ETERNAL ?
                Longerval.unionArray(taskStart, taskEnd, beliefStart, beliefEnd ) :
                ( belief != null && taskStart==ETERNAL ? new long[] { beliefStart, beliefEnd } :
                        new long[] { taskStart, taskEnd });
        this.taskEvi = taskTruth != null ? TruthIntegration.evi(_task, t, 0) : 0;
        this.beliefEvi = belief != null ? TruthIntegration.evi(_belief, t, 0) : 0;


        setTTL(ttl);


        deriver.budgeting.premise(this);

        deriver.rules.run(this);


    }

    @Override
    public final void tryMatch() {


        forEachMatch.test(this);



        /* finally {
            revert(now);
        }*/

    }


    /**
     * only returns derivation-specific functors.  other functors must be evaluated at task execution time
     */
    @Override
    public final Term transformAtomic(Atomic atomic) {


        if (atomic instanceof Variable) {
            Term y = resolve(atomic);
            if (y != null)
                return y;
        }

        if (atomic instanceof Atom) {
            if (atomic == TaskTerm) {
                return taskTerm;
            } else if (atomic == BeliefTerm) {
                return beliefTerm;
            }

            Termed f = derivationFunctors.get(atomic);
            if (f != null)
                return (Term) f;
        }


        return atomic;
    }

    public Derivation next(NAR nar, Deriver deri) {
        NAR pnar = this.nar;

        if (pnar != nar) {
            init(nar);
        }

        long now = nar.time();
        if (now != this.time) {
            this.time = now;
            this.dur = deri.dur();
            this.dtTolerance = Math.round(Param.UNIFY_DT_TOLERANCE_DUR_FACTOR * dur);
            this.ditherTime = nar.dtDither();
            this.confMin = nar.confMin.floatValue();
            this.termVolMax = nar.termVolumeMax.intValue();

            this.random = nar.random();
            //this.random.setSeed(nar.random().nextLong());

        }

        this.deriver = deri;

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
            float te, be, tb;
            if (taskPunc == BELIEF || taskPunc == GOAL) {

                te = taskTruth.evi();
                be = beliefTruthRaw != null ? beliefTruthRaw.evi() : 0;
                tb = te / (te + be);
            } else {

                te = _task.priElseZero();
                be = _belief.priElseZero();
                tb = te + be;
                tb = tb < ScalarValue.EPSILON ? 0.5f : te / tb;
            }
            return evidenceDouble = Stamp.zip(_task.stamp(), _belief.stamp(), tb);
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
    @Override
    public Derivation clear() {
        anon.clear();
        taskUniques = 0;
//        premiseBuffer.clear();
        untransform.clear();
        termutes.clear();
        time = ETERNAL;
        super.clear();
        return this;
    }


    public Term untransform(Term t) {
        return t.replace(untransform);
    }

    public final Task add(Task t) {
        return deriver.derived.add(t, this);
    }

    public final boolean revertLive(int before, int cost) {
        ttl -= cost;
        return revertLive(before);
    }


    public boolean concTruthEviMul(float ratio, boolean eternalize) {
        float e = ratio * concTruth.evi();
        if (eternalize)
            e = Math.max(concTruth.eviEternalized(), e);
        return concTruthEvi(e);
    }

    public boolean concTruthEvi(float e) {
        float cc = w2cSafe(e);
        return cc >= confMin && (this.concTruth = $.t(concTruth.freq(), cc)) != null;
    }

}


