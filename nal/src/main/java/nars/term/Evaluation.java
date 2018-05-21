package nars.term;

import jcog.list.FasterList;
import jcog.util.ArrayIterator;
import jcog.util.CartesianIterator;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.NAR;
import nars.Op;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.ATOM;

public final class Evaluation {

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

    public static Set<Term> solveAll(Term x, NAR n) {
        Set<Term> all = new UnifiedSet<>(4);
        solve(x, n.functors, (y) -> { all.add(y); return true; });
        return !all.isEmpty() ? all : Set.of(x);
    }

    public static boolean solve(Term x, TermContext context, Predicate<Term> each) {
        if (!x.hasAll(Op.funcBits)) {
            each.test(x);
            return false;
        }

        Evaluation s = Evaluation.clear();

        Term y = x.eval(context);
        if (y.op().atomic)
            return each.test(y);
        else {
            return s.get(y, context, each);
        }
    }

    public static Evaluation clear() {
        Evaluation e = Evaluation.the();
        e.reset();
        return e;
    }

    private void reset() {
        proc.clear();
        v.reset();
    }

    public boolean get(Term x, TermContext context, Predicate<Term> each) {
        Iterator<Predicate<VersionMap<Term, Term>>[]> pp;

        int np = proc.size();
        switch (np) {
            case 0:
                //return Collections.singleton(x); //TODO do any substitutions need applied?
                each.test(x);
                return true;
//            case 1:
//                //length=1 special case doesnt need cartesian product
//                pp = Iterators.singletonIterator(proc.get(0)); //<- not working right
//                break;
            default: {
                Iterable[] aa = new Iterable[np];
                for (int i = 0; i < np; i++) {
                    Predicate<VersionMap<Term, Term>>[] pi = proc.get(i);
                    aa[i] = () -> ArrayIterator.get(pi);
                }
                pp = new CartesianIterator<Predicate<VersionMap<Term, Term>>>(Predicate[]::new, aa);
                break;
            }
        }

        proc.clear();

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
                    if (proc.isEmpty()) {

                        if (y!=null) {
                            y = y.normalize(); //TODO optional

                            if (!y.equals(x)) {
                                if (!each.test(y))
                                    return false;
                            }
                        }
                    } else {
                        //recurse, new proc added
                        return get(y, context, each);
                    }
                }
            }

            v.revert(start);
        }


        return true;
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


        class MapTermContext implements TermContext {
            private final ImmutableMap<Term, Term> resolvedImm;

            public MapTermContext(MutableMap<Term, Term> resolved) {
                this(resolved.toImmutable());
            }

            public MapTermContext(ImmutableMap<Term, Term> resolvedImm) {
                this.resolvedImm = resolvedImm;
            }

            @Override
            public Termed apply(Term term) {
                if (term.op() == ATOM) {
                    Term r = resolvedImm.get(term);
                    if (r != null)
                        return r;
                }
                return term;
            }
        }
    }
}
