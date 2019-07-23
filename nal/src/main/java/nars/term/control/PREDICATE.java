package nars.term.control;

import com.google.common.collect.Iterators;
import jcog.Util;
import jcog.data.list.FasterList;
import nars.term.ProxyTerm;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The PrediTerm - a target-identified predicate (boolean-returning) function of a state
 *
 * @param X the type of state that is relevant to implementations
 */
public interface PREDICATE<X> extends Term, Predicate<X> {


    /** suspect */
    Comparator<PREDICATE> sortByCostIncreasing = (a, b) -> {
        if (a.equals(b)) return 0;
        float ac = a.cost();
        float bc = b.cost();
        if (ac > bc) return +1;
        else if (ac < bc) return -1;
        else return a.compareTo(b);
    };
    PREDICATE[] EMPTY_PREDICATE_ARRAY = new PREDICATE[0];


    static <X> PREDICATE<X>[] transform(Function<PREDICATE<X>, PREDICATE<X>> f, PREDICATE[] cache) {
        return Util.mapIfChanged(x -> x.transform(f), cache);
    }

    static <X> PREDICATE<X> compileAnd(PREDICATE<X>[] p) {
        switch (p.length) {
            case 0: return null;
            case 1: return p[0];
            default: {
                FasterList<PREDICATE<X>> pp = new FasterList<>(p);


                restart: do {
                    int ppp = pp.size();
                    for (int i = 0; i < ppp; i++) {
                        if (pp.get(i).reduceIn(pp)) {
                            if (pp.size()==1)
                                break;
                            continue restart; //modified
                        }
                    }
                } while (false); //pp.size() > 1);

                return AND.the(pp);
            }
        }
    }



    @Nullable
    static <X> PREDICATE<X> compileAnd(Collection<PREDICATE<X>> cond, @Nullable PREDICATE<X> conseq) {
        Iterator<PREDICATE<X>> c = cond.iterator();
        return compileAnd(
                    Iterators.toArray(
                            (conseq != null ? Iterators.concat(c, Iterators.singletonIterator(conseq)) : c),
                            PREDICATE.class)
            );
    }

    default PREDICATE<X> transform(Function<PREDICATE<X>, PREDICATE<X>> f) {
        return f != null ? f.apply(this) : this;
    }

    /**
     * a relative global estimate (against its possible sibling PrediTerm's)
     * of the average computational cost of running the test method
     * warning: these need to return constant values for sort consistency
     */
    default float cost() {
        return Float.POSITIVE_INFINITY;
    }

    /** optimization reducer: pp is the list of predicates in which this appears.
     *  return true if the list has been modified (ex: remove this, replace another etc)
     */
    default boolean reduceIn(FasterList<PREDICATE<X>> p) {
        return false;
    }


    default PREDICATE<X> neg() {
        return new NegPredicate<>(this);
    }

    final class NegPredicate<X> extends AbstractPred<X> {
        private final PREDICATE<X> p;

        NegPredicate(PREDICATE<X> p) {
            super(p instanceof ProxyTerm ? ((ProxyTerm)p).ref.neg() : p.term().neg());
            this.p = p;
        }

        @Override
        public float cost() {
            return p.cost() + 0.001f;
        }

        @Override
        public PREDICATE<X> neg() {
            return p; //unneg
        }

        @Override
        public boolean test(X o) {
            return !p.test(o);
        }
    }
}
