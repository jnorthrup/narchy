package nars.derive.action;

import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import nars.$;
import nars.Builtin;
import nars.Narsese;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.action.op.Occurrify;
import nars.derive.action.op.Taskify;
import nars.derive.action.op.Termify;
import nars.derive.action.op.Truthify;
import nars.derive.cond.SingleOrDoublePremise;
import nars.derive.premise.PremiseRuleNormalization;
import nars.derive.rule.CondHow;
import nars.derive.rule.HowBuilder;
import nars.derive.rule.PremiseRule;
import nars.derive.rule.RuleCause;
import nars.derive.util.DerivationFunctors;
import nars.derive.util.PuncMap;
import nars.derive.util.Unifiable;
import nars.op.UniSubst;
import nars.subterm.BiSubterm;
import nars.subterm.Subterms;
import nars.term.Variable;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.IdempotentBool;
import nars.term.util.TermException;
import nars.term.util.conj.ConjMatch;
import nars.term.util.transform.RecursiveTermTransform;
import nars.truth.func.NALTruth;
import nars.truth.func.TruthFunction;
import nars.truth.func.TruthModel;
import nars.unify.constraint.UnifyConstraint;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.derive.premise.PatternTermBuilder.patternify;
import static nars.term.atom.IdempotentBool.Null;
import static nars.time.Tense.DTERNAL;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PatternHow extends CondHow {

    private final TruthModel truthModel;

    protected Occurrify.OccurrenceSolver time;
    protected Term beliefTruth;
    protected Term goalTruth;
    Truthify truthify;
    public Termify termify;


    private Term concTerm;
    /** TODO just use one punc->punc transfer function, and its return of 0 to deny */
    protected BytePredicate taskPunc = null;
    @Deprecated protected ByteToByteFunction concPunc = null;

    @Deprecated
    private transient boolean concBelief = false;
    @Deprecated
    private transient boolean concQuestion = false;
    @Deprecated
    private transient boolean concGoal = false;
    @Deprecated
    private transient boolean concQuest = false;



    public PatternHow(TruthModel truthModel) {
        this.truthModel = truthModel;
    }


//    public static HowBuilder parseSafe(String ruleSrc)  {
//        try {
//            return parse(ruleSrc);
//        } catch (Narsese.NarseseException e) {
//            throw new RuntimeException("rule parse failure:\n" + ruleSrc, e);
//        }
//    }
//
//    public static PatternHow parse(String ruleSrc) throws Narsese.NarseseException {
//        return parse(ruleSrc, NALTruth.the);
//    }

    @Deprecated public static PatternHow parse(String ruleSrc) throws Narsese.NarseseException {
        return parse(ruleSrc, NALTruth.the, null);
    }

    public static PatternHow parse(String ruleSrc, TruthModel truthModel, @Nullable String tag) throws Narsese.NarseseException {
        PatternHow r = new PatternHow(truthModel);
        r._parse(ruleSrc);
        r.tag(tag);
        return r;
    }

    protected void _parse(String ruleSrc) throws Narsese.NarseseException {
        this.source = ruleSrc;
        this.id = new MyPremiseRuleNormalization().apply(
            new UppercaseAtomsToPatternVariables().apply(
                $.INSTANCE.pFast(parseRuleComponents(ruleSrc))
            )
        );

        Term[] precon = ((Compound)id.sub(0)).arrayShared();

        taskPattern(precon[0]);
        beliefPattern(precon[1]);


        Term[] postcon = ((Compound)id.sub(1)).arrayShared();
        concTerm = postcon[0];


        for (int i = 2; i < precon.length; i++) {
            cond(precon[i]);
        }

        time = null;

        Term[] modifiers = postcon.length > 1 ? ((Compound)postcon[1]).arrayShared() : Op.EmptyTermArray;

        for (Term m : modifiers) {
            if (m.op() != Op.INH)
                throw new RuntimeException("Unknown postcondition format: " + m);

            Term type = m.sub(1);
            Term which = m.sub(0);

            switch (type.toString()) {

                case "Punctuation":
                    switch (which.toString()) {
                        case "Belief":
                            concBelief = true;
                            break;
                        case "Question":
                            concQuestion = true;
                            break;
                        case "Goal":
                            concGoal = true;
                            break;
                        case "Quest":
                            concQuest = true;
                            break;

                        /** belief -> question, goal -> quest */
                        case "Answer":
                            assert (taskPunc == null && concPunc == null);
                            assert (beliefTruth != null && goalTruth != null);
                            taskPunc = new BytePredicate() {
                                @Override
                                public boolean accept(byte p) {
                                    return (int) p == (int) QUESTION || (int) p == (int) QUEST;
                                }
                            };
                            concPunc = new ByteToByteFunction() {
                                @Override
                                public byte valueOf(byte p) {
                                    switch (p) {
                                        case QUESTION:
                                            return BELIEF;
                                        case QUEST:
                                            return GOAL;
                                        default:
                                            return (byte) 0;
                                    }
                                }
                            };
                            break;
                        /** belief -> question, goal -> quest */
                        case "Ask":
                            assert (taskPunc == null && concPunc == null);
                            taskPunc = new BytePredicate() {
                                @Override
                                public boolean accept(byte p) {
                                    return (int) p == (int) BELIEF || (int) p == (int) GOAL;
                                }
                            };
                            concPunc = new ByteToByteFunction() {
                                @Override
                                public byte valueOf(byte p) {
                                    switch (p) {
                                        case BELIEF:
                                            return QUESTION;
                                        case GOAL:
                                            return QUEST;
                                        default:
                                            return (byte) 0;
                                    }
                                }
                            };
                            break;

                        /** re-ask a new question/quest in response to question/quest */
                        case "AskAsk":
                            assert (taskPunc == null && concPunc == null);
                            taskPunc = new BytePredicate() {
                                @Override
                                public boolean accept(byte p) {
                                    return (int) p == (int) QUESTION || (int) p == (int) QUEST;
                                }
                            };
                            concPunc = new ByteToByteFunction() {
                                @Override
                                public byte valueOf(byte p) {
                                    switch (p) {
                                        case QUESTION:
                                            return QUESTION;
                                        case QUEST:
                                            return QUEST;
                                        default:
                                            return (byte) 0;
                                    }
                                }
                            };
                            break;

                        /** belief,question -> question, goal,quest -> quest */
                        case "AskAll":
                            assert (taskPunc == null && concPunc == null);
                            taskPunc = new BytePredicate() {
                                @Override
                                public boolean accept(byte p) {
                                    return true;
                                }
                            };
                            concPunc = new ByteToByteFunction() {
                                @Override
                                public byte valueOf(byte p) {
                                    switch (p) {
                                        case BELIEF:
                                        case QUESTION:
                                            return QUESTION;
                                        case GOAL:
                                        case QUEST:
                                            return QUEST;
                                        default:
                                            throw new UnsupportedOperationException();
                                    }
                                }
                            };
                            break;
                        default:
                            throw new RuntimeException("unknown punctuation: " + which);
                    }
                    break;

                case "Time":
                    time = Occurrify.solvers.get(which);
                    if (time == null)
                        throw new RuntimeException("unknown Time modifier:" + which);
                    break;


                case "Belief":
                    beliefTruth = which;
                    break;

                case "Goal":
                    goalTruth = which;
                    break;


                default:
                    throw new RuntimeException("Unhandled postcondition: " + type + ':' + which);
            }

        }


    }

    @Override protected void cond(Term o, boolean negated, boolean[] _negationApplied, String pred, Term x, Term y, nars.term.Variable Xv, Variable Yv, Variable Xvs, Variable Yvs) {
        super.cond(o, negated, _negationApplied, pred, x, y, Xv, Yv, Xvs, Yvs);
        switch (pred) {
            case "task":
                String XString = x.toString();
                switch (XString) {


                    case "\"?\"":
                        taskPunc = new BytePredicate() {
                            @Override
                            public boolean accept(byte t) {
                                return (int) t == (int) QUESTION;
                            }
                        };
                        break;
                    case "\"@\"":
                        taskPunc = new BytePredicate() {
                            @Override
                            public boolean accept(byte t) {
                                return (int) t == (int) QUEST;
                            }
                        };
                        break;
                    case "\".\"":
                        taskPunc = new BytePredicate() {
                            @Override
                            public boolean accept(byte t) {
                                return (int) t == (int) BELIEF;
                            }
                        };
                        break;
                    case "\"!\"":
                        taskPunc = new BytePredicate() {
                            @Override
                            public boolean accept(byte t) {
                                return (int) t == (int) GOAL;
                            }
                        };
                        break;

                    case "\".?\"":
                        assert (taskPunc == null);
                        taskPunc = new BytePredicate() {
                            @Override
                            public boolean accept(byte t) {
                                return (int) t == (int) BELIEF || (int) t == (int) QUESTION;
                            }
                        };
                        break;

                    case "\"?@\"":
                        assert (taskPunc == null && concPunc == null);
                        taskPunc = new BytePredicate() {
                            @Override
                            public boolean accept(byte t) {
                                return (int) t == (int) QUESTION || (int) t == (int) QUEST;
                            }
                        };
                        concPunc = new ByteToByteFunction() {
                            @Override
                            public byte valueOf(byte c) {
                                return c;
                            }
                        };
                        break;

                    case "\".!\"":
                        assert (taskPunc == null && concPunc == null);
                        taskPunc = new BytePredicate() {
                            @Override
                            public boolean accept(byte t) {
                                return (int) t == (int) BELIEF || (int) t == (int) GOAL;
                            }
                        };
                        concPunc = new ByteToByteFunction() {
                            @Override
                            public byte valueOf(byte c) {
                                return c;
                            }
                        };
                        break;
                    case "all":
                        taskPunc = new BytePredicate() {
                            @Override
                            public boolean accept(byte t) {
                                return true;
                            }
                        };
                        break;
//                        case "\"*\"":
//                            pre.addAt(new TaskBeliefOp(PROD, true, false));
//                            break;
//                        case "\"&&\"":
//                            pre.addAt(new TaskBeliefOp(CONJ, true, false));
//                            break;


                    default:
                        throw new RuntimeException("Unknown task punctuation type: " + XString);
                }
                break;

        }
    }


    private static Truthify intern(Truthify x) {
        Truthify y = truthifies.putIfAbsent(x.term(), x);
        return y != null ? y : x;
    }

    @Deprecated public static Stream<PremiseRule> parse(String... rules) {
        return parse(Stream.of(rules).map(String::trim).filter(new Predicate<String>() {
            @Override
            public boolean test(String x) {
                return !x.isEmpty();
            }
        }), NALTruth.the);
    }

    public static Stream<PremiseRule> parse(Stream<String> rules, TruthModel truthModel) {
        String[] currentTag = {null};
        Stream<PremiseRule> s = rules.flatMap(new Function<String, Stream<? extends HowBuilder>>() {
            @Override
            public Stream<? extends HowBuilder> apply(String line) {
                if (!line.contains("|-")) {
                    if (line.endsWith("{")) {
                        //start tag
                        assert (currentTag[0] == null);
                        currentTag[0] = line.substring(0, line.length() - 1).trim();
                        assert (currentTag[0].length() > 0);
                        return Stream.of();
                    } else if (line.endsWith("}")) {
                        //close tag
                        assert (currentTag[0] != null);
                        currentTag[0] = null;
                        return Stream.of();
                    }
                }

                try {
                    HowBuilder h = PatternHow.parse(line, truthModel, currentTag[0]);
                    return Stream.of(h);
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                    return Stream.of();
                }
            }
        }).map(HowBuilder::get).distinct();

        assert(currentTag[0] == null); //closed

        return s;
    }

    private static Subterms parseRuleComponents(String src) throws Narsese.NarseseException {


        String[] ab = ruleImpl.split(src);
        if (ab.length != 2)
            throw new Narsese.NarseseException("Rule component must have arity=2, separated by \"|-\": " + src);

        String A = '(' + ab[0].trim() + ')';
        Term a = Narsese.term(A, false);
        if (!(a instanceof Compound))
            throw new Narsese.NarseseException("Left rule component must be compound: " + src);

        String B = '(' + ab[1].trim() + ')';
        Term b = Narsese.term(B, false);
        if (!(b instanceof Compound))
            throw new Narsese.NarseseException("Right rule component must be compound: " + src);

        return new BiSubterm(a, b);
    }


    private Term conclusion(Term c) {
        //verify that all pattern variables in c are present in either taskTerm or beliefTerm
        assertConclusionVariablesPresent(c);

        if (!c.unneg().op().var) {

            List<Term> subbedConj;
            ArrayHashSet<Term> savedConj;

            if (c.hasAll(INH.bit | CONJ.bit)) {
                subbedConj = new FasterList(0);
                c = saveEteConj(c, subbedConj, savedConj = new ArrayHashSet(0));
            } else {
                savedConj = null;
                subbedConj = null;
            }

            c = conclusionOptimize(ConcTransform.apply(patternify(c)));

            if (savedConj != null && !savedConj.isEmpty())
                c = restoreEteConj(c, subbedConj, savedConj);

        }

        return c;
    }

    private static Term restoreEteConj(Term c, List<Term> subbedConj, ArrayHashSet<Term> savedConj) {
        for (int i = 0, subbedConjSize = subbedConj.size(); i < subbedConjSize; i++) {
            Term y = subbedConj.get(i);
            Term x = savedConj.get(i);
            c = c.replace(y, x);
        }
        return c;
    }

    /**
     * HACK preserve any && occurring in --> by substituting them then replacing them
     */
    @Deprecated
    private static Term saveEteConj(Term c, List<Term> subbedConj, ArrayHashSet<Term> savedConj) {

        c.recurseTerms(new Predicate<Term>() {
            @Override
            public boolean test(Term x) {
                return x.hasAll(INH.bit | CONJ.bit);
            }
        }, new Predicate<Term>() {
            @Override
            public boolean test(Term t) {
                if (t.op() == INH) {
                    Term s = t.sub(0);
                    Term su = s.unneg();
                    if (su.op() == CONJ && su.dt() == DTERNAL)
                        savedConj.add(patternify(s, false));
                    Term p = t.sub(1);
                    Term pu = p.unneg();
                    if (pu.op() == CONJ && pu.dt() == DTERNAL)
                        savedConj.add(patternify(p, false));
                }
                return true;
            }
        }, null);
        if (!savedConj.isEmpty()) {
            int i = 0;
            for (Term x : savedConj) {
                Term y = $.INSTANCE.p(eteConj, $.INSTANCE.the(i));
                subbedConj.add(y);
                c = c.replace(x, y);
                i++;
            }
        }
        return c;
    }

    private void assertConclusionVariablesPresent(Term c) {
        boolean tb = taskPattern.equals(beliefPattern);
        c.recurseTerms(Termlike::hasVarPattern, new Predicate<Term>() {
            @Override
            public boolean test(Term z) {
                if (z.op() == VAR_PATTERN) {
                    if (!(taskPattern.equals(z) || taskPattern.containsRecursively(z)) &&
                            (tb || !(beliefPattern.equals(z) || beliefPattern.containsRecursively(z)))) {
                        throw new RuntimeException("conclusion has pattern variable not contained in task or belief pattern: " + z);
                    }
                }
                return true;
            }
        }, null);
    }

    private Term conclusionOptimize(Term y) {

//            boolean taskEqY = taskPattern.equals(y);
//            boolean beliefEqualY = beliefPattern.equals(y);

        boolean taskObviouslyNotPastable = taskPattern.op().var;
        boolean beliefObviouslyNotPastable = beliefPattern.op().var;
        boolean taskTemporal = taskPattern.hasAny(Temporal);
        boolean beliefTemporal = beliefPattern.hasAny(Temporal);

        boolean atMostOneTemporal = !(taskTemporal && beliefTemporal);

        //decide when inline "paste" (macro substitution) of the premise task or belief terms is allowed.
        boolean taskPastable = false;
        if (!taskObviouslyNotPastable && ((atMostOneTemporal || !y.hasAny(Op.Temporal))))
            taskPastable = true;
        boolean beliefPastable = false;
        if (!beliefObviouslyNotPastable && ((atMostOneTemporal || !y.hasAny(Op.Temporal))))
            beliefPastable = true;

        Term yT, yB;
        Term y0 = y;
        if (beliefPattern.volume() <= taskPattern.volume()) {
            //subst task first
            yT = taskPastable ? y0.replace(taskPattern, DerivationFunctors.TaskTerm) : y0;
            yB = beliefPastable ? yT.replace(beliefPattern, DerivationFunctors.BeliefTerm) : yT;
            y = yB;
        } else {
            //subst belief first
            yB = beliefPastable ? y0.replace(beliefPattern, DerivationFunctors.BeliefTerm) : y0;
            yT = taskPastable ? yB.replace(taskPattern, DerivationFunctors.TaskTerm) : yB;
            y = yT;
        }

//            boolean taskPasted = y0 != y1;
//            if (!taskObviouslyNotPastable && !taskPasted && !y.replace(taskPattern, Derivation.TaskTerm).equals(y))
//                System.out.println("task paste possible: " + y + " -> " + y0);
//
//            boolean beliefPasted = y1 != y2;
//            if (!beliefObviouslyNotPastable && !beliefPasted && !y.replace(beliefPattern, Derivation.BeliefTerm).equals(y))
//                System.out.println("belf paste possible: " + y + " -> " + y0);


//        if (y0 != y) {
//                System.out.println(y + " -> " + y0);
//        }
        return y;
    }


    //    private void matchSuper(boolean taskOrBelief, TermMatcher m, boolean trueOrFalse) {
//        byte[] path = ArrayUtil.EMPTY_BYTE_ARRAY;
//        pre.add(new TermMatch(m, trueOrFalse, false, TaskOrBelief(taskOrBelief).path(path), cost(path.length)));
//    }


    protected void commit() {

        super.commit();

        TruthFunction beliefTruthOp = truthModel.get(beliefTruth);
        if (beliefTruth != null && beliefTruthOp == null)
            throw new RuntimeException("unknown BeliefFunction: " + beliefTruth);

        TruthFunction goalTruthOp = truthModel.get(goalTruth);
        if (goalTruth != null && goalTruthOp == null)
            throw new RuntimeException("unknown GoalFunction: " + goalTruth);







        /** infer missing conclusion punctuation */
        if (concPunc == null) {
            if (beliefTruth != null) concBelief = true;
            if (goalTruth != null) concGoal = true;

            if (!concQuest && !concQuestion) {
                if (beliefTruth != null && goalTruth == null) {
                    concPunc = new ByteToByteFunction() {
                        @Override
                        public byte valueOf(byte p) {
                            return BELIEF;
                        }
                    };
                } else if (beliefTruth == null && goalTruth != null) {
                    concPunc = new ByteToByteFunction() {
                        @Override
                        public byte valueOf(byte p) {
                            return GOAL;
                        }
                    };
                } else if (beliefTruth != null && goalTruth != null) {
                    concPunc = new ByteToByteFunction() {
                        @Override
                        public byte valueOf(byte p) {
                            return (int) p == (int) BELIEF ? BELIEF : GOAL;
                        }
                    };
                }
            } else {
                if (concQuestion && !concQuest) {
                    concPunc = new ByteToByteFunction() {
                        @Override
                        public byte valueOf(byte p) {
                            return QUESTION;
                        }
                    };
                } else if (concQuest && !concQuestion) {
                    concPunc = new ByteToByteFunction() {
                        @Override
                        public byte valueOf(byte p) {
                            return QUEST;
                        }
                    };
                } else if (concQuestion && concQuest) {
                    concPunc = new ByteToByteFunction() {
                        @Override
                        public byte valueOf(byte p) {
                            return ((int) p == (int) QUESTION || (int) p == (int) BELIEF) ? QUESTION : QUEST;
                        }
                    };
                }
            }

            if (!(concBelief || concQuest || concQuestion || concGoal))
                throw new WTF();
        }



        /** infer necessary double premise for derived belief  */

        boolean doubleBelief = false;
        if (beliefTruthOp != null) {
            assert ((int) concPunc.valueOf(BELIEF) == (int) BELIEF || (int) concPunc.valueOf(GOAL) == (int) BELIEF || (int) concPunc.valueOf(QUESTION) == (int) BELIEF || (int) concPunc.valueOf(QUEST) == (int) BELIEF);
            if (!beliefTruthOp.single()) {
                doubleBelief = true;
            }
        }
        /** infer necessary double premise for derived goal  */
        boolean doubleGoal = false;
        if (goalTruthOp != null) {
            assert ((int) concPunc.valueOf(BELIEF) == (int) GOAL || (int) concPunc.valueOf(GOAL) == (int) GOAL || (int) concPunc.valueOf(QUESTION) == (int) GOAL || (int) concPunc.valueOf(QUEST) == (int) GOAL);
            if (!goalTruthOp.single()) {
                doubleGoal = true;
            }
        }

        /*System.out.println( Long.toBinaryString(
                        pStructure) + " " + concPattern
        );*/


//        constraints.forEach(cc -> {
//            PREDICATE<Derivation> p = cc.preFilter(taskPattern, beliefPattern);
//            if (p != null) {
//                pre.addAt(p);
//            }
//        });



        if (concPunc == null)
            throw new UnsupportedOperationException("no concPunc specified");


        if (time == null) {
            time = !doubleBelief && !doubleGoal ? Occurrify.solverDefaultSingle : Occurrify.solverDefaultDouble;
        }


        /** infer necessary task punctuation */
        if (taskPunc == null) {
            //auto
            if (beliefTruth != null && goalTruth != null) {
                taskPunc = new BytePredicate() {
                    @Override
                    public boolean accept(byte t) {
                        return (int) t == (int) BELIEF || (int) t == (int) GOAL;
                    }
                }; //accept belief and goal and map to those
            } else if (beliefTruth != null) {
                taskPunc = new BytePredicate() {
                    @Override
                    public boolean accept(byte t) {
                        return (int) t == (int) BELIEF;
                    }
                }; //accept only belief -> belief
            } else if (goalTruth != null) {
                taskPunc = new BytePredicate() {
                    @Override
                    public boolean accept(byte t) {
                        return (int) t == (int) GOAL;
                    }
                }; //accept only goal -> goal
            }
            //concPunc = t -> t;
        }

        PuncMap tp = new PuncMap(
            PuncMap.p(taskPunc, concPunc, BELIEF),
            PuncMap.p(taskPunc, concPunc, GOAL),
            PuncMap.p(taskPunc, concPunc, QUESTION),
            PuncMap.p(taskPunc, concPunc, QUEST),
            (byte) 0 //COMMAND
        );
        if (!tp.all())
            pre.add(tp); //add filter to allow only the mapped types

        if (doubleBelief || doubleGoal) {
            if (beliefPattern.op() != VAR_PATTERN && !beliefPattern.op().taskable)
                throw new TermException("double premise may be required and belief pattern is not taskable", beliefPattern);

            boolean forBelief = doubleBelief && (int) tp.get(BELIEF) == (int) BELIEF;
            boolean forGoal = doubleGoal && (int) tp.get(GOAL) == (int) GOAL;
            boolean forQ = (doubleBelief && ((int) tp.get(QUESTION) == (int) BELIEF || (int) tp.get(QUEST) == (int) BELIEF))
                ||
                (doubleGoal && ((int) tp.get(QUESTION) == (int) GOAL || (int) tp.get(QUEST) == (int) GOAL));
            if (forBelief || forGoal || forQ) {
                //pre.add(new S(forBelief, forGoal, forQ));
                pre.add(new SingleOrDoublePremise(new PuncMap(forBelief, forGoal, forQ, forQ), false));
            }
        }

        this.truthify = intern(Truthify.the(tp, beliefTruthOp, goalTruthOp, time));

        if (concTerm instanceof IdempotentBool)
            throw new TermException("conclusion pattern is bool", concTerm);

        this.termify = new Termify(conclusion(concTerm), truthify);

    }


    @Override protected How action(RuleCause cause) {
        if (canMatchTask(taskPattern))
            throw new TermException("pattern to match premise task content is not taskable", taskPattern);
        if (canMatchTask(beliefPattern))
            throw new TermException("pattern to match premise belief content is not taskable", beliefPattern);
        return new TruthifyDeriveAction(CONSTRAINTS, truthify, taskPattern, beliefPattern, termify, time, cause);
    }

    private static boolean canMatchTask(Term pattern) {
        return !(pattern instanceof Variable) && !pattern.op().taskable;
    }


    private static final Pattern ruleImpl = Pattern.compile("\\|-");

    private static final Term eteConj = $.INSTANCE.the("eteConj");

    private static final Map<Term, Truthify> truthifies = new ConcurrentHashMap<>(512);


    static class UppercaseAtomsToPatternVariables extends RecursiveTermTransform.NegObliviousTermTransform {

        static final ImmutableSet<Atomic> reservedMetaInfoCategories = Sets.immutable.of(
            Atomic.the("Belief"),
            Atomic.the("Goal"),
            Atomic.the("Punctuation"),
            Atomic.the("Time")
        );
        final UnifiedMap<String, Term> map = new UnifiedMap<>(8);

        @Override
        public Term applyAtomic(Atomic atomic) {
            if (atomic instanceof Atom) {
                if (!reservedMetaInfoCategories.contains(atomic)) {
                    String name = atomic.toString();
                    if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
                        return map.computeIfAbsent(name, new Function<String, Term>() {
                            @Override
                            public Term apply(String n) {
                                return $.INSTANCE.varPattern(1 + map.size());
                            }
                        });

                    }
                }
            }
            return atomic;
        }

    }

    private static class MyPremiseRuleNormalization extends PremiseRuleNormalization {
        @Override
        public Term applyAtomic(Atomic x) {
            if (x instanceof Atom) {
                Functor f = Builtin.functor(x);
                return f != null ? f : x;
            } else
                return super.applyAtomic(x);
        }

    }

    /**
     * conclusion post-processing
     */
    private final RecursiveTermTransform.NegObliviousTermTransform ConcTransform = new RecursiveTermTransform.NegObliviousTermTransform() {
        @Override
        public Term applyPosCompound(Compound c) {

            Term f = Functor.func(c);
            if (f != Null) {
                Subterms a = Functor._args(c);
                if (f.equals(UniSubst.unisubst)) {
                    Unifiable.constrainUnifiable(a, PatternHow.this);
                } else if (f.equals(ConjMatch.BEFORE) || f.equals(ConjMatch.AFTER)) {
                    Unifiable.constraintEvent(a, PatternHow.this, true);
                } else if (f.equals(Derivation.Substitute)) {
                    Unifiable.constrainSubstitute(a, PatternHow.this);
                } else if (f.equals(Derivation.ConjWithout)) {
                    Unifiable.constraintEvent(a, PatternHow.this, false);
                }

                Term cc = a.sub(0);
                if (cc.op()==VAR_PATTERN) {
                    if (Arrays.asList(ConjMatch.BEFORE, ConjMatch.AFTER, ConjMatch.CONJ_WITHOUT_UNIFY).contains(f)) {
                        is(cc, Op.CONJ);
                    }
                }

            }
            return super.applyPosCompound(c);
        }
    };

    public static final class TruthifyDeriveAction extends How {

        public final Truthify truth;
        public final UnifyConstraint<Derivation.PremiseUnify>[] constraints;

        public final Term taskPat;
        public final Term beliefPat;
        public final Taskify taskify;
        /** +1 task first, -1 belief first, 0 unimportant (can decide dynamically from premise) */
        final int order;
        private final boolean patternsEqual;

        public TruthifyDeriveAction(UnifyConstraint<Derivation.PremiseUnify>[] constraints, Truthify truth, Term taskPattern, Term beliefPattern, Termify termify, Occurrify.OccurrenceSolver time, RuleCause cause) {
            super(cause);
            this.truth = truth;
            this.constraints = constraints;

            this.taskPat = taskPattern;
            this.beliefPat = beliefPattern;
            this.order = fwd(taskPattern, beliefPattern);
            this.taskify = new Taskify(termify, time, cause);
            this.patternsEqual = taskPat.equals(beliefPat);

        }

        /**
         * compute probabilistic throttle value, in consideration of the premise's task and the punctuation outcome
         * with respect to the deriver's punctuation equalization
         *
         * returning 0 invalidates this action for the provided derivation
         *
         * TO BE REFINED
         */
        @Override
        public final float priHeuristic(Derivation d) {

            byte punc = truth.punc.get(d.taskPunc);
            if ((int) punc == 0)
                return (float) 0;

            float puncFactor = d.preAmp(d.punc /*taskPunc*/, punc);
            if (puncFactor < Float.MIN_NORMAL)
                return 0f; //entirely disabled by deriver

            if (!truth.test(d))
                return (float) 0;

            return puncFactor * d.x.derivePri.prePri(d);
        }

        @Override
        public String toString() {
            return why.rule.toString();
        }

        @Override
        public final void run(Derivation d) {

            d.unify.constrain(constraints);


            boolean single = patternsEqual;
            //assert(!(single && !d.taskTerm.equals(d.beliefTerm))); //should be eliminated by prefilters
//        if (single && !d.taskTerm.equals(d.beliefTerm))
//            return false;

            boolean fwd = single || fwd(d);

            if (unify(d, fwd, single) && !single) {
                if (d.unify.live())
                    unify(d, !fwd, true);
            }
        }

        /** task,belief or belief,task ordering heuristic
         *  +1 = task first, -1 = belief first, 0 = doesnt matter
         **/
        static int fwd(Term T, Term B) {

            if (!T.equals(B)) {//if one is a variable, match the other since it will be more specific and fail faster
                if (T instanceof Variable && B instanceof Variable) return 0;
                if (B instanceof Variable) return +1;
                if (T instanceof Variable) return -1;

                //match ellipsis-containing term last
                boolean te = Terms.hasEllipsisRecurse(T), be = Terms.hasEllipsisRecurse(B);
                if (te || be) {
                    if (te && !be) return +1;
                    else if (!te && be) return -1;
                }


                //first if one is contained recursively by the other
                boolean Tb = T.containsRecursively(B);
                boolean Bt = B.containsRecursively(T);
                if (Tb && !Bt) return -1; //belief first as it is a part of Task
                if (Bt && !Tb) return +1; //task first as it is a part of Belief

                // first which is more specific in its constant structure
                int taskBits = Integer.bitCount(T.structure() & ~Op.Variable);
                int belfBits = Integer.bitCount(B.structure() & ~Op.Variable);
                if (belfBits > taskBits) return -1;
                if (taskBits > belfBits) return +1;

                //first which has fewer variables
                if (T.varPattern() > B.varPattern()) return -1;
                if (B.varPattern() > T.varPattern()) return +1;

                //first which is smaller
                return Integer.compare(B.volume(), T.volume());
            } else {
                return 0;
            }

        }


        final boolean unify(Derivation d, boolean dir, boolean finish) {
            return d.unify(dir ? taskPat : beliefPat, dir ? d.taskTerm : d.beliefTerm, finish ? taskify : null);
        }

        /** true: task first, false: belief first */
        boolean fwd(Derivation d) {
            switch (order) {
                case +1: return true;
                case -1: return false;
                default:
                    /* decide dynamically according to heuristic function of the premise values */

                    return true;

//                int taskVol = d.taskTerm.volume();
//                int beliefVol = d.beliefTerm.volume();
//                if (taskVol > beliefVol)
//                    return false;
//                if (taskVol < beliefVol)
//                    return true;

                //return d.random.nextBoolean();
            }
        }
    }
}





