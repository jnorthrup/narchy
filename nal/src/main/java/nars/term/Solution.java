package nars.term;

import jcog.list.FasterList;
import jcog.util.ArrayIterator;
import jcog.util.CartesianIterator;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.Op;
import nars.index.term.TermContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Solution {

    public static final ThreadLocal<Solution> solving = new ThreadLocal();

    final List<Predicate<VersionMap<Term, Term>>[]> proc = new FasterList(1);

    final Versioning v = new Versioning(32, 128);

    final VersionMap<Term, Term> subst = new VersionMap(v);

    public static Solution the() {
        return solving.get();
    }

    public static Term solve(Consumer<Solution> c) {
        Solution s = Solution.the();
        if (s != null) {
            c.accept(s);
        }
        return null;
    }

    public static Iterable<Term> solve(Term x, TermContext context) {
        if (!x.hasAny(Op.funcBits))
            return Collections.singleton(x);
        else {
            assert (solving.get() == null);
            Solution s = new Solution();
            solving.set(s);

            try {
                Term y = x.eval(context);
                Iterable<Term> solution;
                if (y.op().atomic)
                    solution = Collections.singleton(y);
                else
                    solution = s.get(y, context);
                return solution;
            } finally {
                solving.remove();
            }


        }
    }

    public Iterable<Term> get(Term x, TermContext context) {
        if (proc.isEmpty()) {
            return Collections.singleton(x);
        }

        //TODO length=1 special case doesnt need cartesian product
        Iterator<Predicate<VersionMap<Term, Term>>[]> pp;

        /*if (proc.size() > 1)*/ {
//        Iterator<Predicate<VersionMap<Term,Term>>> ci = new CartesianIterator(
//                Predicate[]::new,
//                Iterables.transform(proc, (Predicate<VersionMap<Term,Term>>[] p)
//                        -> (Iterable<Predicate<VersionMap<Term,Term>>>)(()->ArrayIterator.get(p))));

            pp = new CartesianIterator<Predicate<VersionMap<Term, Term>>>(
                    Predicate[]::new,
                    proc.stream().map(z -> ((Iterable) (() -> ArrayIterator.get(z))))
                            .toArray(Iterable[]::new)
            );

        }
//        else {
//            pp = proc.iterator();
//        }


        Set<Term> result = new HashSet(1); //TODO stream an iterable

        while (pp.hasNext()) {
            Predicate<VersionMap<Term, Term>>[] n = pp.next();
            assert (n.length > 0);

            int start = v.now();
            boolean fail = false;
            for (Predicate p : n) {
                if (!p.test(subst)) {
                    fail = true;
                    break;
                }
            }

            if (!fail) {

                @Nullable Term y = x.replace(subst);

                if (y != null && !y.equals(x)) {

                    y = y.eval(context);

                    if (y != null)

                        y = y.normalize();

                        if (!y.equals(x)) {
                            result.add(y);
                        }
                }
            }

            v.revert(start);
        }


        return result;
    }

    public void replace(Term x, Term xx) {
        replace(subst(x, xx));
    }

    public void replace(Term x, Term xx, Term y, Term yy) {
        replace(subst(x, xx, y, yy));
    }

    public void replace(Predicate... r) {
        proc.add(r);
    }

    private Predicate<VersionMap<Term, Term>> subst(Term x, Term xx) {
        return (m) -> {
            Term px = m.get(x);
            if (px != null) {
                return px.equals(xx); //set to other value, return true iff equal
            } else {
                m.tryPut(x, xx);
                return true;
            }
        };
    }

    public Predicate<VersionMap<Term, Term>> subst(Term x, Term xx, Term y, Term yy) {
        return (m) -> {
            return subst(x, xx).test(m) && subst(y, yy).test(m);
        };
    }
}
