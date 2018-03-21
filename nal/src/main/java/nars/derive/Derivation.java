package nars.derive;

import jcog.data.ArrayHashSet;
import jcog.pri.Pri;
import jcog.version.Versioned;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.derive.rule.PremiseRule;
import nars.derive.time.DeriveTime;
import nars.op.SubIfUnify;
import nars.op.Subst;
import nars.op.data.differ;
import nars.op.data.intersect;
import nars.op.data.union;
import nars.subterm.Subterms;
import nars.task.NALTask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.anon.Anon;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.pred.PrediTerm;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;
import static nars.truth.TruthFunctions.c2wSafe;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends ProtoDerivation {

    /** initial capacity, it will grow as needed */
    int ANON_CAPACITY = 16;

    /** fixed size */
    int ANON_CACHE_SIZE = ANON_CAPACITY * 1024;

//    public static final Atomic TaskTerm = Atomic.the("_taskTerm");
//    public static final Atomic BeliefTerm = Atomic.the("_beliefTerm");


    public NAR nar;

//    public final ByteShuffler shuffler = new ByteShuffler(64);
//    public final BatchActivation activator = new BatchActivation();

    public final Anon anon =
            new Anon(ANON_CAPACITY);
            //new CachedAnon(ANON_CAPACITY, ANON_CACHE_SIZE);

    /**
     * temporary buffer for derivations before input so they can be merged in case of duplicates
     */
    public final Map<Task, Task> derivations = new LinkedHashMap<>();

    private ImmutableMap<Term, Termed> derivationFunctors;

    public float freqRes, confRes;


    /**
     * mutable state
     */
    public Truth concTruth;
    public byte concPunc;
    public float concEviFactor;
    public final long[] concOcc = new long[2];
    public final Versioned<Term> derivedTerm;


    /**
     * cached values ==========================================
     */
    public int termVolMax;
    public float confMin, eviMin;


    public Task task;
    public Task belief;

    public Truth taskTruth;
    public Truth beliefTruth;

    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    public PrediTerm<Derivation> forEachMatch;


    /**
     * current NAR time, set at beginning of derivation
     */
    public long time = ETERNAL;


    public Task _task;
    public Task _belief;
    public Term _beliefTerm;


    /**
     * evidential overlap
     */
    public float overlapDouble, overlapSingle;

    /** the base priority determined by the task and/or belief (tasks) of the premise.
     * note: this is not the same as the premise priority, which is determined by the links
     * and has already affected the selection of those links to create derived premises.
     * instead, the derived tasks are budgeted according to the priorities of the
     * parent task(s) NOT the links.  this allows the different budget 'currencies' to remain
     * separate.
     */
    public float pri;

    public short[] parentCause;

    public DeriverRoot derive;
    public boolean single;
    public float parentComplexityMax;


    public float premiseEviSingle;
    public float premiseEviDouble;
    private long[] evidenceDouble, evidenceSingle;
    public DeriveTime dtSingle = null, dtDouble = null;


    /**
     * whether either the task or belief are events and thus need to be considered with respect to time
     */
    public boolean temporal;
    public boolean eternal;

    /**
     * original non-anonymized tasks
     */
    public TruthOperator truthFunction;
    public int ditherTime;
    public Deriver deriver;

    /** precise time that the task and belief truth are sampled */
    public long taskAt, beliefAt;

    /** temporary buffer for storing unique premises */
    public Set<Premise> premiseBurst =
            //new LinkedHashSet();
            new ArrayHashSet<>(512);

////    public final TopNUniquePremises premises = new TopNUniquePremises();
//
//    protected class TopNUniquePremises extends TopNUnique<Premise> {
//        private int premisesRemain;
//
//        final FloatFloatToFloatFunction merge = Param.taskTermLinksToPremise;
//
//        TopNUniquePremises() {
//            super(Prioritized::pri);
//        }
//
//        @Override
//        protected void mergeInto(Premise existing, Premise next) {
//            float np = next.pri();
//            if (np > existing.pri()) {
//                existing.priMax(np);
//                sort();
//            }
//        }
//
//        /**
//         * call before generating a concept's premises
//         */
//        public void setTTL(int hypotheticalPremisePerConcept) {
//            this.premisesRemain = hypotheticalPremisePerConcept;
//        }
//
//        /**
//         * returns whether to continue
//         */
//        public boolean tryAdd(PriReference<Task> tasklink, PriReference<Term> termlink) {
//            float pri = tasklink.pri();
//            if (pri == pri) {
//
//                Task t = tasklink.get();
//                if (t != null) {
//
//                    float p = merge.apply(pri,
//                            termlink.priElseZero())
//                            * nar.amp(t);
//                    if (p > minAdmission()) {
//                        add(new Premise(t, termlink.get(), p));
//                    } else {
//                        //System.out.println("rejected early");
//                    }
//                }
//
//            }
//            return --premisesRemain > 0;
//        }
//    }

//    protected class TopNUniquePremises extends TopNUnique<Premise> {
//        private int premisesRemain;
//
//        final FloatFloatToFloatFunction merge = Param.taskTermLinksToPremise;
//
//        TopNUniquePremises() {
//            super(Prioritized::pri);
//        }
//
//        @Override
//        protected void mergeInto(Premise existing, Premise next) {
//            float np = next.pri();
//            if (np > existing.pri()) {
//                existing.priMax(np);
//                sort();
//            }
//        }
//
//        /**
//         * call before generating a concept's premises
//         */
//        public void setTTL(int hypotheticalPremisePerConcept) {
//            this.premisesRemain = hypotheticalPremisePerConcept;
//        }
//
//        /**
//         * returns whether to continue
//         */
//        public boolean tryAdd(PriReference<Task> tasklink, PriReference<Term> termlink) {
//            float pri = tasklink.pri();
//            if (pri == pri) {
//
//                Task t = tasklink.get();
//                if (t != null) {
//
//                    float p = merge.apply(pri,
//                            termlink.priElseZero())
//                            * nar.amp(t);
//                    if (p > minAdmission()) {
//                        add(new Premise(t, termlink.get(), p));
//                    } else {
//                        //System.out.println("rejected early");
//                    }
//                }
//
//            }
//            return --premisesRemain > 0;
//        }
//    }
//

    //public Map<Term, Term> xyDyn = new HashMap();


//    private transient Term[][] currentMatch;

//    public /*static*/ final Cache<Transformation, Term> transformsCache; //works in static mode too
//    /*static*/ {
//    }

//    final MRUCache<Transformation, Term> transformsCache = new MRUCache<>(Param.DERIVATION_THREAD_TRANSFORM_CACHE_SIZE);

    /**
     * if using this, must set: nar, index, random, DerivationBudgeting
     */
    public Derivation() {
        super(
                //null /* any var type */
                VAR_PATTERN
                , null, Param.UnificationStackMax, 0);


        derivedTerm = new Versioned(this, 3);


        //anon = new Anon();
//            @Override
//            public Term get(Term x) {
//                Term y = super.get(x);
//                boolean possiblyEvaluable = false; //quick test
//                Term z = (y != null) ?
//                        ((possiblyEvaluable = y.hasAll(Op.funcBits)) ? y.eval(nar.terms) : y) : Null;
//
//                return z;
//            }

//            @Override
//            public Term _get(Term x) {
//                Term y = super._get(x);
//                return y != null ? y.eval(nar.terms) : null;
//            }


//        Caffeine cb = Caffeine.newBuilder().executor(nar.exe);
//            //.executor(MoreExecutors.directExecutor());
//        int cs = Param.DERIVATION_TRANSFORM_CACHE_SIZE_PER_THREAD;
//        if (cs == -1)
//            cb.softValues();
//        else
//            cb.maximumSize(cs);
//
//        //cb.recordStats();
//
//        transformsCache = cb.builder();

    }

    /**
     * functors to be inserted in PatternIndex's for direct usage
     */
    public static Termed[] ruleFunctors(NAR nar) {
        return new Termed[]{
                union.the,
                differ.the,
                intersect.the,

//                nar.get(Atomic.the("add")),
//                nar.get(Atomic.the("mul")),

                nar.get(Atomic.the("dropAnyEvent")),
                nar.get(Atomic.the("dropAnySet")),
                nar.get(Atomic.the("without")),
                nar.get(Atomic.the("withoutPosOrNeg")),
                nar.get(Atomic.the("conjWithout")),
                nar.get(Atomic.the("conjDropIfEarliest")),
                nar.get(Atomic.the("conjDropIfLatest")),
                nar.get(Atomic.the("conjEvent")),
                nar.get(Atomic.the("ifConjCommNoDepVars")),
                nar.get(Atomic.the("indicesOf")),
                nar.get(Atomic.the("substDiff")),
                nar.get(Atomic.the("ifNeqRoot")),
        };
    }

    /**
     * only returns derivation-specific functors.  other functors must be evaluated at task execution time
     */
    @Override
    public final Term transformAtomic(Term atomic) {

        if (atomic instanceof Bool)//assert (!(x instanceof Bool));
            return atomic;

        if (atomic instanceof Atom) {
            Termed f = derivationFunctors.get(atomic);
            if (f != null)
                return f.term();
        }


        Term y = xy(atomic);
        if (y != null) {
            return y; //an assigned substitution, whether a variable or other type of term
        } else {
            return atomic;
        }

//        else if (x.hasAny(substitutionVector)) {
//            return super.applyTermIfPossible(x);
//        } else {
//            return x;
//        }
    }

    public Derivation cycle(NAR nar, Deriver deri, DeriverRoot deriverRoot) {
        NAR pnar = this.nar;
        if (pnar != nar) {
            init(nar);
        }

        long now = nar.time();
        if (now != this.time) {
            this.time = now;
            this.dur = nar.dur();
            this.ditherTime = nar.dtDitherCycles();
            this.freqRes = nar.freqResolution.floatValue();
            this.confRes = nar.confResolution.floatValue();
            this.confMin = nar.confMin.floatValue();
            this.eviMin = c2wSafe(confMin);
            this.termVolMax = nar.termVolumeMax.intValue();
            //transformsCache.cleanUp();
        }

        this.derive = deriverRoot;
        this.deriver = deri;

        return this;
    }

    void init(NAR nar) {
        this.clear();
        this.nar = nar;
        this.random = nar.random();

        Termed[] derivationFunctors = new Termed[]{
                uniSubAny,
                uniSub,
                polarizeFunc,
                termlinkRandomProxy
        };
        Map<Term, Termed> m = new HashMap<>(derivationFunctors.length + 2);

        for (Termed x : ruleFunctors(nar))
            m.put(x.term(), x);

        for (Termed x : derivationFunctors)
            m.put(x.term(), x);

//        m.put(TaskTerm, taskTerm);
//        m.put(BeliefTerm, beliefTerm);
        this.derivationFunctors = Maps.immutable.ofMap(m);
    }

    @Override public Derivation reset() {
        super.reset();

        anon.clear();

        this.task = this.belief = null;
        this._task = this._belief = null;
        this._beliefTerm = null;

        this.beliefTruth = this.taskTruth = null;

        this.forEachMatch = null;
        this.concTruth = null;
        this.concPunc = 0;
        this.truthFunction = null;
        this.single = false;
        this.evidenceDouble = evidenceSingle = null;
        this.dtSingle = this.dtDouble = null;
        this.concOcc[0] = this.concOcc[1] = ETERNAL;

        this.derivedTerm.clear();


        //        if (revert(0)) {
        //remove common variable entries because they will just consume memory if retained as empty
//            xy.map.keySet().removeIf(k -> {
//                return !(k instanceof AbstractVariable) || k instanceof CommonVariable;
//            });
//            xy.map.clear();
//        }

        return this;
    }


    /** returns false if there was a critical truth deficit */
    protected boolean setTruth() {


        long tAt;
        //if task is eternal, pretend task is temporal and current moment if belief is temporal and task is eternal
        if (_task.isEternal() && (belief!=null && !belief.isEternal()))
            tAt = time;
        else
            tAt = _task.nearestPointInternal(time);

        this.taskAt = tAt;
        switch (this.taskPunc = _task.punc()) {
            case QUESTION:
            case QUEST:
                this.taskPolarity = 0;
                assert(this.taskTruth == null);
                break;
            default:
                if ((this.taskTruth = _task.truth(tAt, dur)) == null)
                    return false;
                assert(this.taskTruth!=null);
                this.taskPolarity = polarity(taskTruth);
                break;
        }

        if (belief != null) {
            long bAt = belief.nearestPointExternal(_task.start(), _task.end());
            if ((this.beliefTruth = belief.truth(bAt, dur))!=null) {
                this.beliefPolarity = polarity(this.beliefTruth);
                this.beliefAt =
                        //bAt;
                        belief.start();
            } else {
                this.belief = null;
                this.beliefPolarity = 0;
                this.beliefAt = TIMELESS;
            }

        } else {
            this.beliefTruth = null;
            this.beliefPolarity = 0;
            this.beliefAt = TIMELESS;
        }

        return true;
    }

    /**
     * setup the derivation for the ProtoDerivation stage
     *
     * must call reset() immediately before or after calling this.
     *
     * TODO move some of these to a superclass method which sets the variables in its scope
     */
    public boolean proto(Task _task, final Task _belief, Term _beliefTerm) {


        final Task task = this.task = anon.put(this._task = _task, dur);
        if (task == null)
            throw new NullPointerException(_task + " could not be anonymized: " +
                    _task.term().anon() + " , " + anon.put(this._task = _task, dur));
        final Term taskTerm = this.taskTerm = task.term();
        final Term beliefTerm;
        if (_belief != null) {
            if ((this.belief = anon.put(this._belief = _belief, dur)) == null)
                throw new NullPointerException(_belief + " could not be anonymized");
            beliefTerm = this.beliefTerm = this.belief.term();
        } else {
            this.belief = null;
            if ((beliefTerm = this.beliefTerm = anon.put(this._beliefTerm = _beliefTerm))==null)
                throw new NullPointerException(_belief + " could not be anonymized");
        }



        this._taskStruct = taskTerm.structure();
        this._taskOp = taskTerm.op().id;
        this._beliefStruct = beliefTerm.structure();
        this._beliefOp = beliefTerm.op().id;

        return setTruth();
    }

    /** called after protoderivation has returned some possible Try's */
    public void derive(int ttl) {
//        int ttv = taskTerm.vars();
//        if (ttv > 0 && bt.vars() > 0) {
//            bt = bt.normalize(ttv); //shift variables up to be unique compared to taskTerm's
//        }
        this.parentComplexityMax =
                //Util.sum(
                Math.max(
                        taskTerm.voluplexity(), beliefTerm.voluplexity()
                );



        long[] taskStamp = task.stamp();
        this.overlapSingle = task.isCyclic() ? 1 : 0; //Stamp.cyclicity(taskStamp);

        if (_belief != null) {

            /** to compute the time-discounted truth, find the minimum distance
             *  of the tasks considering their dtRange
             */
//            if (!belief.isEternal()) {
//                //project belief truth to task's time
//                this.beliefTruth = !task.isEternal() ?
//                        belief.truth(belief.nearestTimeOf(task.start(), task.end()), dur, confMin) :
//                        belief.truth();
//                //belief.truth(ETERNAL, ETERNAL...)
//            } else {
//            this.beliefTruth = beliefTruth;
//            }

            long[] beliefStamp = _belief.stamp();
            this.overlapDouble =

                    //Math.min(1, Util.sum(
//                    Util.or(
//                            //Util.max(
//                            overlapSingle,
//                            Stamp.overlapFraction(taskStamp, beliefStamp),
//                            Stamp.cyclicity(beliefStamp)
//                    );

//                    (task.isCyclic() /* || belief.isCyclic()*/) ?
//                            1 :
                            Stamp.overlapFraction(taskStamp, beliefStamp);

        } else {
            this.overlapDouble = 0;
        }


        this.eternal = task.isEternal() && (_belief == null || _belief.isEternal());
        this.temporal = !eternal || (taskTerm.isTemporal() || (_belief != null && beliefTerm.isTemporal()));

        this.parentCause = _belief != null ?
                Cause.sample(Param.causeCapacity.intValue(), _task, _belief) :
                _task.cause();

        float taskPri = _task.priElseZero();
        this.pri =
                _belief == null ?
                        Param.TaskToDerivation.valueOf(taskPri) :
                        Param.TaskBeliefToDerivation.apply(taskPri, _belief.priElseZero());

        //this.pri *= Math.max(1f, nar.amp(parentCause));

        this.premiseEviSingle = taskTruth != null ? taskTruth.evi() : Float.NaN;
        this.premiseEviDouble = beliefTruth != null ?
                //Math.max(premiseEviSingle, beliefTruth.evi()) :
                premiseEviSingle + beliefTruth.evi() :
                premiseEviSingle;


        setTTL(ttl);
        derive.can.accept(this);
    }

    @Override
    public final void tryMatch() {


        int now = now();
        try {
            //xy.replace(nar::applyTermIfPossible); //resolve to an abbreviation or other indexed term
            forEachMatch.test(this);
        } catch (Exception e) {
            logger.error("{} {}", this, e);
        } finally {
            revert(now); //undo any changes applied in conclusion
        }

    }

    @Nullable
    public long[] evidenceSingle() {
        if (evidenceSingle == null) {
            evidenceSingle =
                task.stamp();
                //Stamp.cyclic(task.stamp());
        }
        return evidenceSingle;
    }

    @Nullable
    public long[] evidenceDouble() {
        if (evidenceDouble == null) {
            float te, be, tb;
            if (task.isBeliefOrGoal()) {
                //for belief/goal use the relative evi
                te = taskTruth.evi();
                be = beliefTruth != null ? beliefTruth.evi() : 0; //beliefTruth can be zero in temporal cases
                tb = te / (te + be);
            } else {
                //for question/quest, use the relative priority
                te = task.priElseZero();
                be = belief.priElseZero();
                tb = te + be;
                tb = tb < Pri.EPSILON ? 0.5f : te / tb;
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
        derivations.clear();
        termutes.clear();
        time = ETERNAL;
        super.clear();
    }



    public Task add(Task t) {
        return derivations.merge(t, t, DUPLICATE_DERIVATION_MERGE);
    }


    final static BiFunction<Task, Task, Task> DUPLICATE_DERIVATION_MERGE = (pp, tt) -> {
        pp.priMax(tt.pri());
        ((NALTask)pp).causeMerge(tt);
        if (pp.isCyclic() && !tt.isCyclic()) {
            //clear cyclic if the other is not cyclic, for fairness
            pp.setCyclic(false);
        }
        return pp;
    };


    private final SubIfUnify uniSubAny = new SubIfUnify(this);

    private final Subst uniSub = new Subst() {

        @Override
        public @Nullable Term apply(Subterms xx) {
            Term y = super.apply(xx);
            if (y != null && !(y instanceof Bool)) {

                replaceXY(xx.sub(0), y);

                Term a = xx.sub(1);
                Term b = xx.sub(2);
                if (!a.equals(b)) {
                    replaceXY(a, b); //also include the subterms in case the structure of the compound changed signficantly the events may still hold the clue
//                    //replaceXY(b, a); //reverse mapping
                }
            }
            return y;
        }
    };
    final Functor.LambdaFunctor polarizeFunc = Functor.f2("polarize", (subterm, whichTask) -> {
        if (subterm instanceof Bool)
            return subterm;

        Truth compared;
        if (whichTask.equals(PremiseRule.Task)) {
            compared = taskTruth;
        } else {
            compared = beliefTruth;
        }
        if (compared == null)
            return Null;
        else
            return compared.isNegative() ? subterm.neg() : subterm;
    });

    private static final Atomic _tlRandom = (Atomic) $.the("termlinkRandom");
    final Functor.LambdaFunctor termlinkRandomProxy = Functor.f1("termlinkRandom", (x) -> {
        x = anon.get(x);
        if (x == null)
            return Null;

        Term y = $.func(_tlRandom, x).eval(nar.concepts.functors);
        if (y!=null && y.op().conceptualizable)
            return anon.put(y);
        return Null;
    });

    //    /**
//     * experimental memoization of transform results
//     */
//    @Override
//    @Nullable
//    public Term transform(@NotNull Term pattern) {
//
//        if (!Param.DERIVATION_TRANSFORM_CACHE) {
//            return super.transform(pattern); //xy.get(pattern); //fast variable resolution
//        }
//
//        Term y = xy(pattern);
//        if (y!=null) {
//            if (pattern instanceof Variable || y.vars(null) == 0) {
////                if (xy.get(y)!=null)
////                    System.out.println(y + " -> " + xy.get(y));
//
//                return y;
//            }
//            pattern = y;
//        }
//        if (pattern instanceof Atomic) {
////            if (xy.get(x)!=null)
////                System.out.println(x + " -> " + xy.get(x));
//            return pattern;
//        }
//
////        if (x.OR(xx -> xx == Null))
////            return Null;
//
//        Transformation key = Transformation.the((Compound) pattern, currentMatch);
//
//        //avoid recursive update problem on the single thread by splitting the get/put
//        Term value = transformsCache.getIfPresent(key);
//        if (value != null)
//            return value;
//
//        value = super.transform(key.pattern);
//        if (value == null)
//            value = Null;
//
//        transformsCache.put(key, value);
//
//        if (value == Null)
//            value = null;
//
//        return value;
//    }
//
//    final static class Transformation {
//        public final Compound pattern;
//        final byte[] assignments;
//        final int hash;
//
//        Transformation(Compound pattern, DynBytes assignments) {
//            this.pattern = pattern;
//            this.assignments = assignments.array();
//            this.hash = Util.hashCombine(assignments.hashCode(), pattern.hashCode());
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            //if (this == o) return true;
//            Transformation tthat = (Transformation) o;
//            if (hash != tthat.hash) return false;
//            return pattern.equals(tthat.pattern) && Arrays.equals(assignments, tthat.assignments);
//        }
//
//        @Override
//        public int hashCode() {
//            return hash;
//        }
//
//        static Transformation the(@NotNull Compound pattern, @NotNull Term[][] match) {
//
//            //TODO only include in the key the free variables in the pattern because there can be extra and this will cause multiple results that could have done the same thing
//            //FasterList<Term> key = new FasterList<>(currentMatch.length * 2 + 1);
//
//            DynBytes key = new DynBytes((2 * match.length + 1) * 8 /* estimate */);
//            pattern.append((ByteArrayDataOutput) key); //in 0th
//            key.writeByte(0);
//            for (Term[] m : match) {
//                Term var = m[0];
//                if (pattern.containsRecursively(var)) {
//                    var.append((ByteArrayDataOutput) key);
//                    key.writeByte(0);
//                    m[1].append((ByteArrayDataOutput) key);
//                    key.writeByte(0);
//                }
//            }
//            return new Transformation(pattern, key);
//        }
//
//    }


}


