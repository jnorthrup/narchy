package nars.term.control;

import jcog.TODO;
import nars.$;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;


/**
 * parallel branching
 * <p>
 * TODO generify beyond only Derivation
 */
public class FORK<X> extends AbstractPred<X> {

    /*@Stable*/
    public final PREDICATE<X>[] branch;

    public FORK(PREDICATE<X>[] actions) {
        super(
                $.sFast(actions)
        );
        assert (actions.length > 0);
        this.branch = actions;
    }

    /**
     * simple exhaustive impl
     */
    @Override
    public boolean test(X x) {

        for (PREDICATE c: branch)
            c.test(x);

        return true;
    }

    @Override
    public float cost() {
        return Float.POSITIVE_INFINITY;
    }

    @Override
    public PREDICATE<X> transform(Function<PREDICATE<X>, PREDICATE<X>> f) {
        throw new TODO();

    }


    @Nullable
    public static <X> PREDICATE<X> fork(Collection<PREDICATE<X>> x, Function<PREDICATE<X>[], PREDICATE<X>> builder) {
        PREDICATE<X>[] xx = x.toArray(PREDICATE.EMPTY_PREDICATE_ARRAY);
        Arrays.sort(xx);
        return fork(xx, builder);
    }

    @Nullable
    private static <X> PREDICATE<X> fork(PREDICATE<X>[] n, Function<PREDICATE<X>[], PREDICATE<X>> builder) {
        switch (n.length) {
            case 0:
                return null;
            case 1:
                return n[0];
            default:
                return builder.apply(n);
        }
    }


}
