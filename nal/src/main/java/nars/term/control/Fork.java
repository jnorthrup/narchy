package nars.term.control;

import jcog.TODO;
import nars.$;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;


/**
 * parallel branching
 * <p>
 * TODO generify beyond only Derivation
 */
public class Fork<X> extends AbstractPred<X> {

    /*@Stable*/
    public final PrediTerm<X>[] branch;

    public Fork(PrediTerm<X>[] actions) {
        super($.pFast((Term[]) actions) /* doesnt force sorting. in some impl, the index order must remain intact */);
        assert (actions.length > 0);
        this.branch = actions;
    }

    /**
     * simple exhaustive impl
     */
    @Override
    public boolean test(X x) {

        for (PrediTerm c : branch)
            c.test(x);

        return true;
    }

    @Override
    public float cost() {
        return Float.POSITIVE_INFINITY;
    }

    @Override
    public PrediTerm<X> transform(Function<PrediTerm<X>, PrediTerm<X>> f) {
        throw new TODO();

    }
































    @Nullable
    public static <X> PrediTerm<X> fork(Collection<PrediTerm<X>> x, Function<PrediTerm[], PrediTerm<X>> builder) {
        PrediTerm[] xx = x.toArray(PrediTerm.EmptyPrediTermArray);
        Arrays.sort(xx);
        return fork(xx, builder);
    }

    @Nullable
    public static <X> PrediTerm<X>  fork(PrediTerm[] n, Function<PrediTerm[], PrediTerm<X>> builder) {
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
