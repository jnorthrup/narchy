package nars.op.prolog;

import alice.tuprolog.*;
import com.google.common.collect.Iterators;
import jcog.Util;
import jcog.data.atomic.AtomicFloat;
import jcog.util.Range;
import nars.*;
import nars.attention.What;
import nars.control.channel.CauseChannel;
import nars.memory.Memory;
import nars.subterm.Subterms;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.NormalizedVariable;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * Prolog mental coprocessor for accelerating reasoning
 * WARNING - introduces cognitive distortion
 * <p>
 * Causes a NARProlog to mirror certain activity of a NAR.  It generates
 * prolog terms from NARS beliefs, and answers NARS questions with the results
 * of a prolog solution (converted to NARS terms), which are input to NARS memory
 * with the hope that this is sooner than NARS can solve it by itself.
 */
public class PrologCore extends Prolog implements Consumer<Task> {

    static final Logger logger = LoggerFactory.getLogger(PrologCore.class);

    //    public static final String AxiomTheory;
    public static final alice.tuprolog.Term ONE = new NumberTerm.Int(1);
    public static final alice.tuprolog.Term ZERO = new NumberTerm.Int(0);
//
//    static {
//        String a;
//        try {
//            a = Util.inputToString(
//                    PrologCore.class.getClassLoader()
//                            .getResourceAsStream("nars/prolog/default.prolog")
//            );
//        } catch (Throwable e) {
//            logger.error("default.prolog {}", e.getMessage());
//            a = "";
//        }
//        AxiomTheory = a;
//    }

    private final NAR nar;

    final Map<Term, alice.tuprolog.Term> beliefs = new ConcurrentHashMap();

    /**
     * beliefs above this expectation will be asserted as prolog beliefs
     */
    @Range(min = 0.5, max = 1.0)
    public final Number trueFreqThreshold = new AtomicFloat(0.9f);

    /**
     * beliefs below this expectation will be asserted as negated prolog beliefs
     */
    @Range(min = 0, max = 0.5)
    public final Number falseFreqThreshold = new AtomicFloat(0.1f);

    /**
     * beliefs above this expectation will be asserted as prolog beliefs
     */
    @Range(min = 0, max = 1.0)
    public final Number confThreshold = new AtomicFloat(0.75f);


    @Range(min = 0, max = 1.0)
    public final Number answerConf = new AtomicFloat(confThreshold.floatValue() * 0.9f);


    private final CauseChannel<Task> in;
    private final What what;

    /** TODO adapt NAR memory as ClauseIndex */
    public static class MyClauseIndex extends ConcurrentHashClauseIndex {


        public MyClauseIndex(Memory t) {

        }


    }

    public PrologCore(NAR n) {
        super(new MyClauseIndex(n.memory), new ConcurrentHashClauseIndex());

        if (NAL.DEBUG)
            setSpy(true);

        this.in = n.newChannel(this);
        this.nar = n;
        this.what = nar.what(); //TODO be parameter


//        /*@Deprecated*/ n.eventTask.on(this);
    }

    @Override
    @Deprecated public void accept(Task task) {

        if (task.isBelief()) {

            if (task.isEternal()) {
                int dt = task.term().dt();
                if (dt == 0 || dt == DTERNAL) {
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


    }

    protected void believe(Task t, boolean truth) {


        Term ct = t.term();

        if (!ct.hasAny(Op.AtomicConstant))
            return;

        beliefs.computeIfAbsent(ct, (pp/*=ct?*/) -> {

            Struct next = (Struct) pterm(t.term());

            if (!truth) {
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


    protected void question(Task question) {
        Term tt = question.term();
        /*if (t.op() == Op.NEGATE) {
            
            tt = ((Compound)tt).target(0);
            truth = !truth;
        }*/

        alice.tuprolog.Term questionTerm = pterm(tt);


        logger.info("solve {}", questionTerm);

        long timeoutMS = 50L;
        solveWhile(questionTerm, (answer) -> {


            switch (answer.result()) {
                case PrologRun.TRUE:
                case PrologRun.TRUE_CP:


                    answer(question, answer);

                    break;
                case PrologRun.FALSE:


                    break;
                default:

                    break;
            }
            return true;
        }, timeoutMS);

    }

    private void answer(Task question, Solution answer) {
        try {
            Term yt = nterm(answer.goal);

            Task y = Task.tryTask(yt, BELIEF, $.t(1f, answerConf.floatValue()), (term, truth) -> {
                Task t = NALTask.the(term, BELIEF, truth, nar.time(), ETERNAL, ETERNAL, nar.evidence())
                        .pri(nar);
                return t;
            });


            if (y != null) {
                logger.info("answer {}\t{}", question, y);
                in.accept(y, what);
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

    private static @Nullable Term[] nterms(Struct s) {
        int len = s.subs();
        Term[] n = new Term[len];
        for (int ni = 0; ni < len; ni++) {
            if ((n[ni] = nterm(s.subResolve(ni))) == null)
                return null;
        }
        return n;
    }


    private static Term nterm(alice.tuprolog.Term t) {
        Term result;
        if (t instanceof Var) {
            result = $.varDep(((Var) t).name());
        } else {
            Struct s = (Struct) t;
            if (s.subs() > 0) {
                switch (s.name()) {

                    case "-->":
                        result = theTwoArity(Op.INH, s);
                        break;
                    case "<->":
                        result = theTwoArity(Op.SIM, s);
                        break;


                    case "==>":
                        result = theTwoArity(Op.IMPL, s);
                        break;


                    case "[":
                        result = SETi.the((nterms(s)));
                        break;
                    case "{":
                        result = SETe.the((nterms(s)));
                        break;

                    case "*":
                        result = PROD.the((nterms(s)));
                        break;
                    case "&&":
                        result = CONJ.the((nterms(s)));
                        break;
                    case "||":
                        result = DISJ(nterms(s));
                        break;
                    case "not":
                        result = (nterm(s, 0).neg());
                        break;


                    default:
                        result = $.func(unwrapAtom(s.name()), nterms(((Struct) t).subArrayShared()));
                        break;
                }
            } else {
                String n = s.name();
                if (n.startsWith("'#")) {

                    result = $.varDep(n.substring(2, n.length() - 1));
                } else {
                    result = $.the(unwrapAtom(n));
                }

            }


        }
//        else {
//            throw new RuntimeException(t + " untranslated");
//        }
        return result;
    }


    private static String unwrapAtom(String n) {
        if ((int) n.charAt(0) == (int) '_')
            n = n.substring(1);
        return n;
    }

    private static String wrapAtom(String n) {
        return '_' + n;
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
        return new Struct("--", p);
    }

    public static alice.tuprolog.Term[] psubterms(Subterms s) {
        return s.array(PrologCore::pterm, alice.tuprolog.Term[]::new);
    }

//    public static alice.tuprolog.Term tterm(String punc, final alice.tuprolog.Term nalTerm, boolean isTrue) {
//        return new Struct(punc, nalTerm, isTrue ? ONE : ZERO);
//    }


    public static alice.tuprolog.Term pterm(Term term) {
        if (term instanceof Compound) {
            Op op = term.op();
            alice.tuprolog.Term[] st = psubterms(term.subterms());
            switch (op) {
                case IMPL:
                    return new Struct(":-", st[1], st[0] /* reversed */);
                case CONJ: {
                    return new Struct(",", st); //TODO may need special depvar handling
//                    int s = target.subs();
//                    alice.tuprolog.Term t = pterm(target.sub(s - 1));
//                    for (int i = s-2; i >= 0; i--) {
//                        t = new Struct(",", t, pterm(target.sub(i)));
//                    }
//                    return t;
                } case NEG:
                    //TODO detect disj
                    return new Struct(/*"\\="*/"not", st);
                case PROD:
                    return new Struct(st);
                case INH:
                    Term pred = term.sub(1);
                    if (pred.op() == ATOM) {
                        Term subj = term.sub(0);
                        if (subj.op() == PROD) {
                            alice.tuprolog.Term args = st[0];
                            return new Struct(wrapAtom(pred.toString()),
                                    args != null ?
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
                case VAR_DEP:
                case VAR_INDEP:
                    return new Var("_" + (((NormalizedVariable) term).id()));


            }
        } else if (term instanceof Atomic) {
            return new Struct(wrapAtom(term.toString()));
        }

        throw new UnsupportedOperationException();


    }


}
