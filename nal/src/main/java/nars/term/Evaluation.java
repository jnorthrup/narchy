package nars.term;

import jcog.list.FasterList;
import jcog.util.ArrayIterator;
import jcog.util.CartesianIterator;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.NAR;
import nars.Op;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Evaluation {

    static final ThreadLocal<Evaluation> solving = ThreadLocal.withInitial(Evaluation::new);

    final List<Predicate<VersionMap<Term, Term>>[]> proc = new FasterList(1);

    final Versioning v = new Versioning(32, 128);

    final VersionMap<Term, Term> subst = new VersionMap(v);


    private Evaluation() {

    }

    public static Evaluation start() {
        return solving.get().clear();
    }

    public static Evaluation the() {
        return solving.get();
    }

    public static Term solve(Consumer<Evaluation> c) {
        Evaluation s = Evaluation.the();
        if (s != null) {
            c.accept(s);
        }
        return null;
    }

    public static Iterable<Term> solve(Term x, NAR n) {
        return solve(x, n.concepts.functors);
    }

    public static Iterable<Term> solve(Term x, TermContext context) {
        if (!x.hasAny(Op.funcBits))
            return Collections.singleton(x);
        else {
            Evaluation s = Evaluation.clear();

            Term y = x.eval(context);
            Iterable<Term> solution;
            if (y.op().atomic)
                solution = Collections.singleton(y);
            else
                solution = s.get(y, context);
            return solution;
        }
    }

    public static Evaluation clear() {
        Evaluation e = Evaluation.the();
        e.reset();
        return e;
    }

    private void reset() {
        if (v.reset())
            subst.map.clear();
        proc.clear();
    }

    public Iterable<Term> get(Term x, TermContext context) {
        Iterator<Predicate<VersionMap<Term, Term>>[]> pp;

        switch (proc.size()) {
            case 0:
                return Collections.singleton(x); //TODO do any substitutions need applied?
//            case 1:
//                //length=1 special case doesnt need cartesian product
//                pp = Iterators.singletonIterator(proc.get(0)); //<- not working right
//                break;
            default:
                pp = new CartesianIterator<Predicate<VersionMap<Term, Term>>>(
                        Predicate[]::new,
                        proc.stream().map(z -> ((Iterable) (() -> ArrayIterator.get(z))))
                                .toArray(Iterable[]::new)
                );
                break;
        }

        Set<Term> result = new UnifiedSet<>(4); //TODO stream an iterable

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
        return (m) -> subst(x, xx).test(m) && subst(y, yy).test(m);
    }

    /**
     * interface necessary for evaluating terms
     */
    public interface TermContext extends Function<Term, Termed> {


        /**
         * elides superfluous .term() call
         */
        default Term applyTermIfPossible(/*@NotNull*/ Term x, Op supertermOp, int subterm) {
            Termed y = apply(x);
            return y != null ? y.term() : x;
        }


    }
}
