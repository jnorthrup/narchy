package nars.index.term;

import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * interface necessary for evaluating terms
 */
public interface TermContext extends Function<Term,Termed> {


    /** elides superfluous .term() call */
    default Term applyTermIfPossible(/*@NotNull*/ Term x) {
        Termed y = apply(x);
        if (y != null)
            return y.term();
        else
            return x;
    }

   /** elides superfluous .term() call */
    @Nullable
    default Term applyTermOrNull(/*@NotNull*/ Term x) {
        Termed y = apply(x);
        if (y != null)
            return y.term();
        else
            return null;
    }

//    /**
//     * internal get procedure: get if not absent
//     */
//    @Nullable
//    default Termed apply(@NotNull Term t) {
//        return apply(t, false);
//    }



}
