package nars.term.buffer;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import jcog.data.iterator.CartesianIterator;
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
public class Termerator extends EvalTermBuffer implements Iterable<Term> {

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
        return y0 == null;
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

    @Override
    public void clear(boolean code, boolean uniques) {
        super.clear(code, uniques);
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
            if (y == Null)
                return false;
            if (y.containsRecursively(x))
                return false; //loop

            ensureReady();
            boolean empty = subs.isEmpty();
            Term existingAssignment = !empty ? subs.get(x) : null;
            if (existingAssignment == null) {
                if (!empty) {
                    Term z = y.replace(subs); //transform the assignment result preventing loops etc
//                    if (z!=y)
//                        Util.nop(); //TEMPORARY
                    y = z;
                }
                return subs.set(x, y);
            } else
                return y.equals(existingAssignment);
        }

    }

    /**
     * assign 2-variables at once.
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public boolean is(Term x, Term xx, Term y, Term yy) {
        return is(x, xx) && is(y, yy);
    }

    public void canBe(Predicate<VersionMap<Term, Term>> x) {
        canBe(List.of(x));
    }

    /**
     * OR, forked
     * TODO limit when # termutators exceed limit
     */
    public final void canBe(Iterable<Predicate<VersionMap<Term, Term>>> x) {
        ensureReady();
        termutes.add(x);
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
        Term  y = subs.get(x);
        if (y != null) {
            if (y instanceof Term) {
                return y;
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
        int nt = termutes == null ?  0 : termutes.size();
        if (nt == 0) {
            return new MyLazySingletonIterator();
        } else {

            int before = v.size();

            if (nt == 1) {
                return Iterators.filter(Iterators.transform(termutes.remove(0).iterator(), tt -> {
                    Term y;
                    if (tt.test(subs)) {
                        int during = v.size();
                        y = term();
                        if (v.size() != during) {
                            throw new TODO("recurse");
                        }
                    } else
                        y = null;
                    v.revert(before);
                    return y;
                }), Objects::nonNull);
            } else {
                CartesianIterator<Predicate>/*<VersionMap<Term,Term>>>*/ ci =
                        new CartesianIterator(Predicate[]::new, termutes.toArrayRecycled(Iterable[]::new));
                termutes.clear();
                return Iterators.filter(Iterators.transform(ci, tt -> {
                    Term y;
                    boolean fail = false;
                    for (Predicate p : tt) {
                        if (!p.test(subs)) {
                            fail = true;
                            break;
                        }
                    }
                    if (!fail) {
                        int during = v.size();
                        y = term();
                        if (v.size() != during) {
                            throw new TODO("recurse");
                        }
                    } else
                        y = null;
                    v.revert(before);
                    return y;
                }), Objects::nonNull);
            }
        }

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
