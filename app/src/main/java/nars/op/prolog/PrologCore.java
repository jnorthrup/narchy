package nars.op.prolog;

import alice.tuprolog.*;
import com.google.common.collect.Iterators;
import jcog.Util;
import jcog.math.Range;
import nars.*;
import nars.control.channel.CauseChannel;
import nars.index.concept.ConceptIndex;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.NormalizedVariable;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.util.time.Tense.DTERNAL;
import static nars.util.time.Tense.ETERNAL;

/**
 * Prolog mental coprocessor for accelerating reasoning
 * WARNING - introduces cognitive distortion
 * <p>
 * Causes a NARProlog to mirror certain activity of a NAR.  It generates
 * prolog terms from NARS beliefs, and answers NARS questions with the results
 * of a prolog solution (converted to NARS terms), which are input to NARS memory
 * with the hope that this is sooner than NARS can solve it by itself.
 */
public class PrologCore extends PrologAgent implements Consumer<Task> {

    final static Logger logger = LoggerFactory.getLogger(PrologCore.class);

    public static final String AxiomTheory;
    public static final alice.tuprolog.Term ONE = new NumberTerm.Int(1);
    public static final alice.tuprolog.Term ZERO = new NumberTerm.Int(0);

    static {
        String a;
        try {
            a = Util.inputToString(
                    PrologCore.class.getClassLoader()
                            .getResourceAsStream("nars/prolog/default.prolog")
            );
        } catch (Throwable e) {
            logger.error("default.prolog {}", e.getMessage());
            a = "";
        }
        AxiomTheory = a;
    }

    private final NAR nar;

    final Map<Term, alice.tuprolog.Term> beliefs = new ConcurrentHashMap();

    /**
     * beliefs above this expectation will be asserted as prolog beliefs
     */
    @Range(min = 0.5, max = 1.0)
    public final MutableFloat trueFreqThreshold = new MutableFloat(0.9f);

    /**
     * beliefs below this expectation will be asserted as negated prolog beliefs
     */
    @Range(min = 0, max = 0.5)
    public final MutableFloat falseFreqThreshold = new MutableFloat(0.1f);

    /**
     * beliefs above this expectation will be asserted as prolog beliefs
     */
    @Range(min = 0, max = 1.0)
    public final MutableFloat confThreshold = new MutableFloat(0.75f);


    @Range(min = 0, max = 1.0)
    public final MutableFloat answerConf = new MutableFloat(confThreshold.floatValue() * 0.9f);

//    private final float existingAnswerThreshold = 0.5f;

    private final long timeoutMS = 50;
    private final CauseChannel<ITask> in;


    /*final ObjectBooleanHashMap<Term> beliefs = new ObjectBooleanHashMap() {


    };*/

    public PrologCore(NAR n) {
        this(n, AxiomTheory);
    }

    public static class MyClauseIndex extends MutableClauseIndex {


        public MyClauseIndex(ConceptIndex t) {
//            this.index = t;
        }

//
//        @Override
//        public void add(String key, ClauseInfo d, boolean first) {
//            super.add(key, d, first);
//        }
    }

    public PrologCore(NAR n, String theory) {
        super(theory, new MyClauseIndex(n.concepts)); //, new NARClauseIndex(n));

        if (Param.DEBUG)
            setSpy(true);

        this.in = n.newChannel(this);
        this.nar = n;


        n.eventTask.on(this);
    }

    @Override
    public void accept(Task task) {

        if (task.isBelief()) {
            //if task is the current highest one, otherwise ignore because we will already be using something more confident or relevant
            if (task.isEternal()) { // && (task == cc.beliefs().matchEternal())) {
                int dt = task.term().dt();
                if (dt == 0 || dt == DTERNAL) { //only nontemporal or instant for now
                    float c = task.conf();
                    if (c >= confThreshold.floatValue()) {
                        float f = task.freq();
                        float t = trueFreqThreshold.floatValue();
                        if (f > t)
                            believe(task, true);
                        else if (f < 1f - t)
                            believe(task, false);
                    }
                }
                /* else: UNSURE */
            }
        } else if (task.isQuestion()) {
            if (task.isEternal() || task.start() == nar.time()) {
                question(task);
            }
        }


        //TODO if task is goal then wrap as goal belief
    }

    protected void believe(Task t, boolean truth) {


        boolean _truth = truth;
        Term ct = t.term();

        if (!ct.hasAny(Op.ConstantAtomics))
            return; //ignore if it contains no atoms (all variables)

        beliefs.computeIfAbsent(ct, (pp) -> {

//            if (prev != null) {
//                if (prev.sub(prev.subs()-1).equals(ONE) ^ truth) {
//                    //retract previous only do this if opposite the truth of this
//                    solve(retraction(prev));
//                }
//                else {
//                    //unchanged
//                    return prev;
//                }
//
//            }

            Struct next = (Struct) pterm(t.term());

            if (!_truth) {
                if (t.op() == IMPL) {
                    next = new Struct(":-", negate(next.subResolve(1)), next.subResolve(0));
                } else {
                    next = negate(next);
                }
            }

            Solution s = solve(assertion(next));
            if (s.isSuccess())
                logger.info("believe {}", next);
            else
                logger.warn("believe {} failed", next);

            return next;
        });


    }

    //TODO async
    protected void question(Task question) {
        Term tt = question.term();
        /*if (t.op() == Op.NEGATE) {
            //unwrap negation
            tt = ((Compound)tt).term(0);
            truth = !truth;
        }*/

        alice.tuprolog.Term questionTerm =
                //new Struct("t",
                pterm(tt);//, new Var("F"));
        //new WrappedTerm(tt);

        //TODO limit max # of inputs
        logger.info("solve {}", questionTerm);

        solve(questionTerm, (answer) -> {

            // supply input an answer to the NAR

            switch (answer.result()) {
                case EngineRunner.TRUE:
                case EngineRunner.TRUE_CP:


                    answer(question, answer);

                    break;
                case EngineRunner.FALSE:
                    //EngineRunner.False does not mean NAL false. it means that nothing is known.
                    //if the value is believed to be false, it will be expressed with a negation functor
                    //around the term

                    //answer(question, answer, false);
                    break;
                default:
                    //no known solution, remain silent
                    break;
            }
        }, timeoutMS);

    }

    private void answer(Task question, Solution answer) {
        try {
            Term yt = nterm(answer.goal);

            Task y = Task.tryTask(yt, BELIEF, $.t(1f, answerConf.floatValue()), (term, truth)->{
                Task t = new NALTask(term, BELIEF, truth,
                        nar.time(), ETERNAL, ETERNAL, nar.time.nextStampArray());
                t.pri(nar.priDefault(BELIEF));
                t.log("Prolog Answer");
                return t;
            });



            if (y!=null) {
                logger.info("answer {}\t{}", question, y);
                in.input(y);
            }

        } catch (Exception e) {
            logger.error("answer {} {} {}", question, answer, e);
        }
    }

    private static Term nterm(Struct s, int subterm) {
        return nterm(s.sub(subterm));
    }

    private static Term[] nterms(alice.tuprolog.Term[] t) {
        return Util.map(PrologCore::nterm, new Term[t.length], t);
    }

    private static Term[] nterms(Struct s) {
        int len = s.subs();
        Term[] n = new Term[len];
        for (int ni = 0; ni < len; ni++) {
            if ((n[ni] = nterm(s.subResolve(ni))) == null)
                return null; //abort
        }
        return n;
    }

    //TODO use wrapper classes which link to the original terms so they can be re-used instead o reallocating fresh ones that will equals() anyway
    private static Term nterm(alice.tuprolog.Term t) {
        if (t instanceof alice.tuprolog.Term) {
            Struct s = (Struct) t;
            if (s.subs() > 0) {
                switch (s.name()) {

                    case "-->":
                        return theTwoArity(Op.INH, s);
                    case "<->":
                        return theTwoArity(Op.SIM, s);
//                    case "<=>":
//                        return theTwoArity(Op.EQUI, s);
                    case "==>":
                        return theTwoArity(Op.IMPL, s);

                    case "~":
                        return theTwoArity(Op.DIFFi, s);
                    case "-":
                        return theTwoArity(Op.DIFFe, s);


                    case "[":
                        return SETi.the((nterms(s)));
                    case "{":
                        return SETe.the((nterms(s)));

                    case "&":
                        return SECTe.the((nterms(s)));
                    case "|":
                        return SECTi.the((nterms(s)));

                    case "*":
                        return PROD.the((nterms(s)));
                    case "&&":
                        return CONJ.the((nterms(s)));
                    case "||":
                        return $.disj(nterms(s));
                    case "not":
                        return (nterm(s, 0).neg());


                    default:
                        return $.func(unwrapAtom(s.name()), nterms(((Struct) t).subArrayShared()));
                }
            } else {
                String n = s.name();
                if (n.startsWith("'#")) {
                    //VarDep which exists as an Atom (not Var) in Prolog
                    return $.varDep(n.substring(2, n.length() - 1));
                }
                //Atom
                return $.the(unwrapAtom(n));
            }
        } else if (t instanceof Var) {
            return $.varDep(((Var) t).name());
            //throw new RuntimeException(t + " untranslated");
        } else {

            throw new RuntimeException(t + " untranslated");
        }
    }


    private static String unwrapAtom(String n) {
        if (n.charAt(0)=='_')
            n = n.substring(1);
        return n;
    }
    private static String wrapAtom(String n) {
        return "_" + n;
    }

    private static Term theTwoArity(Op inherit, Struct s) {
        return inherit.the(nterm(s, 0), nterm(s, 1));
    }


    public static alice.tuprolog.Term assertion(alice.tuprolog.Term p) {
        return new Struct("assertz", p);
    }

    public static alice.tuprolog.Term retraction(alice.tuprolog.Term p) {
        return new Struct("retract", p);
    }

    public static Struct negate(alice.tuprolog.Term p) {
        return new Struct("--", p); //TODO issue retraction on the opposite? ex: retract(x), assertz(not(x))
    }

    public static alice.tuprolog.Term[] psubterms(final Compound subtermed) {
        int l = subtermed.subs();
        alice.tuprolog.Term[] p = new alice.tuprolog.Term[l];
        for (int i = 0; i < l; i++) {
            p[i] = pterm(subtermed.sub(i));
        }
        return p;
    }

    public static alice.tuprolog.Term tterm(String punc, final alice.tuprolog.Term nalTerm, boolean isTrue) {
        return new Struct(punc, nalTerm, isTrue ? ONE : ZERO);
    }

    //NARS term -> Prolog term
    public static alice.tuprolog.Term pterm(final Term term) {
        if (term instanceof Compound) {
            Op op = term.op();
            alice.tuprolog.Term[] st = psubterms(((Compound) term));
            switch (op) {
                case IMPL:
                    return new Struct(":-", st[1], st[0] /* reversed */);
                case CONJ:
                    return new Struct(",", st);
                case NEG:
                    return new Struct(/*"\\="*/"not", st);
                case PROD:
                    return new Struct(st); //list
                case INH:
                    Term pred = term.sub(1);
                    if (pred.op() == ATOM) {
                        Term subj = term.sub(0);
                        if (subj.op() == PROD) {
                            alice.tuprolog.Term args = st[0];
                            return new Struct(wrapAtom(pred.toString()),
                                    args instanceof alice.tuprolog.Term ?
                                            Iterators.toArray(((Struct) st[0]).listIterator(), alice.tuprolog.Term.class) :
                                            new alice.tuprolog.Term[]{args});
                        }
                    }
                    break;

            }

            return new Struct(op.str, st);
        } else if (term instanceof NormalizedVariable) {
            switch (term.op()) {
                case VAR_QUERY:
                case VAR_PATTERN:
                case VAR_DEP: //?? as if atomic
                case VAR_INDEP: //??
                    return new Var("_" + (((NormalizedVariable) term).anonNum()));

                //return new Struct("'#" + ((Variable) term).id() + '\'');
            }
        } else if (term instanceof Atomic) {
            return new Struct(wrapAtom(term.toString()));
        }

        throw new UnsupportedOperationException();

//        //CharSequence s = termString(term);
//        if (term instanceof Statement) {
//            Statement i = (Statement)term;
//            String predicate = classPredicate(i.getClass());
//            alice.tuprolog.Term  subj = alice.tuprolog.Term (i.getSubject());
//            alice.tuprolog.Term  obj = alice.tuprolog.Term (i.getPredicate());
//            if ((subj!=null) && (obj!=null))
//                return new Struct(predicate, subj, obj);
//        }
//        else if ((term instanceof SetTensional) || (term instanceof Product) /* conjunction */) {
//            Compound s = (Compound)term;
//            String predicate = classPredicate(s.getClass());
//            alice.tuprolog.Term [] args = alice.tuprolog.Term s(s.term);
//            if (args!=null)
//                return new Struct(predicate, args);
//        }
//        //Image...
//        //Conjunction...
//        else if (term instanceof Negation) {
//            alice.tuprolog.Term  np = alice.tuprolog.Term (((Negation)term).term[0]);
//            if (np == null) return null;
//            return new Struct("negation", np);
//        }
//        else if (term.getClass().equals(Variable.class)) {
//            return getVariable((Variable)term);
//        }
//        else if (term.getClass().equals(Atom.class)) {
//            return new Struct(pescape(term.toString()));
//        }
//        else if (term instanceof Compound) {
//            //unhandled type of compound term, store as an atomic string
//            //NOT ready yet
//            if (allTerms) {
//                return new Struct('_' + pescape(term.toString()));
//            }
//        }
//
//        return null;
    }

//    /** wraps a NARS term in a prolog term */
//    static class WrappedTerm extends Struct {
//        public final Term t;
//
//        public WrappedTerm(Term t) {
//            super();
//            this.t = t;
//        }
//
//        @Override
//        public int getArity() {
//            return t.size();
//        }
//
//        @Override
//        public alice.tuprolog.Term getArg(int index) {
//            if (t instanceof Compound) {
//                Term sub = ((Compound)t).term(index);
//                return pterm(sub); //maybe wrap
//            }
//            return null;
//        }
//
//        @Override
//        public alice.tuprolog.Term copyGoal(AbstractMap<Var, Var> vars, int idExecCtx) {
//            return this;
//        }
//
//        @Override
//        public alice.tuprolog.Term copyResult(Collection<Var> goalVars, List<Var> resultVars) {
//            return this;
//        }
//    }

//    /** adapter that exposes a NAR as a ClauseIndex */
//    static class NARClauseIndex implements ClauseIndex {
//
//        private final NAR nar;
//
//        //final RUCache<Term,> cache = new RUCache(256);
//
//        public NARClauseIndex(NAR n) {
//            this.nar = n;
//        }
//
//        @Override
//        public FamilyClausesList get(String key) {
//
//            return null;
//        }
//
//        @Override
//        public Iterator<ClauseInfo> iterator() {
//            return Iterators.emptyIterator();
//        }
//
//        @Override
//        public void add(String key, ClauseInfo d, boolean first) {
//
//        }
//
//        @Override
//        public List<ClauseInfo> getPredicates(alice.tuprolog.Term h) {
//            if (h instanceof WrappedTerm) {
//                WrappedTerm w = (WrappedTerm)h;
//                Concept c = nar.concept(w.t);
//
//                //TODO c.getIfAbsent(ClauseInfo.class,
//
//                ClauseInfo clause = c.get(ClauseInfo.class);
//                if (clause == null) {
//                    clause = new ClauseInfo(w, null);
//                    c.put(ClauseInfo.class, clause);
//                }
//                return Collections.singletonList(clause);
//            }
//            return null;
//        }
//
//        @Override
//        public FamilyClausesList remove(String key) {
//            //N/A
//            return null;
//        }
//
//        @Override
//        public void clear() {
//            //N/A
//        }
//
//
//    }

    //    private static Term getVar(Var v) {
    //        //assume it is a dependent variable
    //        return new Variable('#' + v.getName());
    //    }
    //
    //
    //    private static Var getVariable(Variable v) {
    //        if (v.hasVarIndep())
    //            return new Var('I' + v.getIdentifier());
    //        if (v.hasVarQuery())
    //            return new Var("Q" + nextQueryID++);
    //        if (v.hasVarDep()) //check this
    //            return new Var("D" + (variableContext) + '_' + v.getIdentifier());
    //        return null;
    //    }
    //
    //    /** Prolog term --> NARS statement */
    //    public static Term nterm(final alice.tuprolog.Term  term) {
    //
    //        if (term instanceof Struct) {
    //            Struct s = (Struct)term;
    //            int arity = s.getArity();
    //            String predicate = s.getName();
    //            if (arity == 0) {
    //                return Atom.get(unpescape(predicate));
    //            }
    //            if (arity == 1) {
    //                switch (predicate) {
    //                    case "negation":
    //                        return Negation.make(nterm(s.getArg(0)));
    ////                    default:
    ////                        throw new RuntimeException("Unknown 1-arity nars predicate: " + predicate);
    //                }
    //            }
    //            switch (predicate) {
    //                case "product":
    //                    Term[] a = nterm(s.getArg());
    //                    if (a != null) return Product.make(a);
    //                    else return null;
    //                case "setint":
    //                    Term[] b = nterm(s.getArg());
    //                    if (b!=null) return SetInt.make(b);
    //                    else return null;
    //                case "setext":
    //                    Term[] c = nterm(s.getArg());
    //                    if (c!=null) return SetExt.make(c);
    //                    else return null;
    //
    //            }
    //
    //            if (arity == 2) {
    //                Term a = nterm(s.getArg(0));
    //                Term b = nterm(s.getArg(1));
    //                if ((a!=null) && (b!=null)) {
    //                    switch (predicate) {
    //                        case "inheritance":
    //                            return Inheritance.make(a, b);
    //                        case "similarity":
    //                            return Similarity.make(a, b);
    //                        case "implication":
    //                            return Implication.make(a, b);
    //                        case "equivalence":
    //                            return Equivalence.makeTerm(a, b);
    //                        //TODO more types
    ////                        default:
    ////                            throw new RuntimeException("Unknown 2-arity nars predicate: " + predicate);
    //
    //
    //                    }
    //                }
    //            }
    //            System.err.println("nterm() does not yet support translation to NARS terms of Prolog: " + term);
    //        }
    //        else if (term instanceof Var) {
    //            Var v = (Var)term;
    //            alice.tuprolog.Term  t = v.getTerm();
    //            if (t!=v) {
    //                //System.out.println("Bound: " + v + " + -> " + t + " " + nterm(t));
    //                return nterm(t);
    //            }
    //            else {
    //                //System.out.println("Unbound: " + v);
    //                //unbound variable, is there anything we can do with it?
    //                return getVar(v);
    //            }
    //        }
    //        else if (term instanceof nars.tuprolog.Number) {
    //            nars.tuprolog.Number n = (nars.tuprolog.Number)term;
    //            return Atom.get('"' + String.valueOf(n.doubleValue()) + '"');
    //        }
    //
    //        return null;
    //    }


    //    public final NAR nar;
    //    public final NARTuprolog prolog;
    //
    //    Theory axioms;
    //
    //    private float trueThreshold = 0.80f;
    //    private float falseThreshold = 0.20f;
    //    private float confidenceThreshold;
    //    private final Map<Sentence,alice.tuprolog.Term > beliefs = new HashMap();
    //
    //    private boolean eternalJudgments = true;
    //    private boolean presentJudgments = false;
    //
    //    /** how much to scale the memory's duration parameter for this reasoner's "now" duration; default=1.0 */
    //    int durationMultiplier = 1.0f;
    //
    //    /** how often to remove temporally irrelevant beliefs */
    //    @Deprecated float forgetCyclePeriod; ///TODO use a Map<Long,belief> indexed by expiration time, so they can be removed efficiently
    //    private long lastFlush;
    //    private int durationCycles;
    //
    //    static boolean allTerms = false;
    //
    //    /** in seconds */
    //    float maxSolveTime;
    //    float minSolveTime;
    //
    //    /** max # answers returned in response to a question */
    //    int maxAnswers = 3;
    //
    //    boolean reportAssumptions = false;
    //    boolean reportForgets = false;
    //    boolean reportAnswers = false;
    //
    //
    //
    //    public static final Class[] telepathicEvents = { Events.ConceptBeliefAdd.class, Events.ConceptBeliefRemove.class, Events.ConceptQuestionAdd.class, IN.class, OUT.class, Answer.class };
    //
    //    public static final Class[] inputOutputEvents = { IN.class, OUT.class };
    //    private InputMode inputMode = InputMode.InputTask;
    //
    //    //serial #'s
    //    static long nextQueryID = 0;
    //    static long variableContext = 0;
    //
    //    public NARPrologMirror(NAR nar, float minConfidence, boolean telepathic, boolean eternalJudgments, boolean presentJudgments) {
    //        super(nar, true, telepathic ? telepathicEvents : inputOutputEvents );
    //        this.nar = nar;
    //        this.confidenceThreshold = minConfidence;
    //        this.prolog = new NARTuprolog(nar);
    //        this.forgetCyclePeriod = nar.memory.duration() / 2;
    //        this.maxSolveTime = 40.0f / 1e3f;
    //        this.minSolveTime = maxSolveTime/2f;
    //
    //        try {
    //            alice.tuprolog.Term [] ax = toArray(new Theory(getAxiomString()).iterator(prolog.prolog), alice.tuprolog.Term .class);
    //            axioms = new Theory(ax);
    //        } catch (InvalidTheoryException e) {
    //            e.printStackTrace();
    //            System.exit(1);
    //        }
    //
    //
    //        setTemporalMode(eternalJudgments, presentJudgments);
    //    }
    //
    //    public NARPrologMirror setInputMode(InputMode i) {
    //        this.inputMode = i;
    //        return this;
    //    }
    //    public NARPrologMirror setTemporalMode(boolean eternalJudgments, boolean presentJudgments) {
    //        this.eternalJudgments = eternalJudgments;
    //        this.presentJudgments = presentJudgments;
    //        return this;
    //    }
    //
    //    boolean validTemporal(Sentence s) {
    //        long e = s.getOccurrenceTime();
    //
    //        if (eternalJudgments && (e == Stamp.ETERNAL))
    //            return true;
    //
    //        if (presentJudgments) {
    //            long now = nar.time();
    //            if (TemporalRules.concurrent(now, e, (int)(durationCycles * durationMultiplier)))
    //               return true;
    //        }
    //
    //        return false;
    //    }
    //
    //    public Map<Sentence, alice.tuprolog.Term > getBeliefs() {
    //        return beliefs;
    //    }
    //
    //    protected void beliefsChanged() {
    //    }
    //
    //    protected boolean forget(Sentence belief) {
    //        if (beliefs.remove(belief)!=null) {
    //
    //            beliefsChanged();
    //
    //            if (reportForgets) {
    //                System.err.println("Prolog forget: " + belief);
    //            }
    //            return true;
    //        }
    //        return false;
    //    }
    //
    //    protected void updateBeliefs() {
    //        if (presentJudgments) {
    //            long now = nar.time();
    //            durationCycles = (nar.param).duration.get();
    //            if (now - lastFlush > (long)(durationCycles/ forgetCyclePeriod) ) {
    //
    //                Set<Sentence> toRemove = new HashSet();
    //                for (Sentence s : beliefs.keySet()) {
    //                    if (!validTemporal(s)) {
    //                        toRemove.add(s);
    //                    }
    //                }
    //                for (Sentence s : toRemove) {
    //                    forget(s);
    //                }
    //
    //                lastFlush = now;
    //            }
    //        }
    //    }
    //
    //    @Override
    //    public void event(final Class channel, final Object... arg) {
    //
    //        if (channel == ConceptBeliefAdd.class) {
    //            Concept c = (Concept)arg[0];
    //            Task task = (Task)arg[1];
    //            add(task.sentence, task);
    //        }
    //        else if (channel == ConceptBeliefRemove.class) {
    //            Concept c = (Concept)arg[0];
    //            remove(c, (Sentence)arg[1]);
    //        }
    //        else if (channel == Events.ConceptQuestionAdd.class) {
    //            Concept c = (Concept)arg[0];
    //            Task task = (Task)arg[1];
    //            add(task.sentence, task);
    //        }
    //        else if ((channel == IN.class) || (channel == OUT.class)) {
    //            Object o = arg[0];
    //            if (o instanceof Task) {
    //                Task task = (Task)o;
    //                Sentence s = task.sentence;
    //
    //                add(s, task);
    //            }
    //        }
    //    }
    //
    //    /** remove belief unless there are other similar beliefs remaining in 'c' */
    //    private void remove(Concept c, Sentence forgotten) {
    //        for (Task x : c.beliefs) {
    //            if (x.equals(forgotten)) continue;
    //            if (believable(x.getTruth()) && similarTruth(x.getTruth(), forgotten.truth)
    //                    && similarTense(x.sentence, forgotten)) {
    //                //there still remains evidence for this belief in the concept
    //                return;
    //            }
    //        }
    //
    //        remove(forgotten, null);
    //    }
    //
    //    protected void remove(Sentence s, Task task) {
    //        //TODO
    //    }
    //
    //    protected void add(Sentence s, Task task) {
    //
    //        variableContext = s.term.hashCode();
    //
    //        if (!(s.term instanceof Compound))
    //            return;
    //
    //        if (!validTemporal(s))
    //            return;
    //
    //        updateBeliefs();
    //
    //        //only interpret input judgments, or any kind of question
    //        if (s.isJudgment()) {
    //
    //            processBelief(s, task, true);
    //        }
    //        else if (s.isQuestion()) {
    //
    //            //System.err.println("question: " + s);
    //            onQuestion(s);
    //
    //            float priority = task.getPriority();
    //            float solveTime = ((maxSolveTime - minSolveTime) * priority) + minSolveTime;
    //
    //            if (beliefs.containsKey(s)) {
    //                //TODO search for opposite belief
    //
    //                //already determined it to be true
    //                answer(task, s.term, null);
    //                return;
    //            }
    //
    //            try {
    //                Struct qh = newQuestion(s);
    //
    //                if (qh!=null) {
    //                    //System.out.println("Prolog question: " + s.toString() + " | " + qh.toString() + " ? (" + Texts.n2(priority) + ")");
    //
    //                    Theory t = getTheory(beliefs);
    //                    t.append(axioms);
    //
    //                    prolog.setTheory(t);
    //
    //                    SolveInfo si = prolog.query(qh, solveTime);
    //
    //                    int answers = 0;
    //
    //                    alice.tuprolog.Term  lastSolution = null;
    //
    //                    do {
    //                        if (si == null) break;
    //
    //                        alice.tuprolog.Term  solution = si.getSolution();
    //                        if (solution == null)
    //                            break;
    //
    //                        if (lastSolution!=null && solution.equals(lastSolution))
    //                            continue;
    //
    //                        lastSolution = solution;
    //
    //                        try {
    //                            Term n = nterm(solution);
    //                            if (n!=null)
    //                                answer(task, n, solution);
    //                            else
    //                                onUnanswerable(solution);
    //                        }
    //                        catch (Exception e) {
    //                            //problem generating a result
    //                            e.printStackTrace();
    //                        }
    //
    //                        si = prolog.prolog.solveNext(solveTime);
    //
    //                        solveTime /= 2d;
    //                    }
    //                    while ((answers++) < maxAnswers);
    //
    //                    prolog.prolog.solveEnd();
    //
    //                }
    //                else {
    //                    onUnrecognizable(s);
    //                }
    //            } catch (NoSolutionException nse) {
    //                //no solution, ok
    //            } catch (InvalidTermException nse) {
    //                nar.emit(NARPrologMirror.class, s + " : not supported yet");
    //                nse.printStackTrace();
    //            } catch (Exception ex) {
    //                nar.emit(ERR.class, ex.toString());
    //                ex.printStackTrace();
    //            }
    //
    //        }
    //
    //    }
    //
    //    protected void onUnrecognizable(Sentence s) {
    //        //System.err.println(this + " unable to express question in Prolog: " + s);
    //    }
    //
    //    protected void onUnanswerable(alice.tuprolog.Term  solution) {
    //        //System.err.println(this + " unable to answer solution: " + solution);
    //
    //    }
    //
    //    protected void processBelief(Sentence s, Task task, boolean addOrRemove) {
    //
    //        Truth tv = s.truth;
    //        if (believable(tv)) {
    //
    //            boolean exists = beliefs.containsKey(s.term);
    //            if ((addOrRemove) && (exists))
    //                return;
    //            else if ((!addOrRemove) && (!exists))
    //                return;
    //
    //            try {
    //                Struct th = newJudgmentTheory(s);
    //                if (th!=null) {
    //
    //                    if (tv.getFrequency() < falseThreshold) {
    //                        th = negation(th);
    //                    }
    //
    //                    if (addOrRemove) {
    //                        if (beliefs.putIfAbsent(s, th)==null) {
    //
    //                            beliefsChanged();
    //
    //                            if (reportAssumptions)
    //                                System.err.println("Prolog assume: " + th + " | " + s);
    //                        }
    //                    }
    //                    else {
    //                        forget(s);
    //                    }
    //
    //                }
    //            } catch (Exception ex) {
    //                nar.emit(ERR.class, ex.toString());
    //            }
    //        }
    //
    //
    //    }
    //
    //    /** creates a theory from a judgment Statement */
    //    public static Struct newJudgmentTheory(final Sentence judgment) throws InvalidTheoryException {
    //
    //        alice.tuprolog.Term  s;
    //        /*if (judgment.truth!=null) {
    //            s = pInfer(alice.tuprolog.Term (judgment.content), judgment.truth);
    //        }
    //        else {*/
    //        try {
    //            s = alice.tuprolog.Term (judgment.term);
    //        }
    //        catch (Exception e) {
    //            e.printStackTrace();
    //            return null;
    //        }
    //        //}
    //
    //        return (Struct) s;
    //    }
    //
    //    Struct newQuestion(final Sentence question) {
    //        alice.tuprolog.Term  s = alice.tuprolog.Term (question.term);
    //        return (Struct) s;
    //    }
    //
    //    //NOT yet working
    //    public Struct pInfer(alice.tuprolog.Term  t, Truth tv) {
    //        double freq = tv.getFrequency();
    //        double conf = tv.getConfidence();
    //        Struct lt = new Struct(new alice.tuprolog.Term [] { t,
    //            new Struct( new alice.tuprolog.Term [] {
    //                new nars.tuprolog.Double(freq),
    //                new nars.tuprolog.Double(conf)
    //            })
    //        });
    //        return new Struct("infer", lt);
    //    }
    //
    //    public static Struct negation(alice.tuprolog.Term  t) {
    //        return new Struct("negation", t);
    //    }
    //
    //    public static String pquote(final String x) {
    //        return "'" + x + '\'';
    //    }
    //
    //    public static String pescape(final String p) {
    //        if (!Parser.isAtom(p)) {
    //            return pquote(p);
    //        }
    //        if (Character.isDigit(p.charAt(0))) {
    //            return pquote(p);
    //        }
    //        return p;
    //    }
    //    public static String unpescape(String p) {
    //        return p.toString();
    //    }
    //
    //    public boolean believable(Truth tv) {
    //        return (tv.getConfidence() > confidenceThreshold) && ((tv.getFrequency() > trueThreshold) || (tv.getFrequency() < falseThreshold));
    //    }
    //
    //    public boolean similarTense(Sentence a, Sentence b) {
    //        boolean ae = a.isEternal();
    //        boolean be = b.isEternal();
    //        if (ae && be) return true;
    //        else if (ae && !be) return false;
    //        else if (!ae && be) return false;
    //        else {
    //            return (TemporalRules.concurrent(a.getOccurrenceTime(), b.getOccurrenceTime(), nar.memory.duration()));
    //        }
    //    }
    //
    //
    //    public boolean similarTruth(Truth a, Truth b) {
    //        float af = a.getFrequency();
    //        float bf = b.getFrequency();
    //        if ((af < falseThreshold) && (bf < falseThreshold))
    //            return true;
    //        if ((af > trueThreshold) && (bf > trueThreshold))
    //            return true;
    //        return false;
    //    }
    //
    //    protected static String classPredicate(final Class c) {
    //        String s = c.getSimpleName();
    //        switch (s) {
    //            case "SetInt1": s = "setint"; break;
    //            case "SetExt1": s = "setext"; break;
    //        }
    //        return s.toLowerCase();
    //    }
    //
    //
    //    public Sentence getBeliefSentence(Sentence question, Term belief, Task parentTask) {
    //        float freq = 1.0f;
    //        float conf = Global.DEFAULT_JUDGMENT_CONFIDENCE;
    //        float priority = Global.DEFAULT_JUDGMENT_PRIORITY;
    //        int durability = Global.DEFAULT_JUDGMENT_DURABILITY;
    //        Tense tense = question.isEternal() ? Tense.Eternal : Tense.Present;
    //
    //        //TODO use derivation of prolog result to create a correct stamp
    //
    //        return new Sentence(belief, '.', new Truth(freq, conf),
    //                new Stamp(nar.memory, tense));
    //    }
    //
    //    /** reflect a result to NARS, and remember it so that it doesn't get reprocessed here later */
    //    public Term answer(Task question, Term t, alice.tuprolog.Term  pt) {
    //        if (reportAnswers)
    //            System.err.println("Prolog answer: " + t);
    //
    //        Sentence a = getBeliefSentence(question.sentence, t, question);
    //
    //        input(a, inputMode, question);
    //
    //        if (pt!=null) {
    //            beliefs.put(a, pt);
    //            beliefsChanged();
    //        }
    //
    //        return t;
    //    }
    //
    //    /*
    //    public static class NARStruct extends Struct {
    //
    //        Sentence sentence = null;
    //
    //        public NARStruct(Sentence sentence, String predicate, nars.prolog.Term[] args) {
    //            super(predicate, args);
    //
    //            this.sentence = sentence;
    //        }
    //
    //        public NARStruct(String predicate, nars.prolog.Term... args) {
    //            this(null, predicate, args);
    //        }
    //
    //        public Sentence getSentence() {
    //            return sentence;
    //        }
    //
    //        public void setSentence(Sentence sentence) {
    //            this.sentence = sentence;
    //        }
    //
    //
    //    }
    //    */
    //
    //
    //
    //
    //
    //    private String getAxiomString() {
    //        return
    //                    "inheritance(A, C) :- inheritance(A,B),inheritance(B,C). " + '\n' +
    //                    "similarity(A, B) :- inheritance(A,B),inheritance(B,A). " + '\n' +
    //
    //                    "implication(A, C) :- implication(A,B),implication(B,C). " + '\n' +
    //
    //                    "similarity(A, B) :- similarity(B,A). " + '\n' +
    //                    "not(similar(A, B)) :- not(inheritance(A,B)),inheritance(B,A). " + '\n' +
    //
    //                    "equivalence(A, B) :- equivalence(B,A). " + '\n' +
    //                    "similarity(A, B) :- equivalence(A,B). " + '\n' +
    //
    //                    "not(equivalence(A, B)) :- not(similar(A,B)). " + '\n' +
    //
    //
    //                    "A :- not(not(A))." + '\n';
    //    }
    //
    //    public static Theory getTheory(Map<Sentence, alice.tuprolog.Term > beliefMap) throws InvalidTheoryException  {
    //        return new Theory(new Struct(beliefMap.values().toArray(new Struct[beliefMap.size()])));
    //    }
    //
    //    public Theory getBeliefsTheory() throws InvalidTheoryException {
    //        return getTheory(beliefs);
    //    }
    //
    //    protected void onQuestion(Sentence s) {
    //    }
    //
    //    public static alice.tuprolog.Term [] alice.tuprolog.Term s(Term[] term) {
    //        alice.tuprolog.Term [] tt = new alice.tuprolog.Term [term.length];
    //        int i = 0;
    //        for (Term x : term) {
    //            if ((tt[i++] = alice.tuprolog.Term (x)) == null) return null;
    //        }
    //        return tt;
    //    }
    //
    //    public static Term[] nterm(final alice.tuprolog.Term [] term) {
    //        Term[] tt = new Term[term.length];
    //        int i = 0;
    //        for (alice.tuprolog.Term  x : term) {
    //            if ((tt[i++] = nterm(x)) == null) return null;
    //        }
    //        return tt;
    //    }
    //


}
