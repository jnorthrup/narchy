package nars.derive.constraint;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.primitives.Floats;
import jcog.TODO;
import jcog.Util;
import jcog.list.FasterList;
import nars.$;
import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.AndCondition;
import nars.derive.PrediTerm;
import nars.term.Term;
import nars.term.subst.Unify;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;


public abstract class MatchConstraint extends AbstractPred<Derivation> {

    public final Term target;

    protected MatchConstraint(Term target, String func, Term... args) {
        super($.func("unifyIf", target, $.func(func, args)));
        this.target = target;
    }

    public static PrediTerm<Derivation> combineConstraints(AndCondition a) {
        RoaringBitmap constraints = new RoaringBitmap();
        @NotNull PrediTerm[] cond1 = a.cond;
        for (int i = 0, cl = cond1.length; i < cl; i++) {
            Term x = cond1[i];
            if (x instanceof MatchConstraint) {
                constraints.add(i);
            }
        }
        if (constraints.getCardinality() < 2) {
            return a;
        } else {
            //identify contiguous runs of constraints
            List<IntIntPair> ranges = new FasterList<>(1); //inclusive
            int start = -1, end = -1;
            PeekableIntIterator ii = constraints.getIntIterator();
            while (ii.hasNext()) {
                int next = ii.next();
                if (start == -1) {
                    start = end = next;
                } else {
                    if (next == end+1) {
                        end++;
                    } else {
                        if (end - start >= 1) {
                            //compile that range
                            ranges.add(pair(start, end));
                        }
                        start = -1; //broken
                    }
                }
            }
            if (end-start >= 1)
                ranges.add(pair(start, end));

            if (ranges.size() > 1) throw new TODO();
            IntIntPair rr = ranges.get(0);


            List<PrediTerm<Derivation>> l = new FasterList();
            int i;
            for (i = 0; i < start; i++) {
                l.add(a.cond[i]);
            }

            CompoundConstraint.the(
                    Util.map(MatchConstraint.class::cast, MatchConstraint[]::new, ArrayUtils.subarray(a.cond, rr.getOne(), rr.getTwo()+1))
            ).forEach(l::add);

            i = end+1;
            for ( ; i < a.cond.length; i++) {
                l.add(a.cond[i]);
            }
            return AndCondition.the((List)l);
        }
    }

//    /**
//     * combine certain types of items in an AND expression
//     */
//    public static List<PrediTerm<Derivation>> combineConstraints(List<PrediTerm<Derivation>> p) {
//        if (p.size() == 1)
//            return p;
//
//        SortedSet<MatchConstraint> constraints = new TreeSet<>((a, b)->Float.compare(a.cost(), b.cost()));
//        Iterator<PrediTerm<Derivation>> il = p.iterator();
//        while (il.hasNext()) {
//            PrediTerm c = il.next();
//            if (c instanceof MatchConstraint) {
//                constraints.add((MatchConstraint) c);
//                il.remove();
//            }
//        }
//
//
//        if (!constraints.isEmpty()) {
//
//
//            int iMatchTerm = -1; //first index of a MatchTerm op, if any
//            for (int j = 0, cccSize = p.size(); j < cccSize; j++) {
//                PrediTerm c = p.get(j);
//                if ((c instanceof Fork) && iMatchTerm == -1) {
//                    iMatchTerm = j;
//                }
//            }
//            if (iMatchTerm == -1)
//                iMatchTerm = p.size();
//
//            //1. sort the constraints and add them at the end
//            int c = constraints.size();
//            if (c > 1) {
//                p.add(iMatchTerm, new CompoundConstraint(constraints.toArray(new MatchConstraint[c])));
//            } else
//                p.add(iMatchTerm, constraints.iterator().next()); //just add the singleton at the end
//        }
//
//        return p;
//    }

    /**
     * cost of testing this, for sorting. higher value will be tested later than lower
     */
    @Override
    abstract public float cost();

    @Override
    public boolean test(Derivation p) {
        //this will not be called when it is part of a CompoundConstraint group
        return p.constrain(this);
    }


    static final class CompoundConstraint extends AbstractPred<Derivation> {


        private final MatchConstraint[] cache;
        final Term target;

        /** groups the constraints into their respective targets */
        public static Iterable<PrediTerm<Derivation>> the(MatchConstraint[] c) {
            assert(c.length > 1);
            ListMultimap<Term, MatchConstraint> m = MultimapBuilder.hashKeys().arrayListValues().build();
            for (MatchConstraint x : c) {
                m.put(x.target, x);
            }
            return ()->m.asMap().entrySet().stream().map(e -> {
                Collection<MatchConstraint> cc = e.getValue();
                MatchConstraint[] d = cc.toArray(new MatchConstraint[cc.size()]);
                assert(d.length > 0);
                if (d.length == 1) {
                    return (PrediTerm<Derivation>)d[0];
                } else {
                    Arrays.sort(d, PrediTerm.sortByCost);
                    return new CompoundConstraint(d);
                }
            }).iterator();


        }

        private CompoundConstraint(MatchConstraint[] c) {
            super($.func("unifyIf", c[0].target, $.sete(c)));
            this.cache = c;
            this.target = c[0].target;
            for (int i = 1; i < c.length; i++) {
                if (!c[i].target.equals(target))
                    throw new RuntimeException();
            }
        }

        @Override
        public float cost() {
            return Util.sum(MatchConstraint::cost, cache);
        }

        @Override
        public boolean test(Derivation derivation) {
            return derivation.constrain(target, cache);
        }
    }

    /**
     * @param targetVariable current value of the target variable (null if none is set)
     * @param potentialValue potential value to assign to the target variable
     * @param f              match context
     * @return true if match is INVALID, false if VALID (reversed)
     */
    abstract public boolean invalid(Term y, Unify f);
}
