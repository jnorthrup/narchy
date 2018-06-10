package nars.term.compound;

import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.function.Predicate;

import static nars.time.Tense.DTERNAL;

public interface SeparateSubtermsCompound extends Compound {

    @Override
    default int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return subterms().intifyRecurse(reduce, reduce.intValueOf(v, this));
    }

    @Override
    default int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return subterms().intifyShallow(reduce, v);
    }

    @Override
    default boolean isTemporal() {
        return (dt() != DTERNAL && op().temporal)
                ||
                (subterms().isTemporal());
    }

    @Override
    default boolean OR(/*@NotNull*/ Predicate<Term> p) {
        return subterms().OR(p);
    }

    @Override
    default boolean AND(/*@NotNull*/ Predicate<Term> p) {
        return subterms().AND(p);
    }

}
