package nars.derive;

import jcog.Util;
import jcog.WTF;
import jcog.data.ShortBuffer;
import jcog.decide.MutableRoulette;
import jcog.pri.ScalarValue;
import jcog.signal.meter.FastCounter;
import nars.*;
import nars.attention.What;
import nars.control.Caused;
import nars.derive.action.How;
import nars.derive.action.PatternHow;
import nars.derive.action.op.Occurrify;
import nars.derive.action.op.Taskify;
import nars.derive.premise.Premise;
import nars.derive.util.DerivationFailure;
import nars.derive.util.DerivationFunctors;
import nars.derive.util.PremiseBeliefMatcher;
import nars.eval.Evaluation;
import nars.op.Replace;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.term.*;
import nars.term.anon.AnonWithVarShift;
import nars.term.atom.*;
import nars.term.functor.AbstractInlineFunctor1;
import nars.term.util.TermTransformException;
import nars.term.util.transform.InstantFunctor;
import nars.term.util.transform.RecursiveTermTransform;
import nars.time.When;
import nars.truth.MutableTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.unify.Unify;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 * instantiated threadlocal, and recycled mutably
 */
public abstract class Derivation extends PreDerivation implements Caused, Predicate<PremiseRunnable> {

    public final Deriver deriver;

    /** premise formation (preprocessing) */
    public final PremiseBeliefMatcher beliefMatch = new PremiseBeliefMatcher();

    /** main premise derivation unifier */
    public final PremiseUnify unify = new PremiseUnify(null, null, NAL.unify.UNIFICATION_STACK_CAPACITY);

    /** post-processing, 2nd-layer uniSubst etc */
    public final UniSubst uniSubstFunctor = new UniSubst(this);


    protected final static Logger logger = LoggerFactory.getLogger(Derivation.class);

    public static final Atom Substitute = Atomic.atom("substitute");
    public static final Atom ConjWithout = Atomic.atom("conjWithout");




    /** current taskify for use in unification match */
    transient Taskify taskify = null;

    private final static int ANON_INITIAL_CAPACITY = 16;
    public final AnonWithVarShift anon;



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
            return polarize(arg, Derivation.this.taskTruth);
        }
    };

    public final Functor polarizeBelief = new AbstractInstantFunctor1("polarizeBelief") {
        @Override
        protected Term apply1(Term arg) {
            if (single) return arg.negIf(random.nextBoolean());
            else return polarize(arg, Derivation.this.beliefTruth_at_Belief);
        }
    };

    /** characterizes the present moment, when it starts and ends */
    public final When<What> when = this;

    /** current running premise, set at beginning of derivation in derive() */
    public transient Premise premise;
    public final Random random;

    private Term polarize(Term arg, MutableTruth t) {
        if (t.is()) {
			boolean tNeg = t.isNegative();
            if (tNeg != (arg instanceof Neg))
                arg = arg.neg(); //invert to expected polarity
        } else
            arg = arg.negIf(random.nextBoolean());
        return arg;
    }

    /**
     * cant be inline since the value will be cached and repeated
     */
//    final Functor polarizeRandom = Functor.f1("polarizeRandom", (arg)->random.nextBoolean() ? arg : arg.neg());
    public final Functor polarizeRandom = new AbstractInlineFunctor1("polarizeRandom") {
        @Override
        protected Term apply1(Term arg) {
            return arg.negIf(unify.random.nextBoolean());
        }
    };
    /**
     * populates retransform map
     */
    public final Replace substituteFunctor = new Replace(Derivation.Substitute) {

        @Override
        public @Nullable Term apply(Evaluation e, Subterms xx) {
            Term input = xx.sub(0), replaced = xx.sub(1), replacement = xx.sub(2);

            Term y = Replace.apply(xx, input, replaced, replacement);
            if (y != null)
                retransform.put(replaced, replacement);

            return y;
        }
    };

    public final NAR nar;


    public transient float confMin;
    public transient double eviMin;
    public transient int termVolMax;
    /**
     * whether either the task or belief are events and thus need to be considered with respect to time
     */
    public transient boolean temporal;
    public transient int ditherDT;

    /**
     * precise time that the task and belief truth are sampled
     */
    public transient long taskStart, taskEnd, beliefStart, beliefEnd; //TODO taskEnd, beliefEnd
    public transient boolean overlapDouble, overlapSingle;

    @Deprecated public transient boolean single;
    @Deprecated public final MutableTruth truth = new MutableTruth();
    @Deprecated public transient byte punc;

    /**
     * current NAR time, set at beginning of derivation
     */
    public transient long time = TIMELESS;

    public transient Task _task, _belief;
    public transient Term _beliefTerm;

    /** evi avg */
    private double eviDouble, eviSingle;

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
    public Term _taskTerm;
    private final ImmutableMap<Atomic, Term> derivationFunctors;


    public final Random random() {
        return random;
    }

    public final boolean use(int x) {
        return unify.use(x);
    }

    @Override
    public boolean test(PremiseRunnable r) {

        How a = r.action;

        if (a instanceof PatternHow.TruthifyDeriveAction)
            reset(r.truth, r.punc, r.single);

        if (NAL.TRACE)
            a.trace(this);

        try {
            a.run(this);
        } catch (Throwable t) {
            logger.error("{} {} {}", r, this, t);
            if (NAL.DEBUG)
                t.printStackTrace();
        }

        return use(NAL.derive.TTL_COST_BRANCH);
    }

    private boolean test(int i) {
        return test(this.post[i]);
    }

    public boolean unify(Term x, Term y, @Nullable Taskify finish) {
        if (finish!=null)
            taskify = finish;

        return unify.unify(x, y, finish!=null);
    }

    /** main premise unification instance */
    public final class PremiseUnify extends Unify {
        public PremiseUnify(@Nullable Op type, Random random, int stackMax) {
            super(type, random, stackMax);
        }

        protected boolean match() {

            Emotion emotion = nar.emotion;

            emotion.deriveUnified.increment();

            Taskify x = Derivation.this.taskify;

            Term y;

            try (var __ = emotion.derive_E_Run2_Subst.time()) {
                y = transformDerived.apply(x.termify.pattern(temporal));
            }

            DerivationFailure termifyFailure;
            if (null == (termifyFailure = DerivationFailure.failure(y, (byte) 0 /* dont consider punc consequences until after temporalization */, Derivation.this))) {

                Task t;

                try (var __ = emotion.derive_E_Run3_Taskify.time()) {
                    t = x.task(y, Derivation.this);
                }

                if (t != null) {
                    try (var __ = emotion.derive_F_Remember.time()) {
                        return remember(t);
                    }
                } //else: taskify faliure is handled in taskify

            } else
                termifyFailure.record(nar);

            return true; //tried.size() < forkLimit;
        }

    }


    /**
     * if using this, must setAt: nar, index, random, DerivationBudgeting
     */
    Derivation(Deriver deriver) {
        super();

        this.deriver = deriver;
        this.nar = deriver.nar();
        this.derivationFunctors = DerivationFunctors.get(this);


        this.random = nar.random();
        for (Unify u : _u) u.random = random;

        this.anon = new AnonWithVarShift(ANON_INITIAL_CAPACITY,
            Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_QUERY.bit
            //Op.VAR_DEP.bit | Op.VAR_QUERY.bit
            //Op.VAR_QUERY.bit
            //0
        ) {

            @Override
            protected boolean intern(Atomic x) {
                //dont ANOM's:
                return  !(x instanceof Img) && !(x instanceof Keyword);
            }

            @Override
            public boolean intrin(Atomic x) {
                return
                    //erased types: intern these intrins for maximum premise key re-use
                    !(x instanceof Int) && !(x instanceof AtomChar) && super.intrin(x);
            }

//            @Override
//            protected boolean intern(Atomic x) {
//                return //!(x instanceof Atom) ||
//                        !derivationFunctors.containsKey(x); //TODO better matcher
//            }

        };
    }

    static private void assertAnon(Term x, @Nullable Term y, @Nullable nars.Task cause) {
        TermTransformException e;
        if (y == null)
            e = new TermTransformException(x, null, "invalid Derivation Anon: null");
        else if (y instanceof Bool)
            e = new TermTransformException(x, y, "invalid Derivation Anon: Bool");
        else if (y instanceof Neg)
            e = new TermTransformException(x, y, "invalid Derivation Anon: Neg");
        else if (NAL.DEBUG && x instanceof Compound && x.opID() != y.opID())
            e = new TermTransformException(x, y, "invalid Derivation Anon: Op changed");
        else if (NAL.DEBUG && x.volume() != y.volume())
            e = new TermTransformException(x, y, "invalid Derivation Anon: Volume Changed");
        else
            return;

        if (cause != null)
            cause.delete();
        throw e;
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

        this.beliefTerm = beliefTerm(anon, _taskTerm, nextBeliefTerm);

        assertAnon(nextBeliefTerm, beliefTerm, nextBelief);

        return nextBelief;
    }

    //shift heuristic condition probabalities TODO refine
    final float PREMISE_SHIFT_EQUALS_ROOT = 0.02f;
    final float PREMISE_SHIFT_CONTAINS_RECURSIVELY = 0.05f;
    final float PREMISE_SHIFT_OTHER = 0.9f;
    final float PREMISE_SHIFT_RANDOM = 0.5f;

    /** t = raw task term, b = raw belief term */
    public Term beliefTerm(AnonWithVarShift anon, Term t, Term b) {

        boolean shift, shiftRandomOrCompletely;
        if (!b.hasAny(Op.Variable & t.structure())) {
            shift = shiftRandomOrCompletely = false; //unnecessary
        } else {
            float r = random.nextFloat();
            if (b.equalsRoot(t)) {
                shift = r < PREMISE_SHIFT_EQUALS_ROOT; //structural identity
            } else if (b.containsRecursively(t) || t.containsRecursively(b)) {
                shift = r < PREMISE_SHIFT_CONTAINS_RECURSIVELY;
            } else {
                shift = r < PREMISE_SHIFT_OTHER;
            }
            shiftRandomOrCompletely = shift && random.nextFloat() < PREMISE_SHIFT_RANDOM;
        }

        return shift ?
            anon.putShift(b, t, shiftRandomOrCompletely ? random : null) :
                (t.equals(b) ?
                    this.taskTerm /* re-use */ :
                    anon.put(b));
    }


    @Nullable private Truth beliefAtTask(Task nextBelief) {
        @Nullable Truth t = nextBelief.truth(taskStart, taskEnd, dur, true); //integration-calculated
        return t!=null && t.evi() >= eviMin ? t : null;
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

    private void budget(Task task, Task belief) {
        float taskPri = task.priElseZero();
        priSingle = taskPri;
        priDouble = belief == null ?
                taskPri :
                NAL.DerivationPri.apply(taskPri, belief.priElseZero());

//        if (Param.INPUT_BUFFER_PRI_BACKPRESSURE && Math.max(priDouble, priSingle) < nar.input.priMin() /* TODO cache */)
//            return false;

        this.eviSingle = task.isBeliefOrGoal() ? task.evi() : 0;
        this.eviDouble = eviSingle + (belief!=null ?
                            belief.evi()
                            :
                            0);
    }

    /**
     * called after protoderivation has returned some possible Try's
     */
    void truthifyReady() {

        Task task = _task;
        this.overlapSingle = task.isCyclic();

        Task belief = _belief;
        if (belief != null) {
            this.overlapDouble =
                Stamp.overlapAny(task, belief);
                //Stamp.overlap(this._task, _belief);
        } else {
            this.overlapDouble = false; //N/A
        }

    }

    public final void ttl(int ttl) {
        unify.setTTL(ttl); //HACK TODO use global TTL register
    }

    public void ready(int deriveTTL) {
        this.stampDouble = stampSingle = null; //TODO maybe can be re-used
        ttl(deriveTTL);
        budget(_task, _belief);

        boolean eternalCompletely = (taskStart == ETERNAL) && (_belief == null || beliefStart == ETERNAL);
        this.temporal = !eternalCompletely || Occurrify.temporal(taskTerm) || (!beliefTerm.equals(taskTerm) && Occurrify.temporal(beliefTerm));
    }

    abstract public void add(Premise p);


    /** attached unifier instances */
    final private Unify[] _u = new Unify[] { unify, uniSubstFunctor.u, beliefMatch};



    /** switch to new context */
    public void next(What w) {

        NAR n = nar;
        time = n.time();

        the(this.x = w);
        dur(w.dur());

        range(
            (long) (time - dur/2),
            (long) Math.ceil(time + dur/2)
        );

        ditherDT = n.dtDither();
        confMin = n.confMin.conf();
        eviMin = n.confMin.evi();
        termVolMax = n.termVolMax.intValue();

        w.tryCommit();

        float uttd = nar.unifyDTToleranceDurs.floatValue();
        int dtTolerance =
            //n.dtDither(); //FINE
            //Math.round(n.dtDither() * n.unifyTimeToleranceDurs.floatValue()); //COARSE
            Math.round(dur * uttd); //COARSE
            //Math.max(n.dtDither(), Math.round(w.dur() * n.unifyTimeToleranceDurs.floatValue())); //COARSE

        for (Unify u : _u)
            u.dtTolerance = dtTolerance;

        w.derivePri.reset(this);

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

            long[] e = Stamp.merge(_task.stamp(), _belief.stamp(), (float) tb, unify.random);
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
                + ' ' + unify.toString();
    }

    /**
     * include any .clear() for data structures in case of emergency we can continue to assume they will be clear on next run()
     */



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

    public final boolean remember(Task t) {

        x.accept(t);
        nar.emotion.deriveTask.increment();

        return unify.use(NAL.derive.TTL_COST_TASK_REMEMBER);
    }

    /** returns appropriate Emotion counter representing the result state  */
    FastCounter run(Premise p, final int deriveTTL) {

        short[] can;

        Emotion e = nar.emotion;
        try (var __ = e.derive_C_Pre.time()) {
            can = preReady(p);
        }

        if (can.length == 0)
            return e.premiseUnderivable1;

        int valid = 0, lastValid = -1;
        PremiseRunnable[] post = this.post;

        try (var __ = e.derive_D_Truthify.time()) {
            this.truthifyReady();

            How[] branch = this.deriver.program.branch;
            for (int i = 0; i < can.length; i++) {
                if ((post[i].pri(branch[can[i]], this)) > Float.MIN_NORMAL) {
                    lastValid = i;
                    valid++;
                }
            }
        }

        if (valid == 0)
            return e.premiseUnderivable2;

        try (var __ = e.derive_E_Run.time()) {

            this.ready(deriveTTL);  //use remainder

            if (valid == 1) {//optimized 1-option case

                //while (post[lastValid].run()) { }

                test(lastValid);

            } else {

                if (valid < can.length) {
                    //sort the valid to the first slots for fastest roulette iteration on the contiguous valid subset
                    //otherwise any order here is valid
                    Arrays.sort(post, 0, can.length, sortByPri);
                }

                float[] pri = new float[valid];
                for (int i = 0; i < valid; i++)
                    pri[i] = post[i].pri;
                MutableRoulette.run(pri, random, wi -> 0, this::test);

                //alternate roulette:
                //  int j; do { j = Roulette.selectRoulette(valid, i -> post[i].pri, d.random);   } while (post[j].run());
            }


        }

        return e.premiseRun;
    }

    private short[] preReady(Premise p) {

        this.premise = p;

        this._task = resetTask(p.task(), this._task);

        Term nextBeliefTerm = p.beliefTerm();
        this._belief = resetBelief(p.belief(), this._beliefTerm = nextBeliefTerm);


        return deriver.program.pre.apply(this);
    }


    //
//    private void ensureClear() {
//
//        if (size!=0)
//            throw new WTF();
//        if (!termutes.isEmpty())
//            throw new WTF();
//
//    }
    @Deprecated void reset(Truth truth, byte punc, boolean single) {
        //ensureClear();

        unify.clear();

        this.retransform.clear();
        this.truth.set(truth);
        this.punc = punc;
        this.single = single;
    }

    public boolean hasBeliefTruth() {
        return _belief!=null && (beliefTruth_at_Belief.is() || beliefTruth_at_Task.is());
    }


    private static final Comparator<? super PremiseRunnable> sortByPri = (a, b)->{
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
    public final float preAmp(byte taskPunc, byte concPunc) {
        return x.derivePri.preAmp(concPunc);
    }

    public final float parentPri() {
        return single ? priSingle : priDouble;
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


    public long[] evidence() {
        return single ? evidenceSingle() : evidenceDouble();
    }

    @Override
    public ShortBuffer preDerive() {
        canCollector.clear();

        deriver.program.what.test(this);
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

	@Override
	@Nullable public final Term why() {
		return premise.why();
	}


	abstract static class AbstractInstantFunctor1 extends AbstractInlineFunctor1 implements InstantFunctor<Evaluation> {

        AbstractInstantFunctor1(String atom) {
            super(atom);
        }
    }


    /**
     * should be created whenever a different NAR owns this Derivation instance, if ever
     */
	public final RecursiveTermTransform transformDerived = new RecursiveTermTransform() {

		@Override
		public Term applyAtomic(Atomic a) {
        	if (a instanceof Variable)
				return unify.resolveVar((Variable)a);
        	else {
        		//atomic constant
				if (a instanceof Atom) {

					if (a == DerivationFunctors.TaskTerm)
						return taskTerm;
					else if (a == DerivationFunctors.BeliefTerm)
						return beliefTerm;

					Term b = derivationFunctors.get(a);

					if (b!=null) {
						if (NAL.DEBUG) {
							if (b == DerivationFunctors.TaskTerm)
								throw new WTF("should have been detected earlier"); //return taskTerm;
							else if (b == DerivationFunctors.BeliefTerm)
								throw new WTF("should have been detected earlier"); //return beliefTerm;
						}
						return b;
					}
				}

				return a;
			}
		}

        @Override
        public final boolean evalInline() {
            return true;
        }

    };


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
