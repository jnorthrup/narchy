package nars.term.buffer;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.NAL;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.util.transform.TermTransform;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static nars.term.atom.Bool.Null;

/**
 * term combination iterator
 * TODO finish and test
 */
public class Termerator extends TermBuffer implements Iterable<Term> {

    /**
     * TODO Set?
     */
    protected FasterList<Iterable<Predicate<VersionMap<Term, Term>>>> termutes = null;

    protected Versioning v = null;

    protected VersionMap<Term, Term> subs = null;

    public Termerator() {

    }

    public Termerator(Term x) {
        this();
        append(x);
    }

    public Termerator(Term x, TermTransform f) {
        this();
        append(x, f);
    }

    /**
     * 2-ary AND
     */
    public static Predicate<VersionMap<Term, Term>> assign(Term x, Term xx, Term y, Term yy) {
        return m -> m.set(x, xx) && m.set(y, yy);
    }

    protected static Predicate<VersionMap<Term, Term>> assign(Term x, Term y) {
        return (subst) -> subst.set(x, y);
    }

    public static boolean canEval(Termlike x) {
        return x instanceof Compound && x.hasAll(Op.FuncBits);
    }

    /**
     * attempt to assert a substitution
     */
    public boolean isTry(Term x, Term y) {
        assert (!(x.equals(y)));
        Object y0 = subs.putIfAbsent(x, y);
        if (y0 == null) {
            return true;
        }
        return false;
    }

    /**
     * assert a termutation
     */
    public void canBe(Term x, Term... y) {
        if (y.length == 1)
            is(x, y[0]);
        else
            canBe(x, ArrayIterator.iterable(y));
    }

    public void clear() {
        super.clear();
        if (v != null) {
            termutes.clear();
            v.clear();
            subs.clear();
        }
    }

    /**
     * assign 1 variable
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public boolean is(Term x, Term y) {
        if (x.equals(y))
            return true;
        else {
            ensureReady();
            return subs.set(x, y);
        }

    }

    /**
     * assign 2-variables at once.
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public boolean is(Term x, Term xx, Term y, Term yy) {
        return is(x, xx) && is(y, yy);
    }

    /**
     * OR, forked
     * TODO limit when # termutators exceed limit
     */
    public void canBe(Iterable<Predicate<VersionMap<Term, Term>>> x) {
        ensureReady();
        termutes.add(x);
    }

    public void canBe(Predicate<VersionMap<Term, Term>> x) {
        ensureReady();
        termutes.add(List.of(x));
    }

    private void ensureReady() {
        if (v == null) {
            v = new Versioning<>(NAL.unify.UNIFICATION_STACK_CAPACITY, NAL.TASK_EVALUATION_TTL);
            subs = new VersionMap<>(v);
            termutes = new FasterList(1);
        }
    }

    public void canBe(Term x, Term y) {
        if (!x.equals(y))
            canBe(assign(x, y));
    }

    public void canBe(Term x, Collection<Term> y) {
        if (!y.isEmpty()) {
            canBe(x, (Iterable) y);
        }
    }

    public void canBePairs(List<Term> y) {
        canBe((VersionMap<Term, Term> e) -> {
            int n = y.size();
            for (int i = 0; i < n; ) {
                if (!e.set(y.get(i++), y.get(i++)))
                    return false;
            }
            return true;
        });
    }

    public void canBe(Term x, Iterable<Term> y) {
        canBe(Iterables.transform(y, yy -> assign(x, yy)));
    }

    public void canBe(Term a, Term b, Term x, Term y) {
        if (x.equals(y)) {
            canBe(a, b);
        } else if (a.equals(b)) {
            canBe(x, y);
        } else {
            canBe(assign(a, b, x, y));
        }
    }

    @Override
    protected Term nextTerm(byte[] bytes, int[] range) {

        Term x = super.nextTerm(bytes, range);
        Object y = subs.get(x);
        if (y != null) {
            if (y instanceof Term) {
                return (Term) y;
            } else {
                //termutator
                //TODO remove subs entry, rely on termutator cartesian iterator afterward
                throw new TODO();
            }
        }
        return x;
    }

    @Override
    public Iterator<Term> iterator() {
        if (termutes == null) {
            return new MyLazySingletonIterator();
        } else {

            int before = v.size();

            if (termutes.size() == 1) {
                return Iterators.filter(Iterators.transform(termutes.remove(0).iterator(), tt -> {
                    Term y;
                    if (tt.test(subs)) {
                        y = term();
                        if (v.size()!=before) {
                            //subs changed
                            //TODO
                        }
                    } else
                        y = null;
                    v.revert(before);
                    return y;
                }), Objects::nonNull);
            } else {
                throw new TODO();
//                CartesianIterator<Predicate>/*<VersionMap<Term,Term>>>*/ ci =
//                        new CartesianIterator(
//                                Predicate[]::new, termutes.toArrayRecycled(Iterable[]::new));
//                termutes.clear();
//                nextProduct:
//                while (ci.hasNext()) {
//
//                    v.revert(before);
//
//                    Predicate/*<VersionMap<Term,Term>>*/[] c = ci.next();
//
//                    for (Predicate<VersionMap<Term, Term>> cc : c) {
//                        if (cc == null)
//                            break; //null target list
//                        if (!cc.test(subs))
//                            continue nextProduct;
//                    }
//
//                    //all components applied successfully
//
//                    if (!recurse(e, y))
//                        return false;
//
//                }
            }
        }
//        subs.entrySet().removeIf((e)->{
//            Object v = e.getValue();
//            if (v instanceof Term) {
//                this.sub.interReplace()
//                return true;
//            }
//
//        });
//        if (subs.isEmpty())
//            return Iterators.singletonIterator(term());
//        else {
//
//            return subs.entrySet().stream(x->{
//               TermBuffer t = clone();
//
//
//            });
//        }

}


private final class MyLazySingletonIterator implements Iterator {

    private Term n = null;

    @Override
    public boolean hasNext() {
        if (n == null) {
            n = term();
        }
        return (n != Null);
    }

    @Override
    public Object next() {
        Term x = n;
        this.n = Null;
        return x;
    }
}
}
