package nars.control;

import jcog.pri.Pri;
import jcog.version.Versioned;
import nars.*;
import nars.derive.DeriverRoot;
import nars.derive.rule.PremiseRule;
import nars.derive.time.DeriveTime;
import nars.op.Subst;
import nars.op.SubstUnified;
import nars.op.data.differ;
import nars.op.data.intersect;
import nars.op.data.union;
import nars.task.DerivedTask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.anon.Anon;
import nars.term.anon.CachedAnon;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.pred.PrediTerm;
import nars.term.sub.Subterms;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends ProtoDerivation {

    public static final Atomic TaskTerm = Atomic.the("_taskTerm");
    public static final Atomic BeliefTerm = Atomic.the("_beliefTerm");


    public NAR nar;


//    public final ByteShuffler shuffler = new ByteShuffler(64);

    public final BatchActivation activator = new BatchActivation();

    public final Anon anon;

    /**
     * temporary buffer for derivations before input so they can be merged in case of duplicates
     */
    public final Map<DerivedTask, DerivedTask> derivations = new LinkedHashMap<>();

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
    public float confMin;


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


    /**
     * evidential overlap
     */
    public float overlapDouble, overlapSingle;

    public float premisePri;
    public short[] parentCause;

    public DeriverRoot deriver;
    public boolean single;
    public int parentComplexity;


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

        anon = new CachedAnon(16);
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
                nar.get(Atomic.the("dropAnyEvent")),
                nar.get(Atomic.the("dropAnySet")),
                nar.get(Atomic.the("without")),
                nar.get(Atomic.the("conjWithout")),
                nar.get(Atomic.the("conjDropIfEarliest")),
                nar.get(Atomic.the("conjEvent")),
                nar.get(Atomic.the("ifConjCommNoDepVars")),
                nar.get(Atomic.the("indicesOf")),
                nar.get(Atomic.the("substDiff")),
        };
    }

    /**
     * only returns derivation-specific functors.  other functors must be evaluated at task execution time
     */
    @Override
    public final Termed apply(Term x) {
        return applyTermIfPossible(x);
    }

    @Override
    public final Term applyTermIfPossible(Term x) {

        if (x instanceof Bool)//assert (!(x instanceof Bool));
            return x;

        if (x instanceof Atom) {
            Termed f = derivationFunctors.get(x);
            if (f != null)
                return f.term();
        }


        Term y = xy(x);
        if (y != null) {
            return y; //an assigned substitution, whether a variable or other type of term
        } else {
            return x;
        }

//        else if (x.hasAny(substitutionVector)) {
//            return super.applyTermIfPossible(x);
//        } else {
//            return x;
//        }
    }

    public Derivation cycle(NAR nar, DeriverRoot deriver) {
        NAR pnar = this.nar;
        if (pnar != nar) {
            init(nar);
        }

        long now = nar.time();
        if (now != this.time) {
            this.time = now;
            this.dur = nar.dur();
            this.freqRes = nar.freqResolution.floatValue();
            this.confRes = nar.confResolution.floatValue();
            this.confMin = nar.confMin.floatValue();
            this.termVolMax = nar.termVolumeMax.intValue();
            //transformsCache.cleanUp();
        }

        this.deriver = deriver;

        return this;
    }

    public void init(NAR nar) {
        this.clear();
        this.nar = nar;
        this.random = nar.random();

        Functor.LambdaFunctor polarizeFunc = Functor.f2("polarize", (subterm, whichTask) -> {
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

        Termed[] derivationFunctors = new Termed[]{
                new uniSubAny(this),
                new uniSub(this),
                polarizeFunc
        };
        Map<Term, Termed> m = new HashMap<>(derivationFunctors.length + 2);

        for (Termed x : ruleFunctors(nar))
            m.put(x.term(), x);

        for (Termed x : derivationFunctors)
            m.put(x.term(), x);

        m.put(TaskTerm, () -> taskTerm);
        m.put(BeliefTerm, () -> beliefTerm);
        this.derivationFunctors = Maps.immutable.ofMap(m);
    }

    public Derivation reset() {
        super.reset();

        anon.clear();

        this.task = this.belief = null;

        this.beliefTruth = this.taskTruth = null;

        this.forEachMatch = null;
        this.concTruth = null;
        this.concPunc = 0;
        this.truthFunction = null;
        this.single = false;
        this.evidenceDouble = evidenceSingle = null;
        this.dtSingle = this.dtDouble = null;
        this.concOcc[0] = this.concOcc[1] = ETERNAL;
        this._task = this._belief = null;
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


    protected void setTruth() {

        Truth taskTruth;
        switch (this.taskPunc = task.punc()) {
            case QUESTION:
            case QUEST:
                taskTruth = null;
                break;
            default:
                taskTruth = task.truth();
        }
        this.taskPolarity = (this.taskTruth = taskTruth) != null ? polarity(taskTruth) : 0;

        if (belief != null) {
            long ts = task.start();
            long te = task.end();

            if ((this.beliefTruth = belief.truth(belief.theNearestTimeWithin(ts, te), dur))!=null) {
                this.beliefPolarity = polarity(this.beliefTruth);
            } else {
                this.beliefPolarity = 0;
            }

        } else {
            this.beliefTruth = null;
            this.beliefPolarity = 0;
        }

    }

    /**
     * setup the derivation for the ProtoDerivation stage
     *
     * must call reset() immediately before or after calling this.
     *
     * TODO move some of these to a superclass method which sets the variables in its scope
     */
    public void proto(Task _task, final Task _belief, Term _beliefTerm) {


        final Task task = this.task = anon.put(this._task = _task);
        if (task == null)
            throw new NullPointerException(_task + " could not be anonymized: " +
                    _task.term().anon() + " , " + anon.put(this._task = _task));

        if (_belief != null) {
            if ((this.belief = anon.put(this._belief = _belief)) == null)
                throw new NullPointerException(_belief + " could not be anonymized");
        } else {
            this.belief = null;
        }
        final Term taskTerm = this.taskTerm = task.term();
        final Term beliefTerm = this.beliefTerm = anon.put(_beliefTerm);


        this._taskStruct = taskTerm.structure();
        this._taskOp = taskTerm.op().id;

        this._beliefStruct = beliefTerm.structure();

        Op bOp = beliefTerm.op();
        this._beliefOp = bOp.id;

        setTruth();


    }

    /** called after protoderivation has returned some possible Try's */
    public boolean derive() {
//        int ttv = taskTerm.vars();
//        if (ttv > 0 && bt.vars() > 0) {
//            bt = bt.normalize(ttv); //shift variables up to be unique compared to taskTerm's
//        }
        this.parentComplexity =
                //Util.sum(
                Math.max(
                        taskTerm.complexity(), beliefTerm.complexity()
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
                    (task.isCyclic() /* || belief.isCyclic()*/) ?
                            1 :
                            Stamp.overlapFraction(taskStamp, beliefStamp);

        } else {
            this.overlapDouble = 0;
        }


        this.eternal = task.isEternal() && (_belief == null || _belief.isEternal());
        this.temporal = !eternal || (taskTerm.isTemporal() || (_belief != null && beliefTerm.isTemporal()));

        this.parentCause = _belief != null ?
                Cause.zip(nar.causeCapacity.intValue(), _task, _belief) :
                _task.cause();

        float taskPri = _task.priElseZero();
        this.premisePri =
                //p.priElseZero(); //use the premise pri directly
                _belief == null ? taskPri : Param.TaskBeliefDerivation.apply(taskPri, _belief.priElseZero());

//        float parentValue =
//                //nar.evaluate(parentCause); /* value of the parent cause as a multiplier above and below 1.0x */
//                0.5f * nar.evaluate(parentCause); /* can decrease only (0..1.0) */
//                //Pri.EPSILON + 0.5f * nar.evaluate(parentCause); //bounded 0..1.0
//        this.premisePri *= parentValue;


        this.premiseEviSingle = taskTruth != null ? taskTruth.evi() : 0;
        this.premiseEviDouble = beliefTruth != null ?
                Math.min(premiseEviSingle, beliefTruth.evi()) : //to be fair to the lesser confidence
                premiseEviSingle;


        return true;
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
                //for belief/goal use the relative conf
                te = taskTruth.conf();
                be = beliefTruth != null ? beliefTruth.conf() : 0; //beliefTruth can be zero in temporal cases
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
        return task + " " + (belief != null ? belief : beliefTerm)
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

    public int getAndSetTTL(int next) {
        int before = this.ttl;
        this.ttl = next;
        return before;
    }

    /**
     * called at the end of the cycle, input all generated derivations
     */
    public int commit(Consumer<Collection<DerivedTask>> target) {

        activator.commit(nar);

        //experimental normalization
        final float[] priSum = {0};
        derivations.values().forEach(dd -> priSum[0] = dd.priElseZero());
        if (priSum[0] > 1f) {
            float factor = 1f/priSum[0];
            derivations.values().forEach(dd -> dd.priMult(factor));
        }

        int s = derivations.size();
        if (s > 0) {
            nar.emotion.taskDerived.increment(s);
            target.accept(derivations.values());
            derivations.clear();
        }
        return s;
    }

    public static class uniSubAny extends SubstUnified {

        final static Atom func = (Atom) $.the("subIfUnifiesAny");

        public uniSubAny(Derivation parent) {
            super(func, parent);
        }

        @Override
        public Term apply(Subterms xx) {
            Term y = super.apply(xx);
//            if (y != null && y != Null) {
//                Term x = xx.sub(0);
//                if (!x.equals(y))
//                    parent.putXY(x, y); //store the transformation
//            }
            return y;
        }
    }

    public static class uniSub extends Subst {

        final static Atom func = (Atom) $.the("substitute");
        private final Derivation parent;

        public uniSub(Derivation parent) {
            super(func);
            this.parent = parent;
        }

        @Override
        public @Nullable Term apply(Subterms xx) {
            Term y = super.apply(xx);
//            if (y != null && y != Null) {
//                Term x = xx.sub(0);
//                if (!x.equals(y))
//                    parent.putXY(x, y); //store the outer transformation
//            }
            return y;
        }
    }

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


