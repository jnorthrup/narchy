package nars;

import jcog.Texts;
import nars.op.*;
import nars.op.data.flat;
import nars.op.data.reflect;
import nars.task.NALTask;
import nars.term.Variable;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.IdempotentBool;
import nars.term.atom.IdempotInt;
import nars.term.functor.AbstractInlineFunctor1;
import nars.term.functor.AbstractInlineFunctor2;
import nars.term.functor.UnaryBidiFunctor;
import nars.term.obj.QuantityTerm;
import nars.term.util.Image;
import nars.term.util.conj.Conj;
import nars.time.Tense;
import nars.util.var.DepIndepVarIntroduction;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.IntStream;

import static nars.Op.*;
import static nars.io.NarseseParser.termDynamic;
import static nars.op.Cmp.cmp;
import static nars.term.Functor.f0;
import static nars.term.atom.IdempotentBool.*;
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
public enum Builtin {
    ;
    public static final Functor[] statik = {

            Equal.equal,
            cmp,


            MathFunc.add,
            MathFunc.mul,
            MathFunc.XOR.xor,

            Member.member,

            Replace.replace,


            SetFunc.intersect,
            SetFunc.differ,
            SetFunc.union,


            ListFunc.append,
            ListFunc.reverse,
            ListFunc.sub,
            ListFunc.subs,

//            new AbstractInlineFunctor2("nullIfContainsEvent") {
//                @Override
//                protected Term apply(Term c, Term e) {
//                    if (c.equals(e)) return Null;
//                    if (Conj.containsEvent(c, e))
//                        return Null;
//                    return c;
//                }
//            },

            new AbstractInlineFunctor1.AbstractInstantFunctor1("unneg") {
                @Override protected Term apply1(Term x) { return x.unneg(); }
            },

            new AbstractInlineFunctor1.AbstractInstantFunctor1("imageNormalize") {
                @Override protected Term apply1(Term x) { return Image.imageNormalize(x); }
            },

            Functor.f2Inline("imageInt", Image::imageInt),
            Functor.f2Inline("imageExt", Image::imageExt),


            new AbstractInlineFunctor2("eventOf") {
                @Override
                protected Term apply(Term conj, Term event) {
                    return Conj.eventOf(conj, event) ? True : False;
                }
            },

            /** similar to without() but for any (but not necessarily ALL) (possibly-recursive) CONJ sub-events. removes all instances of the positive event */
            new AbstractInlineFunctor2("conjWithout") {
                @Override
                protected Term apply(Term conj, Term event) {
                    var x = Conj.diffAll(conj, event);
                    return conj.equals(x) ? Null : x;
                }
            },


            /** similar to without() but for any (but not necessarily ALL) (possibly-recursive) CONJ sub-events. removes all instances of the positive or negative of event */
            new AbstractInlineFunctor2("conjWithoutPN") {
                @Override
                protected Term apply(Term conj, Term event) {
                    var x = Conj.diffAllPN(conj, event);
                    return conj.equals(x) ? Null : x;
                }
            },


            /** applies the changes in structurally similar terms "from" and "to" to the target target */
            Functor.f3((Atom) $.the("substDiff"), (target, from, to) -> {
                if (from.equals(to))
                    return Null;

                int n;
                if (from.opID() == to.opID() && (n = from.subs()) == to.subs()) {

                    Map<Term, Term> m = null;
                    for (var i = 0; i < n; i++) {
                        var f = from.sub(i);
                        var t = to.sub(i);
                        if (!f.equals(t)) {
                            if (m == null) m = new UnifiedMap<>(1);
                            m.put(f, t);
                        }
                    }
                    if (m != null) {
                        var y = target.replace(m);
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


                var s = x.subs();
                if (s > 0) {
                    TreeSet<Term> indices = null;
                    for (var i = 0; i < s; i++) {
                        if (x.sub(i).equals(y)) {
                            if (indices == null) indices = new TreeSet();
                            indices.add(IdempotInt.the(i));
                        }
                    }
                    return indices == null ? Null : SETe.the(indices);
                }
                return Null;
            }),
            Functor.f2("keyValues", (x, y) -> {


                var s = x.subs();
                if (s > 0) {
                    TreeSet<Term> indices = null;
                    for (var i = 0; i < s; i++) {
                        if (x.sub(i).equals(y)) {
                            if (indices == null) indices = new TreeSet();
                            indices.add($.p(y, IdempotInt.the(i)));
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
                var s = x.subs();
                if (s > 0) {
                    var t = IntStream.range(0, s).mapToObj(i -> x.sub(i).equals(y) ? y : $.varDep("z" + i)).toArray(Term[]::new);
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

            new UnaryBidiFunctor("anon") {

                @Override
                protected Term compute(Term x) {
                    return x.op().var ? null : x.anon();
                }
            },


            Functor.f2("ifThen", (condition, conseq) -> {
                if (!condition.equals(True)) {
                    if (condition== Null)
                        return Null;
                    return null;
                } else
                    return conseq;
            }),

            Functor.f3("ifThenElse", (condition, ifTrue, ifFalse) -> {
                if (!(condition instanceof IdempotentBool))
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


            Functor.f2("subterm", (x, index) -> {
                try {
                    if (index instanceof IdempotInt && index.op() == INT) {
                        return x.sub($.intValue(index));
                    }
                } catch (NumberFormatException ignored) {
                }
                return null;
            }),

            Functor.f1("quote", x -> x)
    };

    private static final ImmutableMap<Term, Functor> statiks;
    static {
        MutableMap<Term,Functor> s = new UnifiedMap(statik.length);
        for (var f : statik) {
            s.put(f.term(), f);
        }
        statiks = s.toImmutable();
    }
    public static @Nullable Functor functor(Term x) {
        return statiks.get(x);
    }


    private static void registerFunctors(NAR nar) {
        for (var t : Builtin.statik)
            nar.add(t);


        nar.add(SetFunc.sort(nar));

        /** dynamic target builder - useful for NAR specific contexts like clock etc.. */
        nar.add(Functor.f(termDynamic, s -> {
            var o = Op.stringToOperator.get($.unquote(s.sub(0)));
            var args = s.sub(1).subterms().arrayShared();
            if (args.length == 2) {
                if (o.temporal) {

                    var dtTerm = s.sub(2);
                    if (!(dtTerm instanceof QuantityTerm))
                        dtTerm = QuantityTerm.the(dtTerm);

                    var dt = Tense.occToDT(nar.time.toCycles(((QuantityTerm) dtTerm).quant));
                    return o.the(dt, args);
                }

                throw new UnsupportedOperationException("unrecognized modifier argument: " + s);
            }

            return o.the(args);

        }));

        /** applies # dep and $ indep variable introduction if possible. returns the input term otherwise  */
        nar.add(Functor.f1Inline("varIntro", x -> {
            if (!(x instanceof Compound)) return Null;
            var result = DepIndepVarIntroduction.the.apply((Compound)x, nar.random(), null);
            return result != null ? result : Null;
        }));

        /** subterm, but specifically inside an ellipsis. otherwise pass through */
        nar.add(Functor.f("esubterm", c -> {


            var x = c.sub(0, null);
            if (x == null)
                return Null;

            var index = c.sub(1, Null);
            if (index == Null)
                return Null;

            int which;
            if (index != null) {
                if (index instanceof Variable)
                    return Null;

                which = $.intValue(index, -1);
                if (which < 0)
                    return Null;

            } else
                which = nar.random().nextInt(x.subs());

            return x.sub(which);
        }));

        nar.add(new AbstractInlineFunctor1("negateEvents") {
            @Override protected Term apply1(Term arg) {
                return Conj.negateEvents(arg);
            }
        });

        /** warning: this returns Null if unchanged */
        nar.add(new AbstractInlineFunctor2("without") {
            @Override
            protected Term apply(Term container, Term content) {
                var y = Terms.withoutAll(container, content::equals);
                return y == container ? Null : y;
            }
        });
        /** warning: this returns Null if unchanged */
        nar.add(new AbstractInlineFunctor2("unsect") {
            @Override
            protected Term apply(Term container, Term content) {
                var y = Terms.withoutAll(container, content.op() != CONJ ? content::equals : content::contains);
                return y == container ? Null : y;
            }
        });
        /** warning: this returns Null if unchanged */
        nar.add(new AbstractInlineFunctor2("withoutPN") {
            @Override
            protected Term apply(Term container, Term _content) {
                var content = _content.unneg();
                var y = Terms.withoutAll(container, content::equalsPosOrNeg);
                return y == container ? Null : y;
            }
        });
//        /** warning: this returns Null if unchanged */
//        nar.add(new AbstractInlineFunctor2("unsectPN") {
//            @Override
//            protected Term apply(Term container, Term _content) {
//                Term content = _content.unneg();
//                Term y = Terms.withoutAll(container, content.op()!=CONJ ? content::equalsPosOrNeg : content::containsPosOrNeg);
//                return y == container ? Null : y;
//            }
//        });


//        /**
//         * TODO rename this to 'dropAnyCommutive'
//         * remove an element from a commutive conjunction (or setAt), at random, and try re-creating
//         * the compound. wont necessarily work in all situations.
//         * TODO move the type restriction to another functor to wrap this
//         *
//         * this also filter a single variable (depvar) from being a result
//         */
//        nar.on(Functor.f1Inline("dropAnySet", (Term t) -> {
//            Op oo = t.op();
//
//            if (!oo.in(SETi.bit | SETe.bit | SECTi.bit | SECTe.bit))
//                return Bool.Null;
//
//            int size = t.subs();
//            switch (size) {
//                case 0:
//                    assert (false) : "empty set impossible here";
//                    return Bool.Null;
//                case 1:
//                    return Bool.Null; /* can't shrink below one element */
//                case 2:
//                    int n = nar.random().nextInt(2);
//                    return oo.the(t.sub(n)) /* keep the remaining target wrapped in a set */;
//                default:
//                    Term[] y = Terms.dropRandom(nar.random(), t.subterms());
//                    return oo.the(y);
//            }
//        }));


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








    nar.add(new AbstractInlineFunctor1("chooseAnySubEvent") {

        @Override
        protected Term apply1(Term conj) {
            return Conj.chooseEvent(conj, nar.random(),
                    true,
                    (when, what)->true);
        }
    });

   nar.add(new AbstractInlineFunctor2("chooseUnifiableSubEvent") {
           @Override
           protected Term apply(Term conj, Term event) {
               if (event instanceof nars.term.Variable)
                   return Null; //impossible
               //choose a likely unifiable candiate sub-event.  do not choose an equal match
               if (conj instanceof Neg)
                   conj = conj.unneg();

               var requireVars = !event.hasVars();
               if (requireVars && !conj.hasVars())
                   return Null;

//               boolean econj = event.op()==CONJ;
//               int edt = event.dt();

               return Conj.chooseEvent(conj, nar.random(),
                       //(!econj || edt!=DTERNAL), (!econj || edt!=0),
                       true,
                       (when, what)-> (!requireVars || what.hasVars())
                               && Terms.possiblyUnifiable(event, what, true, Op.Variable));
           }
       });


        nar.add(Functor.f1Concept("beliefTruth", nar, (c, n) -> $.quote(n.belief(c, n.time()))));
//        nar.on(Functor.f1Concept("goalTruth", nar, (c, n) -> $.quote(n.goal(c, n.time()))));

        nar.add(f0("self", nar::self));

        nar.add(Functor.f1("the", what -> {


            if (what instanceof Atom) {
                switch (what.toString()) {
                    case "sys":
                        return $.p(
                                $.quote(nar.emotion.summary()),
                                $.quote(nar.memory.summary()),
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


        nar.add(Functor.f("slice", (args) -> {
            if (args.subs() == 2) {
                var x = args.sub(0);
                if (x.subs() > 0) {
                    var len = x.subs();

                    var index = args.sub(1);
                    var o = index.op();
                    if (o == INT) {

                        var i = ((IdempotInt) index).i;
                        return i >= 0 && i < len ? x.sub(i) : False;

                    } else if (o == PROD && index.subs() == 2) {
                        var start = (index).sub(0);
                        if (start.op() == INT) {
                            var end = (index).sub(1);
                            if (end.op() == INT) {
                                var si = ((IdempotInt) start).i;
                                if (si >= 0 && si < len) {
                                    var ei = ((IdempotInt) end).i;
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


        nar.setOp(Task.BeliefAtom, (x, nn) -> Task.tryTask(x.term().sub(0).sub(0), BELIEF, $.t(1f, nn.confDefault(BELIEF)), (term, truth) -> ((Task) NALTask.the(term, BELIEF, truth, nn.time(), ETERNAL, ETERNAL, nn.evidence())).pri(nn.priDefault(BELIEF)))
        );


        nar.addOp1("assertTrue", (x, nn) -> {
            if (!x.op().var)
                assertSame(True, x);
        });

        nar.addOp2("assertEquals", (x, y, nn) -> {
            if (!/*x.hasVars*/x.op().var && !y.op().var)
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

//        initMemoryOps(nar);
    }

//    private static void initMemoryOps(NAR nar) {
//        nar.addOp1("load", (id, nn) -> {
//            Runnable r = nn.memoryExternal.copy(id, nn.self());
//            if (r != null)
//                nn.runLater(r);
//        });
//
////        /** eternal tasks only */
////        nar.onOp1("remember", (id, nn) -> {
////            nn.runLater(() -> {
////                save(nn, id, Task::isEternal);
////                nn.logger.info("remembered {}", id);
////            });
////        });
////
////        /** all tasks */
////        nar.onOp1("save", (id, nn) -> {
////            nn.runLater(() -> {
////                save(nn, id, (t) -> true);
////                nn.logger.info("saved {}", id);
////            });
////        });
//
//        nar.addOp2("memory2txtfile", (id, filePath, nn) -> nn.runLater(() -> {
//            try {
//                PrintStream p = new PrintStream(new FileOutputStream($.unquote(filePath)));
//                User.the().get(id.toString(), (byte[] x) -> {
//                    try {
//                        IO.readTasks(x, (Task t) -> p.println(t.toString(true)));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                });
//                p.close();
//
//                //nn.log("saved {} to {}", id, filePath);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        }));
//    }


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


















