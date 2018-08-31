package nars;

import jcog.Texts;
import jcog.User;
import jcog.data.list.FasterList;
import jcog.pri.PriReference;
import nars.concept.Concept;
import nars.op.*;
import nars.op.data.flat;
import nars.op.data.reflect;
import nars.subterm.Subterms;
import nars.task.NALTask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.obj.QuantityTerm;
import nars.term.util.Conj;
import nars.term.util.Image;
import nars.time.Tense;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;

import static nars.Op.*;
import static nars.term.Functor.f0;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Built-in set of default Functors and Operators, registered into a NAR on initialization
 * Provides the standard core function library
 * <p>
 * see:
 * https:
 */
public class Builtin {

    public static final Functor[] statik = {

            Equal.the,
            Member.the,

            Subst.replace,


            SetFunc.intersect,
            SetFunc.differ,
            SetFunc.union,


            ListFunc.append,
            ListFunc.reverse,
            ListFunc.sub,
            ListFunc.subs,

            Image.imageNormalize,
            Image.imageInt,
            Image.imageExt,

            /** XOR(a,b) == (a && --b) || (--a && b) */
            new Functor.AbstractInlineFunctor2("xor") {
                @Override
                protected Term apply(Term a, Term b) {
                    return $.or( $.and(a, b.neg()), $.and(a.neg(), b) );
                }
            },

            /** similar to without() but for (possibly-recursive) CONJ sub-events. removes all instances of the positive event */
            new Functor.AbstractInlineFunctor2("conjWithout") {
                @Override
                protected Term apply(Term conj, Term event) {
                    Term x = Conj.without(conj, event, false);
                    if (conj.equals(x))
                        return Null;
                    return x;
                }
            },
            /** similar to without() but for (possibly-recursive) CONJ sub-events. removes all instances of the positive event */
            new Functor.AbstractInlineFunctor2("conjWithoutAll") {
                @Override
                protected Term apply(Term include, Term exclude) {
                    Term x = Conj.withoutAll(include, exclude);
                    if (include.equals(x))
                        return Null;
                    return x;
                }
            },

            /** applies the changes in structurally similar terms "from" and "to" to the target term */
            Functor.f3((Atom) $.the("substDiff"), (target, from, to) -> {
                if (from.equals(to))
                    return Null;

                int n;
                if (from.op() == to.op() && (n = from.subs()) == to.subs()) {

                    Map<Term, Term> m = null;
                    for (int i = 0; i < n; i++) {
                        Term f = from.sub(i);
                        Term t = to.sub(i);
                        if (!f.equals(t)) {
                            if (m == null) m = new UnifiedMap<>(1);
                            m.put(f, t);
                        }
                    }
                    if (m != null) {
                        Term y = target.replace(m);
                        if (y != null && !y.equals(target))
                            return y;
                    }
                }
                return Null;
            }),

            /** similar to C/Java "indexOf" but returns a set of all numeric indices where the 2nd argument occurrs as a subterm of the first
             *  if not present, returns Null
             * */

            Functor.f2("indicesOf", (x, y) -> {


                int s = x.subs();
                if (s > 0) {
                    TreeSet<Term> indices = null;
                    for (int i = 0; i < s; i++) {
                        if (x.sub(i).equals(y)) {
                            if (indices == null) indices = new TreeSet();
                            indices.add(Int.the(i));
                        }
                    }
                    if (indices == null)
                        return Null;
                    else {
                        return SETe.the(indices);

                    }
                }
                return Null;
            }),
            Functor.f2("keyValues", (x, y) -> {


                int s = x.subs();
                if (s > 0) {
                    TreeSet<Term> indices = null;
                    for (int i = 0; i < s; i++) {
                        if (x.sub(i).equals(y)) {
                            if (indices == null) indices = new TreeSet();
                            indices.add($.p(y, Int.the(i)));
                        }
                    }
                    if (indices == null)
                        return Null;
                    else {
                        switch (indices.size()) {
                            case 0:
                                return Null;
                            case 1:
                                return indices.first();
                            default:

                                return SETe.the(indices);
                        }
                    }
                }
                return Null;
            }),

            Functor.f2("varMask", (x, y) -> {
                int s = x.subs();
                if (s > 0) {
                    Term[] t = new Term[s];
                    for (int i = 0; i < s; i++) {
                        t[i] = x.sub(i).equals(y) ? y : $.varDep("z" + i);
                    }
                    return $.p(t);
                }
                return Null;
            }),


            Functor.f1Const("reflect", reflect::reflect),


            Functor.f1Const("toString", x -> $.quote(x.toString())),
            Functor.f1Const("toChars", x -> $.p(x.toString().toCharArray(), $::the)),
            Functor.f1Const("complexity", x -> $.the(x.complexity())),

            flat.flatProduct,

            Functor.f2("similaritree", (a, b) ->
                    ((a instanceof Variable) || (b instanceof Variable)) ? null :
                            $.the(Texts.levenshteinDistance(a.toString(), b.toString()))
            ),

            new Functor.UnaryBidiFunctor("anon") {

                @Override
                protected Term compute(Term x) {
                    return x.op().var ? null : x.anon();
                }
            },


            Functor.f2("ifThen", (condition, conseq) -> {
                if (!condition.equals(True)) {
                    if (condition==Null)
                        return Null;
                    return null;
                } else
                    return conseq;
            }),

            Functor.f3("ifThenElse", (condition, ifTrue, ifFalse) -> {
                if (!(condition instanceof Bool))
                    return null;
                else {
                    if (condition == True)
                        return ifTrue;
                    else if (condition == False)
                        return ifFalse;
                    else
                        return Null;
                }
            }),

            Functor.f3("ifOrElse", (condition, conseqTrue, conseqFalse) -> {
                if (condition.hasVars()) return null;
                else {
                    if (condition.equals(True))
                        return conseqTrue;
                    else if (condition.equals(False))
                        return conseqFalse;
                }
                return Null;
            }),

            Functor.f2("ifNeqRoot", (returned, compareTo) ->
                    !returned.equalsRoot(compareTo) ? returned : Null
            ),


            Functor.f2("subterm", (Term x, Term index) -> {
                try {
                    if (index instanceof Int && index.op() == INT) {
                        return x.sub($.intValue(index));
                    }
                } catch (NumberFormatException ignored) {
                }
                return null;
            }),


            MathFunc.add,
            MathFunc.mul,


            Functor.f1("quote", x -> x)
    };

    private static final ImmutableMap<Term, Functor> statiks;
    static {
        MutableMap<Term,Functor> s = new UnifiedMap(statik.length);
        for (Functor f : statik) {
            s.put(f.term(), f);
        }
        statiks = s.toImmutable();
    }
    @Nullable public static Functor functor(Term x) {
        return statiks.get(x);
    }


    private static void registerFunctors(NAR nar) {
        for (Functor t : Builtin.statik) {
            nar.on(t);
        }

        nar.on(SetFunc.sort(nar));

        /** dynamic term builder - useful for NAR specific contexts like clock etc.. */
        nar.on(Functor.f("term", (Subterms s) -> {
            Op o = Op.stringToOperator.get($.unquote(s.sub(0)));
            Term[] args = s.sub(1).subterms().arrayShared();
            if (args.length == 2) {
                if (o.temporal) {

                    Term dtTerm = s.sub(2);
                    if (!(dtTerm instanceof QuantityTerm)) {
                        dtTerm = QuantityTerm.the(dtTerm);
                    }

                    int dt = Tense.occToDT(nar.time.toCycles(((QuantityTerm) dtTerm).quant));
                    return o.the(dt, args);
                }

                throw new UnsupportedOperationException("unrecognized modifier argument: " + s);
            }

            return o.the(args);

        }));

        nar.on(Functor.f1("varIntro", (x) -> {
            Pair<Term, Map<Term, Term>> result = nars.op.DepIndepVarIntroduction.the.apply(x, nar.random());
            return result != null ? result.getOne() : Null;
        }));

        nar.on(Functor.f1((Atom) $.the("termlinkRandom"), (Term t) -> {
            @Nullable Concept c = nar.conceptualize(t);
            if (c == null)
                return Null;
            @Nullable PriReference<Term> tl = c.termlinks().sample(nar.random());
            if (tl == null)
                return Null;
            return tl.get();
        }));


        /** subterm, but specifically inside an ellipsis. otherwise pass through */
        nar.on(Functor.f("esubterm", (Subterms c) -> {


            Term x = c.sub(0, null);
            if (x == null)
                return Null;

            Term index = c.sub(1, Null);
            if (index == Null)
                return Null;

            int which;
            if (index != null) {
                if (index instanceof Variable)
                    return Null;

                which = $.intValue(index, -1);
                if (which < 0) {
                    return Null;
                }
            } else {

                which = nar.random().nextInt(x.subs());
            }

            return x.sub(which);


        }));

        nar.on(new Functor.AbstractInlineFunctor2("without") {
            @Override
            protected Term apply(Term container, Term content) {
                return Op.without(container, x -> x.equals(content), nar.random());
            }
        });
        nar.on(new Functor.AbstractInlineFunctor2("withoutPosOrNeg") {
            @Override
            protected Term apply(Term container, Term content) {
                Term c = content.unneg();
                return Op.without(container, x -> x.unneg().equals(c), nar.random());
            }
        });


        /**
         * TODO rename this to 'dropAnyCommutive'
         * remove an element from a commutive conjunction (or set), at random, and try re-creating
         * the compound. wont necessarily work in all situations.
         * TODO move the type restriction to another functor to wrap this
         *
         * this also filter a single variable (depvar) from being a result
         */
        nar.on(Functor.f1Inline("dropAnySet", (Term t) -> {
            Op oo = t.op();

            if (!oo.in(SETi.bit | SETe.bit | SECTi.bit | SECTe.bit))
                return Null;

            int size = t.subs();
            switch (size) {
                case 0:
                    assert (false) : "empty set impossible here";
                    return Null;
                case 1:
                    return Null; /* can't shrink below one element */
                case 2:
                    int n = nar.random().nextInt(2);
                    return oo.the(t.sub(n)) /* keep the remaining term wrapped in a set */;
                default:
                    Term[] y = Terms.dropRandom(nar.random(), t.subterms());
                    return oo.the(y);
            }
        }));


//        /** depvar cleaning from commutive conj */
//        nar.on(Functor.f1((Atom) $.the("ifConjCommNoDepVars"), (Term t) -> {
//            if (!t.hasAny(VAR_DEP))
//                return t;
//            Op oo = t.op();
//            if (oo != CONJ)
//                return t;
//
//
//            SortedSet<Term> s = t.subterms().toSetSorted();
//            if (!s.removeIf(x -> x.unneg().op() == VAR_DEP))
//                return t;
//
//            return CONJ.the(t.dt(), s);
//        }));

        /** drops a random contained event, whether at first layer or below */
        nar.on(Functor.f1Inline("dropAnyEvent", (Term t) -> {
            Op oo = t.op();
            boolean negated = (oo == NEG);
            if (negated) {
                t = t.unneg();
                oo = t.op();
            }
            if (oo != CONJ)
                return Null;

            FasterList<LongObjectPair<Term>> ee = Conj.eventList(t);
            ee.remove(nar.random().nextInt(ee.size()));
            Term x = Conj.conj(ee);
            if (x.equals(t))
                return Null;
            assert(x != null);
            return x.negIf(negated);
        }));


        /** similar to without() but for (possibly-recursive) CONJ sub-events. removes all instances of the positive or negative of event */
        nar.on(new Functor.AbstractInlineFunctor2("conjWithoutPosOrNeg") {
            @Override
            protected Term apply(Term conj, Term event) {
                Term x = Conj.without(conj, event, true);
                if (conj.equals(x))
                    return Null;
                return x;
            }
        });



        nar.on(new Functor.AbstractInlineFunctor2("conjDropIfLatest") {
            @Override
            protected Term apply(Term conj, Term event) {
                Term x = Conj.conjDrop(conj, event, false, false);
                if (conj.equals(x))
                    return Null;
                return x;
            }
        });

        nar.on(new Functor.AbstractInlineFunctor2("conjDropIfEarliest") {
            @Override
            protected Term apply(Term conj, Term event) {
                Term x = Conj.conjDrop(conj, event, true, false);
                if (conj.equals(x))
                    return Null;
                return x;
            }
        });
        nar.on(new Functor.AbstractInlineFunctor2("conjDropIfEarliestFiltered") {
            @Override
            protected Term apply(Term conj, Term event) {
                Term x = Conj.conjDrop(conj, event, true, true);
                if (conj.equals(x))
                    return Null;
                return x;
            }
        });
        nar.on(new Functor.AbstractInlineFunctor2("conjDropIfLatestFiltered") {
            @Override
            protected Term apply(Term conj, Term event) {
                Term x = Conj.conjDrop(conj, event, false, true);
                if (conj.equals(x))
                    return Null;
                return x;
            }
        });

        nar.on(Functor.f1Concept("beliefTruth", nar, (c, n) -> $.quote(n.belief(c, n.time()))));
        nar.on(Functor.f1Concept("goalTruth", nar, (c, n) -> $.quote(n.goal(c, n.time()))));

        nar.on(f0("self", nar::self));

        nar.on(Functor.f1("the", what -> {


            if (what instanceof Atom) {
                switch (what.toString()) {
                    case "sys":
                        return $.p(
                                $.quote(nar.emotion.summary()),
                                $.quote(nar.concepts.summary()),
                                $.quote(nar.emotion.summary()),
                                $.quote(nar.exe.toString())
                        );
                }
            }

            Object x = nar.concept(what);
            if (x == null)
                x = what;

            return $.quote($.p($.quote(x.getClass().toString()), $.quote(x.toString())));
        }));


        nar.on(Functor.f("slice", (args) -> {
            if (args.subs() == 2) {
                Term x = args.sub(0);
                if (x.subs() > 0) {
                    int len = x.subs();

                    Term index = args.sub(1);
                    Op o = index.op();
                    if (o == INT) {

                        int i = ((Int) index).id;
                        if (i >= 0 && i < len)
                            return x.sub(i);
                        else
                            return False;

                    } else if (o == PROD && index.subs() == 2) {
                        Term start = (index).sub(0);
                        if (start.op() == INT) {
                            Term end = (index).sub(1);
                            if (end.op() == INT) {
                                int si = ((Int) start).id;
                                if (si >= 0 && si < len) {
                                    int ei = ((Int) end).id;
                                    if (ei >= 0 && ei <= len) {
                                        if (si == ei)
                                            return Op.EmptyProduct;
                                        if (si < ei) {
                                            return $.p(Arrays.copyOfRange(x.subterms().arrayClone(), si, ei));
                                        }
                                    }
                                }

                                return False;
                            }
                        }

                    }
                }
            }
            return null;
        }));
    }

    private static void registerOperators(NAR nar) {


        nar.onOp(Op.Belief, (x, nn) -> Task.tryTask(x.term().sub(0).sub(0), BELIEF, $.t(1f, nn.confDefault(BELIEF)), (term, truth) -> new NALTask(term, BELIEF, truth,
                    nn.time(), ETERNAL, ETERNAL, nn.evidence()).priSet(nn.priDefault(BELIEF)))
        );


        nar.onOp1("assertTrue", (x, nn) -> {
            if (!x.op().var)
                assertSame(True, x);
        });

        nar.onOp2("assertEquals", (x, y, nn) -> {
            if (!x.op().var && !y.op().var)
                assertEquals(/*msg,*/ x, y);
        });

//        nar.onOp1("js", (code, nn) -> {
//            if (code.op() == ATOM) {
//                String js = $.unquote(code);
//                Object result;
//                try {
//                    result = NARjs.the().eval(js);
//                } catch (ScriptException e) {
//                    result = e;
//                }
//                nn.input(Operator.log(nar.time(), $.p(code, $.the(result))));
//            }
//        });

        initMemoryOps(nar);
    }

    private static void initMemoryOps(NAR nar) {
        nar.onOp1("load", (id, nn) -> {
            Runnable r = nn.memory.copy(id, nn.self());
            if (r != null)
                nn.runLater(r);
        });

//        /** eternal tasks only */
//        nar.onOp1("remember", (id, nn) -> {
//            nn.runLater(() -> {
//                save(nn, id, Task::isEternal);
//                nn.logger.info("remembered {}", id);
//            });
//        });
//
//        /** all tasks */
//        nar.onOp1("save", (id, nn) -> {
//            nn.runLater(() -> {
//                save(nn, id, (t) -> true);
//                nn.logger.info("saved {}", id);
//            });
//        });

        nar.onOp2("memory2txtfile", (id, filePath, nn) -> nn.runLater(() -> {
            try {
                PrintStream p = new PrintStream(new FileOutputStream($.unquote(filePath)));
                User.the().get(id.toString(), (byte[] x) -> {
                    try {
                        IO.readTasks(x, (Task t) -> p.println(t.toString(true)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                p.close();

                nn.logger.info("saved {} to {}", id, filePath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }));
    }


//
//    static void save(NAR nar, Term id, Predicate<Task> filter) {
//        ByteArrayOutputStream memDump;
//        nar.outputBinary(memDump = new ByteArrayOutputStream(256 * 1024), filter);
//        User.the().put(id.toString(), memDump.toByteArray());
//    }


    public static void init(NAR nar) {
        registerFunctors(nar);
        registerOperators(nar);
    }

}


















