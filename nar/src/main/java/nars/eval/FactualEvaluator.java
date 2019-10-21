package nars.eval;

import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.unify.Unify;
import nars.unify.UnifyAny;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Op.CONJ;
import static nars.Op.IMPL;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

public class FactualEvaluator extends Evaluator {

    private final Function<Term, Stream<Term>> factResolver;
//    public final Map<Term, Node> nodes = new ConcurrentHashMap();

    @Deprecated
    public static FactualEvaluator query(String s, NAR n) throws Narsese.NarseseException {
        return query($.$(s), n);
    }

    @Deprecated
    private static FactualEvaluator query(Term x, NAR n) {
        FactualEvaluator f = new FactualEvaluator(n::axioms, n.facts(0.75f, true));
        f.eval(new Predicate<Term>() {
            @Override
            public boolean test(Term y) {
                return true;
            }
        }, true, false, x);
        return f;
    }

    public static Set<Term> queryAll(Term x, NAR n) {
        Set<Term> solutions = new ArrayHashSet(1);
        FactualEvaluator f = new FactualEvaluator(n::axioms, n.facts(0.75f, true));
        f.eval(new Predicate<Term>() {
            @Override
            public boolean test(Term y) {
                solutions.add(y);
                return true;
            }
        }, true, false, x);
        return solutions;
    }

    enum ProofTruth {
        True, False, Unknown, Conflict, Choose
    }

//    ProofTruth truth(Term x) {
//
//        if (x == Bool.True)
//            return ProofTruth.True;
//        else if (x == Bool.False)
//            return ProofTruth.False;
//        else if (x == Bool.Null)
//            return ProofTruth.Conflict;
//
//        Node f = nodes.get(x);
//        ProofTruth t = f == null ? Unknown : f.truth();
////        if (t == ProofTruth.Unknown) {
////            query(x, null);
////
////            //try again HACK
////            Node f2 = nodes.get(x);
////            return f2 == null ? ProofTruth.Unknown : f2.truth();
////        }
//        return t;
//    }

//    /**
//     * proof node
//     */
//    abstract static class Node {
//        protected MutableSet<Term> conds = null;
//
//        @Deprecated
//        abstract public ProofTruth truth();
//
//        public void print() {
//            if (conds != null) {
//                conds.forEach(c -> {
//                    if (!(c instanceof Bool)) {
//                        System.out.println("\t" + c);
//                    }
//                });
//            }
//        }
//
//
//        public boolean add(Term t) {
//            if (conds == null)
//                conds = new UnifiedSet(1);
//            return conds.add(t);
//        }
//    }
//
//    class ChooseNode extends Node {
//
//
//        @Override
//        public ProofTruth truth() {
//            switch (conds.size()) {
//                case 0:
//                    return ProofTruth.True;
//                case 1:
//                    return FactualEvaluator.this.truth(conds.getOnly());
//                default:
//                    return Choose;
//            }
////            if (changed) {
////                for (Term t : conds)
////                    FactualEvaluator.this.truth(t); //recurse
////                changed = false;
////            }
////            return Choose;
//        }
//    }
//
//    class FactNode extends Node {
//        private ProofTruth truth = null;
//
//        @Override
//        public boolean add(Term t) {
//            if (super.add(t)) {
//                truth = null; //invalidate
//                return true;
//            }
//            return false;
//        }
//
//        @Override
//        public String toString() {
//            return (truth == null ? Unknown : truth) + "\t" +
//                    (conds == null ? "" : conds.toString());
//        }
//
//
//        @Override
//        public ProofTruth truth() {
//            if (truth != null)
//                return truth;
//
//            return truth = _truth();
//        }
//
//        private ProofTruth _truth() {
//            if (conds == null)
//                return Unknown;
//
//            MutableSet<Term> cond = conds;
//            if (cond.size() == 1) {
//                Term ff = cond.getOnly();
//                return FactualEvaluator.this.truth(ff);
//
//            } else if (cond.size() > 1) {
//                boolean trues = false, falses = false;
//                for (Term cc : cond) {
//                    ProofTruth y = FactualEvaluator.this.truth(cc);
//                    if (y == ProofTruth.True)
//                        trues = true;
//                    else if (y == ProofTruth.False)
//                        falses = true;
//                    if (trues && falses)
//                        return ProofTruth.Conflict;
//                }
//                if (trues && !falses)
//                    return ProofTruth.True;
//                else if (falses && !trues)
//                    return ProofTruth.False;
//            }
//
//            return Unknown;
//        }
//
//
//        public ProofTruth getTruth() {
//            return truth;
//        }
//    }


//    /** head |- OR(tail1, tail2, ...) */
//    private final SetMultimap<Term, Term> ifs = MultimapBuilder.hashKeys().linkedHashSetValues().build();


    private FactualEvaluator(Function<Atom, Functor> resolver, Function<Term, Stream<Term>> facts) {
        super(resolver);
        this.factResolver = facts;
    }

    public FactualEvaluator clone() {
        return new FactualEvaluator(funcResolver, factResolver);
    }

    @Override
    public @Nullable ArrayHashSet<Term> clauses(Compound x, Evaluation e) {


        /** actually should be a set but duplicates should not normally be encountered */


        //TODO cache
        if (x.op() == CONJ && (x.dt() == DTERNAL || x.dt() == XTERNAL)) {
            int n = x.subterms().subs();
            List<List<Term>> matches = new FasterList<>(n);
            for (Term xx : x.subterms()) {
                List<Term> m = resolve(xx);
                if (m.isEmpty())
                    m = Collections.EMPTY_LIST;
                //TODO trim list?
                matches.add(m);
            }
            for (int i = 0, matchesSize = matches.size(); i < matchesSize; i++) {
                if (!matches.get(i).isEmpty())
                    e.canBe(x.sub(i), matches.get(i));
            }
        } else {
            List<Term> m = resolve(x);
            if (!m.isEmpty()) {
                e.canBe(x, m);
            }
        }


        return super.clauses(x, e);
    }

    private List<Term> resolve(Term x) {
        List<Term> matches = new FasterList(1);
        resolve(x, matches);
        return matches;
    }

    private void resolve(Term x, List<Term> matches) {
        /*List<Predicate<VersionMap<Term, Term>>> l = */
        Unify u = new UnifyAny();
        u.commonVariables = false;
        //TODO neg, temporal
        factResolver.apply(x.normalize()).forEach(new Consumer<Term>() {
            @Override
            public void accept(Term y1) {
                boolean neg = y1 instanceof Neg;
                if (neg) y1 = y1.unneg();
                if (y1.op() == IMPL) {


                    Term pre = y1.sub(1);
                    if (pre.unify(x, u.clear())) {

                        //ifs.get(k).forEach(v->{
                        Term z = u.apply(y1.sub(0));
                        if (z.op().conceptualizable) {
//                                //facts.addAt($.func("ifThen", vv, k));
//                                //facts.put(vv, True);

//
//                                nodeOrAdd(x).add(z);
//
                            matches.add(z.negIf(neg));

                        }
//                                });
                    }

                } else {

//                        Node nx = nodeOrAdd(x);
//                        if (y.equals(x)) {
//                            nx.add(Bool.True); //if x==y add True ?
//                        } else if (y.equalsNeg(x)) {
//                            nx.add(Bool.False);
//                        } else {
//                            nx.add(y);
//                        }
                    if (x.unify(y1, u.clear())) {
                        Term z = u.apply(x);
                        if (z.op().conceptualizable) {
                            if (!z.equals(x)) {
                                matches.add(z.negIf(neg));
                            }

                        }
                    }
                }
            }
        });
    }


//    private FactualEvaluator.Node nodeOrAdd(Term x) {
//        return nodes.computeIfAbsent(x, z -> {
//            if (z.hasVars())
//                return new ChooseNode();
//            else
//                return new FactNode();
//        });
//    }
//
//
//    /**
//     * @return +1 true, 0 - unknown, -1 false
//     */
//    public ProofTruth truth(Term x, Evaluation e) {
//        if (x instanceof Bool)
//            return ProofTruth.Conflict;
//
//
//        Node node = nodes.get(x);
//        if (node instanceof ChooseNode) {
//            ChooseNode c = (ChooseNode) node;
//            switch (c.conds.size()) {
//                case 0:
//                    return ProofTruth.True;
//                case 1:
//                    return truth(c.conds.getOnly(), e);
//                default: {
//                    e.canBe(
//                            c.conds.stream().map(z -> Evaluation.assign(x, z)).collect(toList())
//                    );
//                    return Choose;
//                }
//            }
//        } else if (node instanceof FactNode) {
//            FactNode c = (FactNode) node;
//            switch (c.conds.size()) {
//                case 0:
//                    return ProofTruth.True;
//                case 1:
//                    return truth(c.conds.getOnly(), e);
//                default: {
//                    throw new TODO();
////                    if (c.conds.anySatisf
////                            c.conds.stream().map(z -> Evaluation.assign(x,z )).collect(toList())
////                    );
////                    return Choose;
//                }
//            }
//        } else if (node != null)
//            return node.truth();
//        else {
//            //e.eval(this, x);
//            //return truth(, e);
//            return Unknown;
//        }
//    }

//
////        UnifySubst u = new UnifySubst(null, new XoRoShiRo128PlusRandom(1) /* HACK */, (t)->{ return false; /* first is ok */ } );
////
////        for (Term k : ifs.keys()) {
////            if (k.unify(x, u.clear())) {
////                ifs.get(k).forEach(v->{
////                    Term vv = u.transform(v);
////                    if (vv.op().conceptualizable) {
////                        //facts.addAt($.func("ifThen", vv, k));
////                        facts.put(k, vv);
////                    }
////                });
////            }
////        }
//
////
////        if (x.op()==CONJ) {
////            //evaluate
////        }
////        return ProofTruth.Unknown;
//    }

//    @Override
//    public void print() {
//        super.print();
//        nodes.forEach((f, p) -> {
//            System.out.println(f + " = " + p.truth());
//            p.print();
//        });
//        System.out.println();
//
////        System.out.println("ifs=" + ifs);
//    }

}
