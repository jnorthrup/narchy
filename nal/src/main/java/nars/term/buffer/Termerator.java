package nars.term.buffer;

import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import jcog.data.iterator.CartesianIterator;
import jcog.data.list.FasterList;
import jcog.version.MultiVersionMap;
import jcog.version.Versioning;
import nars.NAL;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.util.TermException;
import nars.term.util.transform.TermTransform;

import java.util.*;
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
    protected FasterList<Iterable<Predicate<Termerator>>> termutes = null;

    protected Versioning v = null;

    protected MultiVersionMap<Term, Term> subs = null;

    protected Termerator() {

    }

    Termerator(Term x) {
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
    public static Predicate<Termerator> assign(Term x, Term xx, Term y, Term yy) {
        if (x.equals(xx))
            throw new TermException("assign cycle", x);
        if (y.equals(yy))
            throw new TermException("assign cycle", y);

        return m -> m.is(x, xx) && m.is(y, yy);
    }

    protected static Predicate<Termerator> assign(Term x, Term y) {
        if (x.equals(y))
            throw new TermException("assign cycle", x);
        return (subst) -> subst.is(x, y);
    }

    public static boolean evalable(Termlike x) {
        return x instanceof Compound && x.hasAll(Op.FuncBits);
    }

//    /**
//     * attempt to assert a substitution
//     */
//    public boolean isTry(Term x, Term y) {
//        assert (!(x.equals(y)));
//        Object y0 = subs.putIfAbsent(x, y);
//        return y0 == null;
//    }

    /**
     * assert a termutation
     */
    public void canBe(Term x, Term... y) {
        if (y.length == 0)
            throw new NullPointerException();
        else if (y.length == 1)
            is(x, y[0]);
        else {

            //remove duplicates
            Set<Term> yy = Set.of(y);
            if (yy.size()!=y.length)
                y = yy.toArray(Op.EmptyTermArray);

            canBe(x, ArrayIterator.iterable(y));
        }
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
    public final boolean is(Term x, Term y) {

        if (x.equals(y))
            return true;

        if (x == Null || y == Null) return false;

        if (y instanceof Compound && y.containsRecursively(x))
            return false; //loop

        boolean empty = v==null || v.size()==0;

        if (!empty) {
            Term xx = x.replace(subs);
            Term yy = y.replace(subs);
            if (!yy.equals(y) || !xx.equals(x))
                return is(xx, yy); //recurse
        }

        if (!empty) {
            //replace existing subs
            if (!subs.replace((sx, sy)-> !x.equals(sx) ? sy.replace(x, y) : sy))
                return false;

        } else {
            ensureReady();
        }

        return subs.set(x, y);     //assert(set); //return true;
    }

    /**
     * assign 2-variables at once.
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public boolean is(Term x, Term xx, Term y, Term yy) {
        return is(x, xx) && is(y, yy);
    }

    private void canBe(Predicate<Termerator> x) {
        canBe(List.of(x));
    }

    /**
     * OR, forked
     * TODO limit when # termutators exceed limit
     */
    public final void canBe(Iterable<Predicate<Termerator>> x) {
        ensureReady();
        termutes.add(x);
    }


    private void ensureReady() {
        if (v == null) {
            v = new Versioning<>(NAL.unify.UNIFICATION_STACK_CAPACITY, NAL.TASK_EVALUATION_TTL);
            subs = new MultiVersionMap<>(v, NAL.unify.UNIFY_VAR_RECURSION_DEPTH_LIMIT);
            termutes = new FasterList<>(1);
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
        canBe((Termerator e) -> {
            int n = y.size();
            for (int i = 0; i < n; ) {
                if (!e.is(y.get(i++), y.get(i++)))
                    return false;
            }
            return true;
        });
    }

    public void canBe(Term x, Iterable<Term> y) {
        canBe(new ChoiceIterable(x, y));
    }


    @Override
    protected Term nextTerm(byte[] bytes, int[] range) {

        Term x = super.nextTerm(bytes, range);
        Term  y = subs.get(x);
        if (y != null) {
            return y;
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
                    if (tt.test(this)) {
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
                    for (Predicate<Termerator> p : tt) {
                        if (!p.test(this)) {
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

    private static final class ChoiceIterable implements Iterable<Predicate<Termerator>> {
        final Iterable<Term> y;
        private final Term x;

        ChoiceIterable(Term x, Iterable<Term> y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public Iterator<Predicate<Termerator>> iterator() {
            return new ChoiceIterator(x, y);
        }
    }

    private static final class ChoiceIterator implements Iterator<Predicate<Termerator>> {

        final Iterator<Term> y;
        private final Term x;

        ChoiceIterator(Term x, Iterable<Term> y) {
            this(x, y.iterator());
        }
        ChoiceIterator(Term x, Iterator<Term> y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean hasNext() {
            return y.hasNext();
        }

        @Override
        public Predicate<Termerator> next() {
            return assign(x, y.next());
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
