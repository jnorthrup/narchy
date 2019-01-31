package nars.eval;

import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.unify.UnifyAny;
import nars.unify.UnifySubst;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static nars.Op.IMPL;
import static nars.eval.FactualEvaluator.ProofTruth.Choose;
import static nars.eval.FactualEvaluator.ProofTruth.Unknown;

public class FactualEvaluator extends Evaluator {

    private final Function<Term, Stream<Term>> factResolver;
    public final Map<Term, Node> nodes = new ConcurrentHashMap();

    enum ProofTruth {
        True, False, Unknown, Conflict, Choose
    }

    ProofTruth truth(Term x) {

        if (x == Bool.True)
            return ProofTruth.True;
        else if (x == Bool.False)
            return ProofTruth.False;
        else if (x == Bool.Null)
            return ProofTruth.Conflict;

        Node f = nodes.get(x);
        ProofTruth t = f == null ? Unknown : f.truth();
//        if (t == ProofTruth.Unknown) {
//            query(x, null);
//
//            //try again HACK
//            Node f2 = nodes.get(x);
//            return f2 == null ? ProofTruth.Unknown : f2.truth();
//        }
        return t;
    }

    /**
     * proof node
     */
    abstract static class Node {
        protected MutableSet<Term> conds = null;

        @Deprecated
        abstract public ProofTruth truth();

        public void print() {
            if (conds != null) {
                conds.forEach(c -> {
                    if (!(c instanceof Bool)) {
                        System.out.println("\t" + c);
                    }
                });
            }
        }


        public boolean add(Term t) {
            if (conds == null)
                conds = new UnifiedSet(1);
            return conds.add(t);
        }
    }

    class ChooseNode extends Node {


        @Override
        public ProofTruth truth() {
            switch (conds.size()) {
                case 0:
                    return ProofTruth.True;
                case 1:
                    return FactualEvaluator.this.truth(conds.getOnly());
                default:
                    return Choose;
            }
//            if (changed) {
//                for (Term t : conds)
//                    FactualEvaluator.this.truth(t); //recurse
//                changed = false;
//            }
//            return Choose;
        }
    }

    class FactNode extends Node {
        private ProofTruth truth = null;

        @Override
        public boolean add(Term t) {
            if (super.add(t)) {
                truth = null; //invalidate
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return (truth == null ? Unknown : truth) + "\t" +
                    (conds == null ? "" : conds.toString());
        }


        @Override
        public ProofTruth truth() {
            if (truth != null)
                return truth;

            return truth = _truth();
        }

        private ProofTruth _truth() {
            if (conds == null)
                return Unknown;

            MutableSet<Term> cond = conds;
            if (cond.size() == 1) {
                Term ff = cond.getOnly();
                return FactualEvaluator.this.truth(ff);

            } else if (cond.size() > 1) {
                boolean trues = false, falses = false;
                for (Term cc : cond) {
                    ProofTruth y = FactualEvaluator.this.truth(cc);
                    if (y == ProofTruth.True)
                        trues = true;
                    else if (y == ProofTruth.False)
                        falses = true;
                    if (trues && falses)
                        return ProofTruth.Conflict;
                }
                if (trues && !falses)
                    return ProofTruth.True;
                else if (falses && !trues)
                    return ProofTruth.False;
            }

            return Unknown;
        }


        public ProofTruth getTruth() {
            return truth;
        }
    }


//    /** head |- OR(tail1, tail2, ...) */
//    private final SetMultimap<Term, Term> ifs = MultimapBuilder.hashKeys().linkedHashSetValues().build();


    protected FactualEvaluator(Function<Atom, Functor> resolver, Function<Term, Stream<Term>> facts) {
        super(resolver);
        this.factResolver = facts;
    }

    public FactualEvaluator clone() {
        return new FactualEvaluator(funcResolver, factResolver);
    }

    @Override
    public @Nullable ArrayHashSet<Term> discover(Term x, Evaluation e) {


        /** actually should be a set but duplicates should not normally be encountered */
        List<Term> matches = new FasterList(1);

        //TODO cache
        /*List<Predicate<VersionMap<Term, Term>>> l = */
        factResolver.apply(x).
                //map(y -> {
                        forEach(y -> {


                    //TODO neg, temporal
                    if (y.op() == IMPL) {

                        UnifySubst u = new UnifyAny(new XoRoShiRo128PlusRandom(1) /* HACK */);

                        Term head = y.sub(1); //predicate, headicate
                        if (head.unify(x, u.clear())) {
                            //ifs.get(k).forEach(v->{
                            Term vv = u.transform(y.sub(0));
                            if (vv.op().conceptualizable) {
                                //facts.addAt($.func("ifThen", vv, k));
                                //facts.put(vv, True);
                                nodeOrAdd(x).add(vv);
                                matches.add(vv);

                            }
//                                });
                        }

                    } else {

                        Node nx = nodeOrAdd(x);
                        if (y.equals(x)) {
                            nx.add(Bool.True); //if x==y addAt True ?
                        } else if (y.equalsNeg(x)) {
                            nx.add(Bool.False);
                        } else {
                            nx.add(y);
                        }
                        if (!y.equals(x)) {
                            matches.add(y);
                        }


                    }
                });

        if (!matches.isEmpty()) {
            e.canBe(x, matches);
        }
        return super.discover(x, e);
    }


    private FactualEvaluator.Node nodeOrAdd(Term x) {
        return nodes.computeIfAbsent(x, z -> {
            if (z.hasVars())
                return new ChooseNode();
            else
                return new FactNode();
        });
    }


    /**
     * @return +1 true, 0 - unknown, -1 false
     */
    public ProofTruth truth(Term x, Evaluation e) {
        if (x instanceof Bool)
            return ProofTruth.Conflict;


        Node node = nodes.get(x);
        if (node instanceof ChooseNode) {
            ChooseNode c = (ChooseNode) node;
            switch (c.conds.size()) {
                case 0:
                    return ProofTruth.True;
                case 1:
                    return truth(c.conds.getOnly(), e);
                default: {
                    e.canBe(
                            c.conds.stream().map(z -> Evaluation.assign(x, z)).collect(toList())
                    );
                    return Choose;
                }
            }
        } else if (node instanceof FactNode) {
            FactNode c = (FactNode) node;
            switch (c.conds.size()) {
                case 0:
                    return ProofTruth.True;
                case 1:
                    return truth(c.conds.getOnly(), e);
                default: {
                    throw new TODO();
//                    if (c.conds.anySatisf
//                            c.conds.stream().map(z -> Evaluation.assign(x,z )).collect(toList())
//                    );
//                    return Choose;
                }
            }
        } else if (node != null)
            return node.truth();
        else {
            //e.eval(this, x);
            //return truth(, e);
            return Unknown;
        }
    }
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

    @Override
    public void print() {
        super.print();
        nodes.forEach((f, p) -> {
            System.out.println(f + " = " + p.truth());
            p.print();
        });
        System.out.println();

//        System.out.println("ifs=" + ifs);
    }

}
