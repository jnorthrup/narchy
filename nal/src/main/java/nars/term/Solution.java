package nars.term;

import jcog.list.FasterList;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.Op;
import nars.index.term.TermContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class Solution {

    public static final ThreadLocal<Solution> solving = new ThreadLocal();

    final List<Predicate<VersionMap<Term,Term>>[]> proc = new FasterList(1);

    final Versioning v = new Versioning(32, 128);

    final VersionMap<Term,Term> subst = new VersionMap(v);

    public static Solution the() {
        return solving.get();
    }

    public Iterable<Term> get(Term x, TermContext context) {
        if (proc.isEmpty()) {
            return Collections.singleton(x);
        }

        //TODO length=1 special case doesnt need cartesian product
        assert(proc.size()==1);

        Iterator<Predicate<VersionMap<Term,Term>>[]> ci = proc.iterator();

        Set<Term> result = new HashSet() ; //TODO stream an iterable

//        Iterator<Predicate<VersionMap<Term,Term>>> ci = new CartesianIterator(
//                Predicate[]::new,
//                Iterables.transform(proc, (Predicate<VersionMap<Term,Term>>[] p)
//                        -> (Iterable<Predicate<VersionMap<Term,Term>>>)(()->ArrayIterator.get(p))));

        while (ci.hasNext()) {
            Predicate<VersionMap<Term, Term>>[] n = ci.next();
            assert(n.length > 0);

            int start = v.now();
            for (Predicate p : n) {
                if (!p.test(subst))
                    return null;

                @Nullable Term y = x.replace(subst);

                if (y!=null && !y.equals(x)) {

//                y = y.eval(context);
//
//                if (y!=null && !y.equals(x)) {
                    result.add(y);
//                }
                }

                v.revert(start);
            }


        }

        return result;
    }

    public static Iterable<Term> solve(Term x, TermContext context) {
        if (!x.hasAny(Op.VariableBits) || !x.hasAny(Op.funcBits))
            return Collections.singleton(x);
        else {
            Solution s = new Solution();
            assert(solving.get()==null);
            solving.set(s);

            Term y = x.eval(context);
            if (y.op().atomic)
                return Collections.singleton(y);

            Iterable<Term> solution = s.get(y, context);

            solving.remove();

            return solution;
        }
    }

    public void replace(Term x, Term xx) {
        replace( subst(x, xx) );
    }

    public void replace(Term x, Term xx, Term y, Term yy) {
        replace( subst(x, xx, y, yy) );
    }
    public void replace(Predicate... r) {
        proc.add( r );
    }

    private Predicate<VersionMap<Term,Term>> subst(Term x, Term xx) {
        return (m)->{
            Term px = m.get(x);
            if (px != null) {
                return px.equals(xx); //set to other value, return true iff equal
            } else {
                m.tryPut(x, xx);
                return true;
            }
        };
    }

    public Predicate<VersionMap<Term,Term>> subst(Term x, Term xx, Term y, Term yy) {
        return (m)->{
            return subst(x, xx).test(m) && subst(y, yy).test(m);
        };
    }
}
