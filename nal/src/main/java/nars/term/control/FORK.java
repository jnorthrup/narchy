package nars.term.control;

import jcog.TODO;
import nars.$;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;


/**
 * parallel branching
 * <p>
 * TODO generify beyond only Derivation
 */
public class FORK<X> extends AbstractPred<X> {

    /*@Stable*/
    public final PREDICATE<X>[] branch;

    public FORK(Collection<PREDICATE<X>> actions) {
        this(sort(actions.toArray(PREDICATE.EmptyPredicateArray)));
    }

    private static <X> PREDICATE<X>[] sort(PREDICATE<X>[] x) {
        if (x.length > 1)
            Arrays.sort(x);
        return x;
    }

    private FORK(PREDICATE<X>[] actions) {
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

        for (PREDICATE c: branch) {
            c.test(x);
        }

        return true;
    }

//    @Override
//    public MethodHandle compile() {
//        //https://stackoverflow.com/questions/52962364/how-to-chain-methodhandle-invocations
//        MethodHandle loop = MethodHandles.countedLoop(
//            constant(int.class, branch.length),
//            null,
//            MethodHandles.arrayElementGetter(PREDICATE[].class).bindTo(branch).asType(MethodType.methodType(Void.class)));
//        return MethodHandles.filterReturnValue(loop.bindTo(this), constant(boolean.class, true));
//
//
//
////        MethodHandle mh = null;
////        for (int i = 1; i < branch.length; i++) {
////            if (mh == null)
////                mh = MethodHandles.foldArguments(branch[i-1].compile(), branch[i].compile());
////            else
////                mh = MethodHandles.foldArguments(mh, branch[i].compile());
////        }
////        return mh;
////        MethodHandle[] mh = new MethodHandle[branch.length+1];
////        int i = 0;
////        for (PREDICATE p : branch) {
////            MethodHandle pp = p.compile();
////            //MethodType rt = pp.type().changeReturnType(void.class);
////            mh[i++] = pp.asType(MethodType.methodType(void.class, pp.type().parameterType(0)));
////        }
////        mh[i] = constant(boolean.class, false);
////        return MethodHandles.loop(mh);
//    }

    @Override
    public PREDICATE<X> transform(Function<PREDICATE<X>, PREDICATE<X>> f) {
        throw new TODO();

    }

    public static @Nullable <X> PREDICATE<X> fork(List<PREDICATE<X>> n, Function<List<PREDICATE<X>>, PREDICATE<X>> builder) {
        switch (n.size()) {
            case 0:
                return null;
            case 1:
                return n.iterator().next();
            default:
                return builder.apply(n);
        }
    }


}
