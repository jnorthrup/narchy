package nars.derive;

import jcog.Util;
import jcog.data.set.ArrayHashSet;
import jcog.math.random.SplitMix64Random;
import jcog.pri.ScalarValue;
import nars.*;
import nars.control.Cause;
import nars.derive.premise.PreDerivation;
import nars.derive.step.Occurrify;
import nars.op.SubIfUnify;
import nars.op.Subst;
import nars.subterm.Subterms;
import nars.task.proxy.TaskWithTerm;
import nars.term.*;
import nars.term.anon.Anon;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.compound.util.Image;
import nars.term.control.PREDICATE;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.func.TruthFunc;
import nars.util.term.TermHashMap;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static nars.Op.*;
import static nars.Param.TTL_UNIFY;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;
import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2cSafe;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends PreDerivation {


    public static final Atomic Task = Atomic.the("task");
    public static final Atomic Belief = Atomic.the("belief");
    private final static int ANON_INITIAL_CAPACITY = 16;


    //    private static final Atomic _tlRandom = (Atomic) $.the("termlinkRandom");
    public final ArrayHashSet<Premise> premiseBuffer =
            new ArrayHashSet<>(256) {
                @Override
                public Set<Premise> newSet() {
                    return new UnifiedSet<>(256, 0.99f);
                }
            };

    public final Anon anon;



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
                return Null;
            return compared.isNegative() ? subterm.neg() : subterm;
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

    private transient boolean eternal;
    public transient boolean temporal;
    public transient TruthFunc truthFunction;
    public transient int ditherTime;

    public Deriver deriver;
    private ImmutableMap<Term, Termed> derivationFunctors;

    /**
     * precise time that the task and belief truth are sampled
     */
    public transient long taskStart, beliefStart; //TODO taskEnd, beliefEnd
    public transient boolean taskBeliefTimeIntersects;
    private transient Term _beliefTerm;
    private transient long[] evidenceDouble, evidenceSingle;
    private transient int taskUniques;
    private transient ImmutableLongSet taskStamp;
    public transient boolean overlapDouble, overlapSingle;
    public transient float pri;
    public transient short[] parentCause;
    public transient boolean concSingle;
    public transient float parentComplexitySum;
    public transient float premiseEviSingle, premiseEviDouble;
    public transient long[] concOcc;
    public transient Truth concTruth;
    public transient byte concPunc;
    public transient Term concTerm;
    public transient Task _task, task, _belief, belief;

    /**
     * if using this, must set: nar, index, random, DerivationBudgeting
     */
    public Derivation() {
        super(

                VAR_PATTERN
                , null, Param.UnificationStackMax, 0,
                new TermHashMap()

        );

        this.random = new SplitMix64Random(1);


        this.anon = new Anon(ANON_INITIAL_CAPACITY);
//        this.anon = new CachedAnon(ANON_INITIAL_CAPACITY, 64 * 1024) {
//            @Override
//            protected boolean cacheGet() {
//                return false;
//            }
////
////            @Override
////            public Term put(Term x) {
////                if (x instanceof Atom) {
////                    Termed f = staticFunctors.get(x);
////                    if (f != null)
////                        x = (Term)f;
////                }
////                return super.put(x);
////            }
//
//        };

//        this.termlinkRandomProxy = Functor.f1("termlinkRandom", (x) -> {
//            x = anon.get(x);
//            if (x == null)
//                return Null;
//
//            Term y = $.func(_tlRandom, x).eval(nar, false);
//            if (y != null && y.op().conceptualizable)
//                return anon.put(y);
//            return Null;
//        });
    }

    @Override
    public final boolean eval() {
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
                    nar.concept("conjDropIfEarliestFiltered"),
                    nar.concept("conjDropIfLatest"),
                    nar.concept("conjDropIfLatestFiltered"),
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
    public boolean reset(Task nextTask, final Task nextBelief, Term nextBeliefTerm) {






        if (taskUniques > 0 && this._task != null && this._task.term().equals(nextTask.term())) {


            anon.rollback(taskUniques);


        } else {

            anon.clear();
            this.taskTerm = anon.put(nextTask.term());


            this.taskUniques = anon.uniques();
        }

        assert (taskTerm != null) : (nextTask + " could not be anonymized: " + nextTask.term().anon() + " , " + taskTerm);


        if (this._task == null || !Arrays.equals(this._task.stamp(), nextTask.stamp())) {
            this.taskStamp = Stamp.toSet(nextTask);
        }
        if (this._task == null || this._task != nextTask) {
            this.task = new TaskWithTerm(taskTerm, nextTask);
        }

        long taskStart = nextTask.start();

        assert (taskStart != TIMELESS);


        this._task = nextTask;

        this.taskPunc = nextTask.punc();
        if ((taskPunc == BELIEF || taskPunc == GOAL)) {
            this.taskTruth = nextTask.truth();
            assert(taskTruth!=null);
        } else {
            this.taskTruth = null;
        }

        this.taskStart = taskStart;

        long taskEnd = nextTask.end();
        if (nextBelief != null) {
            this.beliefTruthRaw = nextBelief.truth();
            this.beliefTruthDuringTask = nextBelief.truth(taskStart, taskEnd, dur);
            this.beliefStart = nextBelief.start();

            if (Param.ETERNALIZE_BELIEF_PROJECTED_IN_DERIVATION && !(taskStart==ETERNAL || beliefStart==ETERNAL)) {
                Truth beliefEte = beliefTruthRaw.eternalized(1, Param.TRUTH_MIN_EVI, nar);
                this.beliefTruthDuringTask = Truth.stronger(beliefTruthDuringTask, beliefEte);
            }

            this._belief = beliefTruthRaw != null || beliefTruthDuringTask != null ? nextBelief : null;
        } else {
            this._belief = null;
        }

try {
        if (this._belief != null) {

            beliefTerm = anon.put(this._beliefTerm = nextBelief.term());
            this.belief = new TaskWithTerm(beliefTerm, nextBelief);
        } else {

            this.beliefTerm = anon.put(this._beliefTerm = nextBeliefTerm);
            this.beliefStart = TIMELESS;
            this.belief = null;
            this.beliefTruthRaw = this.beliefTruthDuringTask = null;
        }
} catch (Throwable w) {
	//HACK
	if (Param.DEBUG) throw w;
	if (nextBelief!=null) nextBelief.delete();
}

        assert (beliefTerm != null) : (nextBeliefTerm + " could not be anonymized");
        assert (beliefTerm.op() != NEG): nextBelief + " , " + nextBeliefTerm + " -> " + beliefTerm + " is invalid NEG op";




        return true;
    }

    /**
     * called after protoderivation has returned some possible Try's
     */
    public void derive(int ttl) {

        this.taskBeliefTimeIntersects =
                this.belief == null
                        ||
                        this.belief.intersects(taskStart, task.end());




        this.termutes.clear();

        reset();

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


        this.overlapSingle = _task.isCyclic();

        if (_belief != null) {

            /** to compute the time-discounted truth, find the minimum distance
             *  of the tasks considering their dtRange
             */


            this.overlapDouble = Stamp.overlapsAny(this.taskStamp, _belief.stamp());


        } else {
            this.overlapDouble = false;
        }


        this.eternal = (taskStart==ETERNAL) && (_belief == null || _belief.isEternal());
        this.temporal = !eternal || (taskTerm.isTemporal() || (_belief != null && beliefTerm.isTemporal()));

        this.parentCause = _belief != null ?
                Cause.sample(Param.causeCapacity.intValue(), _task, _belief) :
                _task.cause();

        float taskPri = _task.priElseZero();
        this.pri =
                _belief == null ?
                        Param.TaskToDerivation.valueOf(taskPri) :
                        Param.TaskBeliefToDerivation.apply(taskPri, _belief.priElseZero());


        this.premiseEviSingle = taskTruth != null ? taskTruth.evi() : Float.NaN;
        this.premiseEviDouble = beliefTruthRaw != null ?

                premiseEviSingle + beliefTruthRaw.evi() :
                premiseEviSingle;


        setTTL(ttl);


        deriver.prioritize.premise(this);

        deriver.rules.test(this);


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
            this.ditherTime = nar.dtDither();
            this.confMin = nar.confMin.floatValue();
            this.termVolMax = nar.termVolumeMax.intValue();
            this.random.setSeed(nar.random().nextLong());

        }

        this.deriver = deri;

        return this;
    }

    @Nullable
    public long[] evidenceSingle() {
        if (evidenceSingle == null) {
            evidenceSingle = task.stamp();
        }
        return evidenceSingle;
    }

    @Nullable
    public long[] evidenceDouble() {
        if (evidenceDouble == null) {
            float te, be, tb;
            if (task.isBeliefOrGoal()) {

                te = taskTruth.evi();
                be = beliefTruthRaw != null ? beliefTruthRaw.evi() : 0;
                tb = te / (te + be);
            } else {

                te = task.priElseZero();
                be = belief.priElseZero();
                tb = te + be;
                tb = tb < ScalarValue.EPSILON ? 0.5f : te / tb;
            }
            return evidenceDouble = Stamp.zip(task.stamp(), belief.stamp(), tb);
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
    public void clear() {
        anon.clear();
        taskUniques = 0;
        premiseBuffer.clear();
        untransform.clear();
        termutes.clear();
        time = ETERNAL;
        super.clear();
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
        float e = ratio * c2wSafe(concTruth.conf());
        if (eternalize)
            e = Math.max(concTruth.eviEternalized(), e);
        return concTruthEvi(e);
    }

    public boolean concTruthEvi(float e) {
        float cc = w2cSafe(e);
        return cc >= confMin && (this.concTruth = $.t(concTruth.freq(), cc)) != null;
    }

}


