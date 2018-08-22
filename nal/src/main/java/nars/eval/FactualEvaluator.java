package nars.eval;

import jcog.random.XoRoShiRo128PlusRandom;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.unify.UnifySubst;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.eval.FactualEvaluator.ProofTruth.Choose;

public class FactualEvaluator extends Evaluator {

    private final Function<Term, Stream<Term>> factResolver;
    public final Map<Term, Node> nodes = new ConcurrentHashMap();

    enum ProofTruth {
        True, False, Unknown, Confusion, Choose
    }

    ProofTruth truth(Term x) {

        if (x == True)
            return ProofTruth.True;
        else if (x == False)
            return ProofTruth.False;
        else if (x == Null)
            return ProofTruth.Confusion;

        Node f = nodes.get(x);
        ProofTruth t = f == null ? ProofTruth.Unknown : f.truth();
        if (t == ProofTruth.Unknown) {
            query(x, null);

            //try again HACK
            Node f2 = nodes.get(x);
            return f2 == null ? ProofTruth.Unknown : f2.truth();
        }
        return t;
    }

    /** proof node */
    abstract class Node {
        protected MutableSet<Term> conds = null;

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
        boolean changed = false;
        @Override
        public boolean add(Term t) {
            if (super.add(t)) {
                changed = true;
                return true;
            }
            return false;
        }

        @Override
        public ProofTruth truth() {
            if (changed) {
                for (Term t : conds)
                    FactualEvaluator.this.truth(t); //recurse
                changed = false;
            }
            return Choose;
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
            return (truth==null ? ProofTruth.Unknown : truth) + "\t" +
                                (conds ==null ? "" : conds.toString());
        }


        @Override public ProofTruth truth() {
            if (truth!=null)
                return truth;

            return truth = _truth();
        }

        private ProofTruth _truth() {
            if (conds == null)
                return ProofTruth.Unknown;

            MutableSet<Term> cond = conds;
            if (cond.size()==1) {
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
                        return ProofTruth.Confusion;
                }
                if (trues && !falses)
                    return ProofTruth.True;
                else if (falses && !trues)
                    return ProofTruth.False;
            }

            return ProofTruth.Unknown;
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
    protected void discover(Term x, Evaluation e) {

        //TODO cache
        /*List<Predicate<VersionMap<Term, Term>>> l = */factResolver.apply(x).
                //map(y -> {
                forEach(y -> {
                    //TODO neg, temporal
                    if (y.op()==IMPL) {

                        UnifySubst u = new UnifySubst(null, new XoRoShiRo128PlusRandom(1) /* HACK */, (t)->{ return false; /* first is ok */ } );
                        u.symmetric = true;

                        Term head = y.sub(1); //predicate, headicate
                        if (head.unify(x, u.clear())) {
                            //ifs.get(k).forEach(v->{
                                Term vv = u.transform(y.sub(0));
                                if (vv.op().conceptualizable) {
                                    //facts.add($.func("ifThen", vv, k));
                                    //facts.put(vv, True);
                                    nodeOrAdd(x).add(vv);
                                }
//                                });
                        }
//                            ifs.put(y.sub(1), y.sub(0));
                        //return null;
                    } else {

                        Term yu = y.unneg();
                        if (!x.unneg().equals(yu)) {
                            nodeOrAdd(x).add(y);
                        }
                        if (!yu.hasVars()) //ground truth
                            nodeOrAdd(yu).add(y.op() == NEG ? False : True);


                        //return Evaluation.assign(x, y);
                    }
                });
        //.filter(Objects::nonNull).collect(toList());

//            if (e!=null && !l.isEmpty())
//                e.isAny(l);

        super.discover(x, e);

        truth(x);

    }

    @NotNull
    private FactualEvaluator.Node nodeOrAdd(Term x) {
        return nodes.computeIfAbsent(x, z->{
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
            return ProofTruth.Confusion;

        query(x, e);

        return nodes.get(x).truth();


//        UnifySubst u = new UnifySubst(null, new XoRoShiRo128PlusRandom(1) /* HACK */, (t)->{ return false; /* first is ok */ } );
//
//        for (Term k : ifs.keys()) {
//            if (k.unify(x, u.clear())) {
//                ifs.get(k).forEach(v->{
//                    Term vv = u.transform(v);
//                    if (vv.op().conceptualizable) {
//                        //facts.add($.func("ifThen", vv, k));
//                        facts.put(k, vv);
//                    }
//                });
//            }
//        }

//
//        if (x.op()==CONJ) {
//            //evaluate
//        }
//        return ProofTruth.Unknown;
    }

    @Override public void print() {
        super.print();
        nodes.forEach((f, p) -> {
            System.out.println(f + " = " + p.truth());
            p.print();
        });
        System.out.println();

//        System.out.println("ifs=" + ifs);
    }

}
