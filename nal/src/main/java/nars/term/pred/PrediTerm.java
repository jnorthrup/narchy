package nars.term.pred;

import jcog.TODO;
import jcog.Util;
import jcog.list.FasterList;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * a term representing a predicate (boolean-returning) function of a state
 *
 * @param X the type of state that is relevant to implementations
 */
public interface PrediTerm<X> extends Term, Predicate<X> {


    Comparator<PrediTerm> sortByCost = (a, b) -> {
        if (a.equals(b)) return 0;
        float ac = a.cost();
        float bc = b.cost();
        if (ac > bc) return +1;
        else if (ac < bc) return -1;
        else return a.compareTo(b);
    };

    static Comparator<PrediTerm<?>> sort(ToIntFunction<PrediTerm<?>> count) {
        return (a, b) -> {

            //if (a.equals(b)) return 0; //doesnt seem to happen

            float ac = count.applyAsInt(a) / a.cost();
            float bc = count.applyAsInt(b) / b.cost();
            if (ac > bc) return -1;
            else if (ac < bc) return +1;
            else return a.compareTo(b);
        };
    }

    static <X> PrediTerm<X>[] transform(Function<PrediTerm<X>, PrediTerm<X>> f, PrediTerm[] cache) {
        return Util.map(x -> x.transform(f), new PrediTerm[cache.length], cache);
    }

    static PrediTerm compileAnd(PrediTerm[] p) {
        switch (p.length) {
            case 0: return null;
            case 1: return p[0];
            default:
                //PrediTerm[] pp = p.clone();
                FasterList<PrediTerm> pp = new FasterList(p);
                Iterator<PrediTerm> ppp = pp.iterator();
                while (ppp.hasNext()) {
                    if (!ppp.next().remainInAND(p))
                        ppp.remove();
                }
                if (pp.size() > 1)
                    pp.sort(sortByCost);

                return AndCondition.the(pp.toArrayRecycled(PrediTerm[]::new));
        }
    }

    @Nullable
    static <X> PrediTerm<X> compileAnd(Stream<PrediTerm<X>> cond, @Nullable PrediTerm<X> conseq) {
        return
            //AndCondition.the
            compileAnd(
                    (conseq != null ? Stream.concat(cond, Stream.of(conseq)) : cond)
                        .toArray(PrediTerm[]::new)
            )
        ;
    }

    default PrediTerm<X> transform(Function<PrediTerm<X>, PrediTerm<X>> f) {
        return f != null ? f.apply(this) : this;
    }

    /**
     * a relative global estimate (against its possible sibling PrediTerm's)
     * of the average computational cost of running the test method
     * warning: these need to return constant values for sort consistency
     */
    default float cost() {
        throw new TODO();
    }

    /** optimization subsumption: determines whether this predicate
     * should remain when appearing in an AND condition of the
     * specified predicates.  one of the 'p' in the array will be this instance
     */
    default boolean remainInAND(PrediTerm[] p) {
        return true;
    }

//    /** returns null on success; returns this instance on the test failure. go figure */
//    default PrediTerm<X> exec(X context, CPU cpu) {
//        if (!test(context))
//            return this;
//        else
//            return null;
//    }

}
