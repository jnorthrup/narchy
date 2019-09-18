package nars.op;

import jcog.Paper;
import jcog.Util;
import jcog.memoize.Memoizers;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.attention.What;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Variable;
import nars.term.*;
import nars.term.atom.Bool;
import nars.term.util.cache.Intermed;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.multimap.set.UnifiedSetMultimap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Function;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
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
 * final target does not need to ompute shadows except for paths which already exist; so order largest target last? (especially if only 2)
 *
 * TODO
 * allow modification of inner conjunctions, not only at top level
 */
@Paper
public class Factorize {

    static final Function<Subterms,Term[]> factorize = Memoizers.the.memoizeByte(
            Factorize.class.getSimpleName() + "_factorize",
            Intermed.SubtermsKey::new,
            Factorize::_factorize, 32 * 1024);

    private static Term[] _factorize(Intermed.SubtermsKey x) {
        Subterms xx = x.subs;

        Term[] xxx = distribute(xx), yyy;
        if (xxx != null)
            yyy = applyConj(xxx, xx.hasVarDep() ? f : fIfNoDep);
        else
            yyy = null;

        return yyy == null ?
                Op.EmptyTermArray  //not worth changing
                :
                yyy;
    }

    /** returns null if detects no reason to re-process */
    @Nullable private static Term[] distribute(Subterms xx) {
        final TermList x = xx.toList();

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

                if (Functor.func(s).equals(Member.member) && (var = s.subPath((byte) 0, (byte) 0)) instanceof Variable) {
                    Term r = s.subPath((byte) 0, (byte) 1);
                    if (r.op().set) {
                        if (xLength == 2) {
                            //special case: there is no reason to process this because it consists of one member and one non-member
                            return null;
                        }


                        //erase even if un-used, it would have no effect
                        x.removeFast(i);

                        Subterms rs = null;

                        ListIterator<Term> jj = x.listIterator();
                        while (jj.hasNext()) {
                            Term xj = jj.next();
                            if (xj.containsRecursively(var)) {
                                jj.remove();
                                if (rs == null)
                                     rs = r.subterms();
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
    static final Comparator<Pair<Term, RichIterable<ObjectBytePair<Term>>>> c = Comparator
            .comparingInt((Pair<Term, RichIterable<ObjectBytePair<Term>>> p) -> -p.getTwo().size()) //more unique subterms involved
            .thenComparingInt((Pair<Term, RichIterable<ObjectBytePair<Term>>> p) -> -p.getOne().volume()) //longest common path
            .thenComparingInt((Pair<Term, RichIterable<ObjectBytePair<Term>>> p) -> -(int)(p.getTwo().sumOfInt(z -> z.getOne().volume())));  //larger subterms involved

    //TODO static class ShadowCompound extends Compound(Compound base, ByteList masked)

    static Term shadow(Term base, ByteList path, Variable f) {
        return base.replaceAt(path, f);
    }

    /**
     * returns the subterms, as a sorted target array setAt, for the new conjunction.  or null if there was nothing factorable
     */
    @Nullable
    private static Term[] applyConj(Term[] x, Variable f) {

        /** shadow target -> replacements */
        final UnifiedSetMultimap<Term, ObjectBytePair<Term>>[] pp = new UnifiedSetMultimap[]{null};
        byte n = (byte) x.length;
        for (int i = 0; i < n; i++) {
            Term s = x[i];
            int pathMin = s.subs() == 1 ? 2 : 1; //dont factor the direct subterm of 1-arity compound (ex: negation, {x}, (x) )
            if (s instanceof Compound) {
                int ii = i;
                s.pathsTo((Term v) -> true, v -> true, (path, what) -> {

                    //if (what.unneg().volume() > 1) { //dont remap any atomics
                    if (!(what instanceof Variable)) { //dont remap variables
                        if (path.size() >= pathMin) {
                            if (pp[0] == null)
                                pp[0] = UnifiedSetMultimap.newMultimap();
                            Term v1 = shadow(s, path, f);
                            if (!(v1 instanceof Bool))
                                pp[0].put(v1, pair(what, (byte)ii));
                        }
                    }
                    return true;
                });

            }
        }

        UnifiedSetMultimap<Term, ObjectBytePair<Term>> p = pp[0];
        if (p == null)
            return null;

        MutableList<Pair<Term, RichIterable<ObjectBytePair<Term>>>> r = p.keyMultiValuePairsView().select((pathwhat) -> pathwhat.getTwo().size() > 1).toSortedList(c);

        if (r.isEmpty())
            return null;


        Pair<Term, RichIterable<ObjectBytePair<Term>>> rr = r.get(0);
        ByteSet masked = rr.getTwo().collectByte(ObjectBytePair::getTwo).toSet();
        Set<Term> t = new UnifiedSet<>(n - masked.size() + 1);
        for (byte i = 0; i < n; i++)
            if (!masked.contains(i)) {
                t.add(x[i]);
            }
        Term s = $.sete(rr.getTwo().collect((ob) -> {
            Term y = ob.getOne();
            return y.op() == CONJ && y.dt() == DTERNAL ? SETe.the(y.subterms()) : y; //flatten
        }));
        if (s instanceof Bool)
            return null;
        Term m = $.func(Member.member, f, s);
        if (m == Null)
            return null;

        t.add(rr.getOne() /* shadow */);
        t.add(m);

//        if (t.contains(Null))
//            return null; //invalid HACK detect earlier

        return Terms.commute(t);
    }



    public static class FactorIntroduction extends EventIntroduction {

        public FactorIntroduction( NAR nar) {
            super(nar);
            isNot(TheTask, IMPL);
        }

        @Override
        protected boolean filter(Term next) {
            return next.op()!=IMPL; //HACK doesnt always work well when CONJ'd with
        }

        @Override
        protected Term applyUnnormalized(Term x, int volMax, What w) {

            Term[] y = factorize.apply(x.subterms().commuted());
            if (y.length == 0)
                return x; //unchanged

            if (Util.or(yy -> yy.hasAny(BOOL), y))
                return x; //something collapsed, maybe an interaction with a prior member application

            if (Util.sum(Term::volume, y) > volMax - 1)
                return x; //excessively complex result


            //        Term[] yy = Terms.sorted(y);
//        if (xx.equalTerms(yy))
//            return x; //unchanged

            return CONJ.the(y);
        }

    }
}
