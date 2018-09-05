package nars.op;

import jcog.Paper;
import jcog.memoize.Memo;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.time.Tense;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.multimap.set.UnifiedSetMultimap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Function;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * trivial case:
 * (f(a) && f(b))   |-   (f(#1) && member(#1,{a,b}))
 * trivial induction case:
 * ((f(#1) && member(#1,{a,b})) && f(c))
 * (&&, f(c), f(#1),  member(#1,{a,b}))
 * (&&, f(#2), member(#2, {#1,c}),  member(#1,{a,b}))   |-   (f(#1) && member(#1,{a,b,c}))
 * <p>
 * optimizations:
 * compute as a byte trie, where each shadow is represented as a byte[] only
 * commutive subterms handled in a special way, pulling the target subterm's variable replacement to the 0th index regardless where it was originally
 * the remaining terms are sorted in their natural order
 * final term does not need to ompute shadows except for paths which already exist; so order largest term last? (especially if only 2)
 *
 * TODO
 * allow modification of inner conjunctions, not only at top level
 */
@Paper
public class Factorize {
    public static Term apply(Term x) {
        if (x.op() == NEG) {
            Term xu = x.unneg();
            if (xu.op() == CONJ) {
                Term y = apply(xu);
                if (y != xu)
                    return y.neg();
            }
            return x;
        }

        if (x.op() != CONJ || !Tense.dtSpecial(x.dt()))
            return x; //unchanged

        return factorize.apply(x);
    }

    static final Function<Term,Term> factorize = Memo.the.memoize(Factorize::_factorize);

    private static Term _factorize(Term x) {
        Subterms xx = x.subterms();

        Term[] xxx = distribute(xx);
        if (xxx == null)
            return x; //not worth changing

        @Nullable Set<Term> y = applyConj(xxx, x.hasVarDep() ? f : fIfNoDep);
        if (y == null)
            return x; //unchanged

//        Term[] yy = Terms.sorted(y);
//        if (xx.equalTerms(yy))
//            return x; //unchanged

        return CONJ.the(x.dt(), y);
    }

    /** returns null if detects no reason to re-process */
    @Nullable private static Term[] distribute(Subterms xx) {
        TermList x = xx.toList();

        //TODO track what subterms (if any) are totally un-involved and exclude them from processing in subsequent stages
        //TODO sort the subterms for optimal processing order
        boolean stable;
        restart: do {
            stable = true;

            for (int i = 0, xLength = x.size(); i < xLength; i++) {
                Term s = x.get(i);
                Term var;

                //TODO optimization step: distribute any equal(#x,constant) terms
//                if (Functor.func(s).equals(Equal.the)) {
//
//                }

                if (Functor.func(s).equals(Member.the) && (var = s.subPath((byte) 0, (byte) 0)) instanceof Variable) {
                    Term r = s.subPath((byte) 0, (byte) 1);
                    if (r.op().isSet()) {
                        if (xLength == 2) {
                            //special case: there is no reason to process this because it consists of one member and one non-member
                            return null;
                        }

                        Subterms rs = r.subterms();

                        //erase even if un-used, it would have no effect
                        x.remove(i);

                        ListIterator<Term> jj = x.listIterator();
                        while (jj.hasNext()) {
                            Term xj = jj.next();
                            if (xj.containsRecursively(var)) {
                                jj.remove();
                                for (Term rr : rs) {
                                    jj.add(xj.replace(var, rr));
                                }
                            }
                        }


                        continue restart;
                    }
                }
            }
        } while (!stable);



        return x.arrayKeep();
    }

    private final static Variable f = $.varDep("_f");
    private final static Variable fIfNoDep = $.varDep(1);

    //TODO static class ShadowCompound extends Compound(Compound base, ByteList masked)

    static Term shadow(Term base, ByteList path, Variable f) {
        return base.replaceAt(path, f);
    }

    /**
     * returns the subterms, as a set, for the new conjunction.  or null if there was nothing factorable
     */
    @Nullable
    protected static Set<Term> applyConj(Term[] x, Variable f) {

        /** shadow term -> replacements */
        UnifiedSetMultimap<Term, ObjectIntPair<Term>> p = UnifiedSetMultimap.newMultimap();
        byte n = (byte) x.length;
        for (int i = 0; i < n; i++) {
            Term s = x[i];
            int pathMin = s.subs() == 1 ? 2 : 1; //dont factor the direct subterm of 1-arity compound (ex: negation, {x}, (x) )
            if (s instanceof Compound) {
                int ii = i;
                s.pathsTo((Term v) -> true, v -> true, (path, what) -> {
                    if (what instanceof Variable)
                        return true; //dont remap variables
                    if (path.size() >= pathMin) {
                        p.put(shadow(s, path, f), pair(what, ii));
                    }
                    return true;
                });
            }
        }
        MutableList<Pair<Term, RichIterable<ObjectIntPair<Term>>>> r = p.keyMultiValuePairsView().select((pathwhat) -> pathwhat.getTwo().size() > 1).toSortedList(
                Comparator
                        .comparingInt((Pair<Term, RichIterable<ObjectIntPair<Term>>> pp) -> -pp.getTwo().size()) //more unique subterms involved
                        .thenComparingInt((Pair<Term, RichIterable<ObjectIntPair<Term>>> pp) -> -pp.getOne().volume()) //longest common path
                        .thenComparingInt((Pair<Term, RichIterable<ObjectIntPair<Term>>> pp) -> -(int) (pp.getTwo().sumOfInt(z -> z.getOne().volume()))) //larger subterms involved
        );

        if (r.isEmpty())
            return null;


        Pair<Term, RichIterable<ObjectIntPair<Term>>> rr = r.get(0);
        ByteSet masked = rr.getTwo().collectByte(bb->(byte)bb.getTwo()).toSet().toImmutable();
        Set<Term> t = new UnifiedSet<>(n - masked.size() + 1);
        for (byte i = 0; i < n; i++)
            if (!masked.contains(i))
                t.add(x[i]);
        t.add(rr.getOne() /* shadow */);
        t.add($.func(Member.the, f, $.sete(rr.getTwo().collect(ObjectIntPair::getOne))));
        return t;
    }

    public static Term applyAndNormalize(Term x) {
        Term y = apply(x);
        return y != x ? y.normalize() : x;
    }

    public static class FactorIntroduction extends Introduction {

        public FactorIntroduction(int capacity, NAR nar) {
            super(capacity, nar);
        }

        @Override
        protected boolean preFilter(Task next) {
            return next.op() == CONJ && Tense.dtSpecial(next.dt()) && next.term().subterms().subs(x -> x instanceof Compound) > 1;
        }

        @Override
        protected @Nullable Term newTerm(Task x) {
            Term xx = x.term();
            Term y = applyAndNormalize(xx);
            return y != xx ? y : null;
        }
    }
}
