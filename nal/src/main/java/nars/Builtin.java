package nars;

import jcog.Texts;
import jcog.User;
import jcog.list.FasterList;
import jcog.pri.PriReference;
import nars.concept.Concept;
import nars.concept.Operator;
import nars.op.*;
import nars.op.data.flat;
import nars.op.data.reflect;
import nars.op.java.Opjects;
import nars.subterm.Subterms;
import nars.task.NALTask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Int;
import nars.term.compound.util.Conj;
import nars.term.compound.util.Image;
import nars.term.obj.QuantityTerm;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import javax.script.ScriptException;
import java.io.*;
import java.util.*;
import java.util.function.Predicate;

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
 * https://en.wikibooks.org/wiki/Prolog/Built-in_predicates
 */
public class Builtin {

    public static final Concept[] statik = {

            Subst.replace,

            //TODO move these to fields of SetFunc
            SetFunc.intersect,
            SetFunc.differ,
            SetFunc.union,


            ListFunc.append,
            ListFunc.sub,
            ListFunc.subs,

            Image.imageNormalize,
            Image.imageInt,
            Image.imageExt,

            /** similar to without() but for (possibly-recursive) CONJ sub-events. removes all instances of the positive event */
            new Functor.AbstractInlineFunctor2("conjWithout") {
                @Override
                protected Term apply(Term conj, Term event) {
                    Term x = Conj.without(conj, event, false);
                    if (conj.equals(x))
                        return Null; //HACK this is used for derivations but in ordinary usage should return the instance not Null
                    return x;
                }
            },
            /** similar to without() but for (possibly-recursive) CONJ sub-events. removes all instances of the positive event */
            new Functor.AbstractInlineFunctor2("conjWithoutAll") {
                @Override
                protected Term apply(Term include, Term exclude) {
                    Term x = Conj.withoutAll(include, exclude);
                    if (include.equals(x))
                        return Null; //HACK this is used for derivations but in ordinary usage should return the instance not Null
                    return x;
                }
            },

            /** applies the changes in structurally similar terms "from" and "to" to the target term */
            Functor.f3((Atom) $.the("substDiff"), (target, from, to) -> {
                if (from.equals(to))
                    return Null; //only interested in when there is a difference to apply

                int n;
                if (from.op() == to.op() && (n = from.subs()) == to.subs()) {
                    //likely they have the same structure
                    Map<Term, Term> m = null; //lazy alloc
                    for (int i = 0; i < n; i++) {
                        Term f = from.sub(i);
                        Term t = to.sub(i);
                        if (!f.equals(t)) {
                            if (m == null) m = new UnifiedMap<>(1);
                            m.put(f, t);
                        }
                    }
                    if (m != null) { //can be empty in 'dt' cases
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
            //Functor.f2("numIndicesOf", (x,y) -> {
            Functor.f2("indicesOf", (x, y) -> {
                //TODO if not impossible subterm...

                int s = x.subs();
                if (s > 0) {
                    TreeSet<Term> indices = null; //lazy alloc
                    for (int i = 0; i < s; i++) {
                        if (x.sub(i).equals(y)) {
                            if (indices == null) indices = new TreeSet();
                            indices.add(Int.the(i));
                        }
                    }
                    if (indices == null)
                        return Null;
                    else {
                        return $.sete(indices);
                        //return $.secte(indices);
                    }
                }
                return Null;
            }),
            Functor.f2("keyValues", (x, y) -> {
                //TODO if not impossible subterm...

                //Functor.f2("indicesOf", (x,y) -> {
                int s = x.subs();
                if (s > 0) {
                    TreeSet<Term> indices = null; //lazy alloc
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
                                return Null; //shouldnt happen
                            case 1:
                                return indices.first();
                            default:
                                //return $.secte(indices);
                                return $.sete(indices);
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
                        t[i] = x.sub(i).equals(y) ? y : $.varDep("z" + i); //unnormalized vars
                    }
                    return $.p(t);
                }
                return Null;
            }),

            //Functor.f0("date", () -> quote(new Date().toString())),

            Functor.f1Const("reflect", reflect::reflect),

//            Functor.f1Const("fromJSON", (jsonString)-> IO.fromJSON($.unquote(jsonString))),
//            Functor.f1Const("toJSON", IO::toJSON),

            Functor.f1Const("toString", x -> $.quote(x.toString())),
            Functor.f1Const("toChars", x -> $.p(x.toString().toCharArray(), $::the)),
            Functor.f1Const("complexity", x -> $.the(x.complexity())),

            flat.flatProduct,

            Functor.f2("similaritree", (a, b) ->
                    ((a instanceof Variable) || (b instanceof Variable)) ? null :
                            $.the(Texts.levenshteinDistance(a.toString(), b.toString()))
            ),

            new Functor.CommutiveBinaryBidiFunctor("equal") {

                @Override
                protected Term compute(Term x, Term y) {
                    if (x.equals(y))
                        return True; //equal

                    if (x.hasVars() || y.hasVars())
                        return null; //unknown

                    return False; //inequal
                }

                @Override
                protected Term computeFromXY(Term x, Term y, Term xy) {
                    return null;
                }

                @Override
                protected Term computeXfromYandXY(Term x, Term y, Term xy) {
                    return xy == True ? y : null;
                }

            },

            Functor.f2("if", (condition, conseq) -> {
                if (condition.hasVars()) return null; //unknown
                else {
                    if (condition.equals(True))
                        return conseq;
                }
                return Null;
            }),

            Functor.f3("ifOrElse", (condition, conseqTrue, conseqFalse) -> {
                if (condition.hasVars()) return null; //unknown
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

            //TODO for binding equal values
//            Functor.f2("equality", (x, y) ->
//                    x.equals(y) ? True : ((x instanceof Variable) || (y instanceof Variable) ? null :
//                    --(x diff y) && --(y diff x) )),

            Functor.f2("subterm", (Term x, Term index) -> {
                try {
                    if (index instanceof Int && index.op() == INT) {
                        return x.sub($.intValue(index));
                    }
                } catch (NumberFormatException ignored) {
                }
                return null;
            }),

            //TODO add exceptions for identities: ex: add(#x,0) --> #x  etc
            MathFunc.add,
            MathFunc.mul,

            //Functor.f2Int("sub", (x, y) -> x - y),


            Functor.f1("quote", x -> x) //TODO does this work    //throw new RuntimeException("quote should never actually be invoked by the system");
    };


    //TODO: http://software-lab.de/doc/ref.html#fun
    //TODO: https://openreview.net/pdf?id=ByldLrqlx (section F)

    public static void init(NAR nar) {
        registerFunctors(nar);
        registerOperators(nar);
    }

    public static void registerFunctors(NAR nar) {
        for (Concept t : Builtin.statik) {
            nar.on(t);
        }

        nar.on(SetFunc.sort(nar));

        /** dynamic term builder - useful for NAR specific contexts like clock etc.. */
        nar.on(Functor.f("term", (Subterms s) -> {
            Op o = Op.stringToOperator.get($.unquote(s.sub(0)));
            Term[] args = s.sub(1).subterms().arrayClone();
            if (s.subs() > 2) {
                if (o.temporal) {
                    //decode DT field
                    Term dtTerm = s.sub(2);
                    if (!(dtTerm instanceof QuantityTerm)) {
                        dtTerm = QuantityTerm.the(dtTerm);
                    }

                    long dt = nar.time.toCycles(((QuantityTerm) dtTerm).quant);
                    if (Math.abs(dt) < Integer.MAX_VALUE - 2) {
                        return o.compound((int) dt, args);
                    } else {
                        throw new UnsupportedOperationException("time unit too large for 32-bit DT interval");
                    }

                }

                throw new UnsupportedOperationException("unrecognized modifier argument: " + s);
            }

            return o.the(args);

        }));

        nar.on(Functor.f1("varIntro", (x) -> {
            Pair<Term, Map<Term, Term>> result = DepIndepVarIntroduction.the.apply(x, nar.random());
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
//        nar.on(Functor.f("service", (TermContainer c) ->
//                $.sete(
//                        nar.services().map(
//                                (e) ->
//                                        $.p(e, $.the(e.getValue().state())))
//                                .toArray(Term[]::new)
//                )
//        ));

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
                //random
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
        nar.on(Functor.f1((Atom) $.the("dropAnySet"), (Term t) -> {
            Op oo = t.op();

            if (oo == INT) {
                if (t instanceof Int.IntRange) {
                    //select random location in the int and split either up or down
                    Int.IntRange i = (Int.IntRange) t;
                    Random rng = nar.random();
                    if (i.min + 1 == i.max) {
                        //arity=2
                        return Int.the(rng.nextBoolean() ? i.min : i.max);
                    } else if (i.min + 2 == i.max) {
                        //arity=3
                        switch (rng.nextInt(4)) {
                            case 0:
                                return Int.the(i.min);
                            case 1:
                                return Int.range(i.min, i.min + 1);
                            case 2:
                                return Int.range(i.min + 1, i.min + 2);
                            case 3:
                                return Int.the(i.max);
                            default:
                                throw new UnsupportedOperationException();
                        }
                    } else {
                        int split =
                                (i.max + i.min) / 2; //midpoint, deterministic
                        //rng.nextInt(i.max-i.min-2);
                        return (rng.nextBoolean()) ?
                                Int.range(i.min, split + 1) :
                                Int.range(split + 1, i.max);
                    }
                }

                //cant drop int by itself
                return Null;
            }

            if (!oo.in(SETi.bit | SETe.bit | SECTi.bit | SECTe.bit))
                return Null;//returning the original value may cause feedback loop in callees expcting a change in value

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


        /** depvar cleaning from commutive conj */
        nar.on(Functor.f1((Atom) $.the("ifConjCommNoDepVars"), (Term t) -> {
            if (!t.hasAny(VAR_DEP))
                return t;
            Op oo = t.op();
            if (oo != CONJ)
                return t;


            SortedSet<Term> s = t.subterms().toSetSorted();
            if (!s.removeIf(x -> x.unneg().op() == VAR_DEP))
                return t;

            return CONJ.the(t.dt(), s);
        }));

        /** drops a random contained event, whether at first layer or below */
        nar.on(Functor.f1((Atom) $.the("dropAnyEvent"), (Term t) -> {
            Op oo = t.op();
            if (oo != CONJ)
                return Null;//returning the original value may cause feedback loop in callees expcting a change in value

            FasterList<LongObjectPair<Term>> ee = Conj.eventList(t);
            ee.remove(nar.random().nextInt(ee.size()));
            return Conj.conj(ee);

//            }

//            if (r instanceof Variable /*&& r.op()!=VAR_DEP*/)
//                return Null; //HACK dont allow returning a variable as an event during decomposition HACK TODO make more careful and return the only result if one subterm is a non-returnable variable

//            return r;
        }));



        /** similar to without() but for (possibly-recursive) CONJ sub-events. removes all instances of the positive or negative of event */
        nar.on(new Functor.AbstractInlineFunctor2("conjWithoutPosOrNeg") {
            @Override
            protected Term apply(Term conj, Term event) {
                Term x = Conj.without(conj, event, true);
                if (conj.equals(x))
                    return Null; //HACK this is used for derivations but in ordinary usage should return the instance not Null
                return x;
            }
        });

        nar.on(new Functor.AbstractInlineFunctor2("conjDropIfLatest") {
            @Override
            protected Term apply(Term conj, Term event) {
                Term x = Conj.conjDrop(conj, event, false);
                if (conj.equals(x))
                    return Null; //HACK this is used for derivations but in ordinary usage should return the instance not Null
                return x;
            }
        });

        nar.on(new Functor.AbstractInlineFunctor2("conjDropIfEarliest") {
            @Override
            protected Term apply(Term conj, Term event) {
                Term x = Conj.conjDrop(conj, event, true);
                if (conj.equals(x))
                    return Null; //HACK this is used for derivations but in ordinary usage should return the instance not Null
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


//            /** slice(<compound>,<selector>)
//            selector :-
//            a specific integer value index, from 0 to compound size
//            (a,b) pair of integers, a range of indices */
        nar.on(Functor.f("slice", (args) -> {
            if (args.subs() == 2) {
                Term x = args.sub(0);
                if (x.subs() > 0) {
                    int len = x.subs();

                    Term index = args.sub(1);
                    Op o = index.op();
                    if (o == INT) {
                        //specific index
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
                                //TODO maybe reverse order will return reversed subproduct
                                return False;
                            }
                        }

                    }
                }
            }
            return null;
        }));
    }

    static void registerOperators(NAR nar) {

        //new System(nar);

        nar.onOp(Op.BELIEF_TERM, (x, nn) -> {
                    return Task.tryTask(x.term().sub(0).sub(0), BELIEF, $.t(1f, nn.confDefault(BELIEF)), (term, truth) -> {
                        return new NALTask(term, BELIEF, truth,
                                nn.time(), ETERNAL, ETERNAL, nn.evidence()).pri(nn.priDefault(BELIEF));
                    });
                }
        );
//        nar.onOp(Op.GOAL_TERM, (x, nn) -> {
//            nar.goal(x);
//        });
//        nar.onOp(Op.QUESTION_TERM, (x, nn) -> {
//            nar.question(x);
//        });
//        nar.onOp(Op.QUEST_TERM, (x, nn) -> {
//            nar.quest(x);
//        });

        nar.onOp1("assertTrue", (x, nn) -> {
            if (!x.op().var)
                assertSame(True, x);
        });

        nar.onOp2("assertEquals", (x, y, nn) -> {
            if (!x.op().var && !y.op().var)
                assertEquals(/*msg,*/ x, y);
        });

        nar.onOp1("js", (code, nn) -> {
            if (code.op() == ATOM) {
                String js = $.unquote(code);
                Object result;
                try {
                    result = NARjs.the().eval(js);
                } catch (ScriptException e) {
                    result = e;
                }
                nn.input(Operator.log(nar.time(), $.p(code, $.the(result))));
            }
        });

        initMemoryOps(nar);
    }

    static void initMemoryOps(NAR nar) {
        nar.onOp1("load", (id, nn) -> {
            Runnable r = nn.memory.copy(id, nn.self());
            if (r != null)
                nn.runLater(r);
        });

        /** eternal tasks only */
        nar.onOp1("remember", (id, nn) -> {
            nn.runLater(() -> {
                save(nn, id, Task::isEternal);
                nn.logger.info("remembered {}", id);
            });
        });

        /** all tasks */
        nar.onOp1("save", (id, nn) -> {
            nn.runLater(() -> {
                save(nn, id, (t) -> true);
                nn.logger.info("saved {}", id);
            });
        });

        nar.onOp2("memory2txtfile", (id, filePath, nn) -> {
            nn.runLater(() -> {
                try {
                    PrintStream p = new PrintStream(new FileOutputStream($.unquote(filePath)));
                    User.the().get(id.toString(), (byte[] x) -> {
                        try {
                            IO.readTasks(x, (Task t) -> {
                                p.println(t.toString(true));
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    p.close();

                    nn.logger.info("saved {} to {}", id, filePath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    static void save(NAR nar, Term id, Predicate<Task> filter) {
        ByteArrayOutputStream memDump;
        nar.outputBinary(memDump = new ByteArrayOutputStream(256 * 1024), filter);
        User.the().put(id.toString(), memDump.toByteArray());
    }

    public static class System {

        private final NAR nar;
        private final Opjects opj;

        public System() {
            //used by the proxy instance
            this.nar = null;
            this.opj = null;
        }

        public System(NAR n) {
            this.nar = n;
            opj = new Opjects(n);
            opj.the("sys", this);
        }

        /**
         * shutdown, terminates VM
         */
        public void off() {

            nar.stop();
            java.lang.System.exit(0);
        }

        /**
         * NAR clear
         */
        public void clear() {
            nar.clear();
        }

        public void reset() {
            nar.reset();
        }

        public void start() {
            nar.start();
        }

        public void startFPS(int fps) {
            nar.startFPS(fps);
        }

        public void save(Object target) {
            //TODO
        }

        public void load(Object source) {
            //TODO
        }

//        nar.on("concept", (Operator) (op, a, nn) -> {
//            Concept c = nn.concept(a[0]);
//            Command.log(nn,
//                (c != null) ?
//                    quote(c.print(new StringBuilder(1024))) : $.func("unknown", a[0])
//            );
//        });

        public void log(Object x) {
            nar.logger.info(" {}", x);
        }

        //        nar.onOpArgs("error", (t, n) -> NAR.logger.error(" {}", t));

//
//        BiConsumer<Task, NAR> log = (t, n) -> NAR.logger.info(" {}", t);
//        nar.onOp("log", log);
//        nar.onOp(Operator.LOG_FUNCTOR, log);
//


//        nar.on(Functor.f("top", (args) -> {
//
//            String query;
//            if (args.subs() > 0 && args.sub(0) instanceof Atom) {
//                query = $.unquote(args.sub(0)).toLowerCase();
//            } else {
//                query = null;
//
//            }
//
//            int MAX_RESULT_LENGTH = 10;
//            List<Term> rows = $.newArrayList(MAX_RESULT_LENGTH);
//            //TODO use Exe stream() methods
//            nar.conceptsActive().forEach(bc -> {
//                if (rows.size() < MAX_RESULT_LENGTH && (query == null || bc.toString().toLowerCase().contains(query))) {
//                    rows.add($.p(
//                            bc.get().term(),
//                            $.the('$' + Texts.n4(bc.pri())))
//                    );
//                }
//            });
//            return $.p(rows);
//
////            else
////                for (PLink<Concept> bc : ii) {
////                    b.append(bc.get()).append('=').append(Texts.n2(bc.pri())).append("  ");
////                    if (b.length() > MAX_RESULT_LENGTH)
////                        break;
////                }
////            }
//
//            //Command.log(n, b.toString());
//            //"core pri: " + cbag.active.priMin() + "<" + Texts.n4(cbag.active.priHistogram(new double[5])) + ">" + cbag.active.priMax());
//
//        }));


//                Functor.f0("help", () -> {
//                    //TODO generalize with a predicate to filter the concepts, and a lambda for appending each one to an Appendable
//                    StringBuilder sb = new StringBuilder(4096);
//
//                    sb.append("Functions:");
//
//                    nar.forEachConcept(x -> {
//                        if (x instanceof PermanentConcept && !(x instanceof SensorConcept)) {
//                            sb.append(x.toString()).append('\n');
//                        }
//                    });
//                    return $.quote(sb);
//                }),

//                //TODO concept statistics
//                //TODO task statistics
//                //TODO emotion summary
//                Functor.f("save", urlOrPath -> {
//                    try {
//                        File tmp;
//                        if (urlOrPath.length == 0) {
//                            tmp = createTempFile("nar_save_", ".nal").toFile();
//                        } else {
//                            tmp = new File($.unquote(urlOrPath[0]));
//                        }
//                        PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmp), 64 * 1024));
//                        nar.outputTasks((x) -> true, ps);
//                        return quote("Saved: " + tmp.getAbsolutePath()); //TODO include # tasks, and total byte size
//                    } catch (IOException e) {
//                        return quote(e);//e.printStackTrace();
//                    }
//                }),


//        nar.on("nar", (terms) -> {
//            //WARNING this could be dangerous to allow open access
//            Term t = terms[0];
//            if (t.op().var) {
//                Set<Term> pp = new TreeSet();
//                for (Field f : ff) {
//                    if (classWhitelist.contains(f.getType())) {
//                        try {
//                            pp.add(func("nar", the(f.getName()), the(f.get(nar))));
//                        } catch (IllegalAccessException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                return parallel(pp);
//            } else {
//                String expr = unquote(t);
//                Object r;
//                try {
//                    r = Ognl.getValue(expr, nar);
//                } catch (OgnlException e) {
//                    r = e;
//                }
//                if (r instanceof Termed)
//                    return ((Termed) r).term();
//                else
//                    return the(r.toString());
//            }
//        });


    }

//    public final AbstractOperator[] defaultOperators = {
//
//            //system control
//
//            //PauseInput.the,
//            new reset(),
//            //new eval(),
//            //new Wait(),
//
////            new believe(),  // accept a statement with a default truth-value
////            new want(),     // accept a statement with a default desire-value
////            new wonder(),   // find the truth-value of a statement
////            new evaluate(), // find the desire-value of a statement
//            //concept operations for internal perceptions
////            new remind(),   // create/activate a concept
////            new consider(),  // do one inference step on a concept
////            new name(),         // turn a compount term into an atomic term
//            //new Abbreviate(),
//            //new Register(),
//
//            //new echo(),
//
//
//            new doubt(),        // decrease the confidence of a belief
////            new hesitate(),      // decrease the confidence of a goal
//
//            //Meta
//            new reflect(),
//            //new jclass(),
//
//            // feeling operations
//            //new feelHappy(),
//            //new feelBusy(),
//
//
//            // math operations
//            //new length(),
//            //new add(),
//
//            new intToBitSet(),
//
//            //new MathExpression(),
//
//            new complexity(),
//
//            //Term manipulation
//            new flat.flatProduct(),
//            new similaritree(),
//
//            //new NumericCertainty(),
//
//            //io operations
//            new say(),
//
//            new schizo(),     //change Memory's SELF term (default: SELF)
//
//            //new js(), //javascript evalaution
//
//            /*new json.jsonfrom(),
//            new json.jsonto()*/
//         /*
//+         *          I/O operations under consideration
//+         * observe          // get the most active input (Channel ID: optional?)
//+         * anticipate       // get the input matching a given statement with variables (Channel ID: optional?)
//+         * tell             // output a judgment (Channel ID: optional?)
//+         * ask              // output a question/quest (Channel ID: optional?)
//+         * demand           // output a goal (Channel ID: optional?)
//+         */
//
////        new Wait()              // wait for a certain number of clock cycle
//
//
//        /*
//         * -think            // carry out a working cycle
//         * -do               // turn a statement into a goal
//         *
//         * possibility      // return the possibility of a term
//         * doubt            // decrease the confidence of a belief
//         * hesitate         // decrease the confidence of a goal
//         *
//         * feel             // the overall happyness, average solution quality, and predictions
//         * busy             // the overall business
//         *
//
//
//         * do               // to turn a judgment into a goal (production rule) ??
//
//         *
//         * count            // count the number of elements in a set
//         * arithmatic       // + - * /
//         * comparisons      // < = >
//         * logic        // binary logic
//         *
//
//
//
//         * -assume           // local assumption ???
//         *
//         * observe          // get the most active input (Channel ID: optional?)
//         * anticipate       // get input of a certain pattern (Channel ID: optional?)
//         * tell             // output a judgment (Channel ID: optional?)
//         * ask              // output a question/quest (Channel ID: optional?)
//         * demand           // output a goal (Channel ID: optional?)
//
//
//        * name             // turn a compount term into an atomic term ???
//         * -???              // rememberAction the history of the system? excutions of operatons?
//         */
//    };
//
//

}

//    Builtin	Operations
//    http://jena.apache.org/documentation/inference/index.html#rules
//    http://jena.apache.org/documentation/javadoc/jena/org/apache/jena/reasoner/rulesys/Builtin.html
//    isLiteral(?x) notLiteral(?x)
//        isFunctor(?x) notFunctor(?x)
//        isBNode(?x) notBNode(?x)
//
//        Test whether the single argument is or is not a literal, a functor-valued literal or a blank-node, respectively.
//        bound(?x...) unbound(?x..)
//        Test if all of the arguments are bound (not bound) variables
//        equal(?x,?y) notEqual(?x,?y)
//        Test if x=y (or x != y). The equality test is semantic equality so that, for example, the xsd:int 1 and the xsd:decimal 1 would test equal.
//        lessThan(?x, ?y), greaterThan(?x, ?y)
//        le(?x, ?y), ge(?x, ?y)
//
//        Test if x is <, >, <= or >= y. Only passes if both x and y are numbers or time instants (can be integer or floating point or XSDDateTime).
//        sum(?a, ?b, ?c)
//        addOne(?a, ?c)
//        difference(?a, ?b, ?c)
//        min(?a, ?b, ?c)
//        max(?a, ?b, ?c)
//        product(?a, ?b, ?c)
//        quotient(?a, ?b, ?c)
//
//        Sets c to be (a+b), (a+1) (a-b), min(a,b), max(a,b), (ab), (a/b). Note that these do not run backwards, if in sum a and c are bound and b is unbound then the test will fail rather than bind b to (c-a). This could be fixed.
//        strConcat(?a1, .. ?an, ?t)
//        uriConcat(?a1, .. ?an, ?t)
//
//        Concatenates the lexical form of all the arguments except the last, then binds the last argument to a plain literal (strConcat) or a URI node (uriConcat) with that lexical form. In both cases if an argument node is a URI node the URI will be used as the lexical form.
//        regex(?t, ?p)
//        regex(?t, ?p, ?m1, .. ?mn)
//
//        Matches the lexical form of a literal (?t) against a regular expression pattern given by another literal (?p). If the match succeeds, and if there are any additional arguments then it will bind the first n capture groups to the arguments ?m1 to ?mn. The regular expression pattern syntax is that provided by java.util.regex. Note that the capture groups are numbered from 1 and the first capture group will be bound to ?m1, we ignore the implicit capture group 0 which corresponds to the entire matched string. So for example
//        regexp('foo bar', '(.) (.)', ?m1, ?m2)
//        will bind m1 to "foo" and m2 to "bar".
//        now(?x)
//        Binds ?x to an xsd:dateTime value corresponding to the current time.
//        makeTemp(?x)
//        Binds ?x to a newly created blank node.
//        makeInstance(?x, ?p, ?v)
//        makeInstance(?x, ?p, ?t, ?v)
//        Binds ?v to be a blank node which is asserted as the value of the ?p property on resource ?x and optionally has type ?t. Multiple calls with the same arguments will return the same blank node each time - thus allowing this call to be used in backward rules.
//        makeSkolem(?x, ?v1, ... ?vn)
//        Binds ?x to be a blank node. The blank node is generated based on the values of the remain ?vi arguments, so the same combination of arguments will generate the same bNode.
//        noValue(?x, ?p)
//        noValue(?x ?p ?v)
//        True if there is no known triple (x, p, ) or (x, p, v) in the model or the explicit forward deductions so far.
//        remove(n, ...)
//        drop(n, ...)
//        Remove the statement (triple) which caused the n'th body term of this (forward-only) rule to match. Remove will propagate the change to other consequent rules including the firing rule (which must thus be guarded by some other clauses). Drop will silently remove the triple(s) from the graph but not fire any rules as a consequence. These are clearly non-monotonic operations and, in particular, the behaviour of a rule set in which different rules both drop and create the same triple(s) is undefined.
//        isDType(?l, ?t) notDType(?l, ?t)
//        Tests if literal ?l is (or is not) an instance of the datatype defined by resource ?t.
//        print(?x, ...)
//        Print (to standard out) a representation of each argument. This is useful for debugging rather than serious IO work.
//        listContains(?l, ?x)
//        listNotContains(?l, ?x)
//        Passes if ?l is a list which contains (does not contain) the element ?x, both arguments must be ground, can not be used as a generator.
//        listEntry(?list, ?index, ?val)
//        Binds ?val to the ?index'th entry in the RDF list ?list. If there is no such entry the variable will be unbound and the call will fail. Only usable in rule bodies.
//        listLength(?l, ?len)
//        Binds ?len to the length of the list ?l.
//        listEqual(?la, ?lb)
//        listNotEqual(?la, ?lb)
//        listEqual tests if the two arguments are both lists and contain the same elements. The equality test is semantic equality on literals (sameValueAs) but will not take into account owl:sameAs aliases. listNotEqual is the negation of this (passes if listEqual fails).
//        listMapAsObject(?s, ?p ?l)
//        listMapAsSubject(?l, ?p, ?o)
//        These can only be used as actions in the head of a rule. They deduce a set of triples derived from the list argument ?l : listMapAsObject asserts triples (?s ?p ?x) for each ?x in the list ?l, listMapAsSubject asserts triples (?x ?p ?o).
//        table(?p) tableAll()
//        Declare that all goals involving property ?p (or all goals) should be tabled by the backward engine.
//        hide(p)
//        Declares that statements involving the predicate p should be hidden. Queries to the model will not report such statements. This is useful to enable non-monotonic forward rules to define flag predicates which are only used for inference control and do not "pollute" the inference results.