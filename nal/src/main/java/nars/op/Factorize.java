package nars.op;

import nars.$;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.time.Tense;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.multimap.set.UnifiedSetMultimap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Set;

import static nars.Op.CONJ;
import static nars.Op.NEG;

/**
 *
 *     trivial case:
 *         (f(a) && f(b))   |-   (f(#1) && member(#1,{a,b}))
 *     trivial induction case:
 *         ((f(#1) && member(#1,{a,b})) && f(c))
 *         (&&, f(c), f(#1),  member(#1,{a,b}))
 *         (&&, f(#2), member(#2, {#1,c}),  member(#1,{a,b}))   |-   (f(#1) && member(#1,{a,b,c}))
 *
 */
public class Factorize {
    public static Term apply(Term x) {
        if (x.op()==NEG) {
            Term xu = x.unneg();
            if (xu.op()==CONJ) {
                Term y = apply(xu);
                if (y!=xu)
                    return y.neg();
            }
            return x;
        }

        int xdt = x.dt();
        if (x.op()!=CONJ || !Tense.dtSpecial(xdt))
            return x; //unchanged

        @Nullable Set<Term> y = applyConj(x.subterms(), x.hasVarDep() ? f:fIfNoDep);
        if (y == null)
            return x; //unchanged

        return CONJ.the(xdt, y);
    }

    private final static Variable f = $.varDep("_f");
    private final static Variable fIfNoDep = $.varDep(1);

    //TODO static class ShadowCompound extends Compound(Compound base, ByteList masked)

    static Term shadow(Term base, ByteList path, Variable f) {
        return base.replaceAt(path, f);
    }

    /** returns the subterms, as a set, for the new conjunction.  or null if there was nothing factorable */
    @Nullable protected static Set<Term> applyConj(Subterms x, Variable f) {

        /** shadow term -> replacements */
        UnifiedSetMultimap<Term, ObjectIntPair<Term>> p = UnifiedSetMultimap.newMultimap();
        int n = x.subs();
        for (int i = 0; i < n; i++) {
            Term s = x.sub(i);
            int pathMin = s.subs() == 1 ? 2 : 1; //dont factor the direct subterm of 1-arity compound (ex: negation, {x}, (x) )
            if (s instanceof Compound) {
                int ii = i;
                s.pathsTo((Term v) -> true, v -> true, (path, what) -> {
                    if (what instanceof Variable)
                        return true; //dont remap variables
                    if (path.size() >= pathMin) {
                        Term shadow = shadow(s, path, f);
                        p.put(shadow, PrimitiveTuples.pair(what, ii));
                    }
                    return true;
                });
            }
        }
        MutableList<Pair<Term, RichIterable<ObjectIntPair<Term>>>> r = p.keyMultiValuePairsView().select((pathwhat) -> pathwhat.getTwo().size() > 1).toSortedList(
                Comparator
                    .comparingInt((Pair<Term, RichIterable<ObjectIntPair<Term>>> pp) -> -pp.getTwo().size()) //more unique subterms involved
                    .thenComparingInt((Pair<Term, RichIterable<ObjectIntPair<Term>>> pp) -> -pp.getOne().volume()) //longest common path
                    .thenComparingInt((Pair<Term, RichIterable<ObjectIntPair<Term>>> pp) -> -(int)(pp.getTwo().sumOfInt(z -> z.getOne().volume()))) //larger subterms involved
        );

        if (r.isEmpty())
            return null;


        Pair<Term, RichIterable<ObjectIntPair<Term>>> rr = r.get(0);
        MutableIntSet masked = rr.getTwo().collectInt(ObjectIntPair::getTwo).toSet();
        Set<Term> t = new UnifiedSet<>(x.subs() - masked.size() + 1);
        for (int i = 0; i < n; i++) {
            if (masked.contains(i))
                continue;
            Term xx = x.sub(i);
//            if (xx instanceof Compound) {
//                if (masked.contains(i)) {
//
//                }
//                ByteList path = rr.getOne().getOne();
//                Term xxx = xx.subPath(path);
//                if (rr.getTwo().contains(xxx)) {
//                    Term yy = xx instanceof Compound ? xx.replaceAt(path, f) : xx;
//                    xx = yy;
//                }
//            }
            t.add(xx);
        }
        t.add(rr.getOne() /* shadow */);
        t.add($.func(Member.the, f, $.sete(rr.getTwo().collect(ObjectIntPair::getOne))));
        return t;
    }

    public static Term applyAndNormalize(Term x) {
        Term y = apply(x);
        return y != x ? y.normalize() : x;
    }
}
