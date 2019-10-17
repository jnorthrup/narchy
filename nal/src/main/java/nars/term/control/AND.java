package nars.term.control;

import jcog.Util;
import jcog.WTF;
import nars.$;
import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jcog.data.iterator.ArrayIterator.stream;

/** AND/while predicate chain */
abstract public class AND<X> extends AbstractPred<X> {

    private final float cost;

    AND(PREDICATE<X>[] cond) {
        super(
                $.p(cond)
        );

        for (PREDICATE<X> x : cond)
            if (x instanceof AND)
                throw new UnsupportedOperationException("should have been flattened");

        assert (cond.length >= 2) : "unnecessary use of AndCondition";
        this.cost = Util.sum((FloatFunction<PREDICATE<X>>) PREDICATE::cost, cond);
    }

    private final static class ANDn<X> extends AND<X> {
        /*@Stable*/
        private final PREDICATE<X>[] cond;

        ANDn(PREDICATE<X>[] cond) {
            super(cond);
            this.cond = cond;
        }

        @Override
        public final boolean test(X m) {
            return Arrays.stream(cond).allMatch(x -> x.test(m));
        }


        public PREDICATE<X> first() {
            return Util.first(cond);
        }

        public PREDICATE<X> last() {
            return Util.last(cond);
        }

    }
    private final static class AND2<X> extends AND<X> {
        /*@Stable*/
        private final PREDICATE<X> a, b;

        private AND2(PREDICATE<X> a, PREDICATE<X> b) {
            super(new PREDICATE[]{ a, b });
            assert(!(a instanceof AND) && !(b instanceof AND));
            this.a = a; this.b = b;
        }

//        @Override
//        public MethodHandle compile() {
//            return MethodHandles.guardWithTest(a.compile(), b.compile(), MethodHandles.zero(boolean.class)).bindTo(this);
//        }

        @Override
        public final boolean test(X x) {
            return a.test(x) && b.test(x);
        }

        public PREDICATE<X> first() {
            return a;
        }

        public PREDICATE<X> last() {
            return b;
        }

    }
    private final static class AND3_MH<X> extends AND<X> {
        /*@Stable*/
        private final PREDICATE<X> a, b, c;

        final MethodHandle A, B, C;
        private AND3_MH(PREDICATE<X> a, PREDICATE<X> b, PREDICATE<X> c) {
            super(new PREDICATE[] { a, b, c });
            this.a = a; this.b = b; this.c = c;
            this.A = a.method();
            this.B = b.method();
            this.C = c.method();
        }

        @Override
        public final boolean test(X x) {
            //return a.test(x) && b.test(x) && c.test(x);
            try {
                return ((boolean)A.invokeExact(x)) && ((boolean)B.invokeExact(x)) && ((boolean)C.invokeExact(x));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        public PREDICATE<X> first() {
            return a;
        }

        public PREDICATE<X> last() {
            return c;
        }

    }
    private final static class AND3<X> extends AND<X> {
        /*@Stable*/
        private final PREDICATE<X> a, b, c;

        private AND3(PREDICATE<X> a, PREDICATE<X> b, PREDICATE<X> c) {
            super(new PREDICATE[] { a, b, c });
            this.a = a; this.b = b; this.c = c;
        }

        @Override
        public final boolean test(X x) {
            return Stream.of(a, b, c).allMatch(xpredicate -> xpredicate.test(x));
        }

        public PREDICATE<X> first() {
            return a;
        }

        public PREDICATE<X> last() {
            return c;
        }

    }

    public static @Nullable <D> PREDICATE<D> the(List<PREDICATE<D>> cond) {
        int s = cond.size();
        switch (s) {
            case 0: return null;
            case 1: return cond.get(0);
            default: return the(cond.toArray(new PREDICATE[s]));
        }
    }

    public static @Nullable <D> PREDICATE<D> the(Term[] cond) {
        int s = cond.length;
        switch (s) {
            case 0: return null;
            case 1: return (PREDICATE<D>) cond[0];
            default:
                boolean needsFlat = Arrays.stream(cond).anyMatch(c -> c instanceof AND);
                if (needsFlat || !(cond instanceof PREDICATE[])) {
                    cond = stream(cond).flatMap(
                            x -> x instanceof AND ?
                                    ((Compound)x).subStream() :
                                    Stream.of(x))
                    .toArray(PREDICATE[]::new);
                }


                switch (cond.length) {
                    case 0:
                    case 1:
                        throw new WTF();
                    case 2:
                        return new AND2<>((PREDICATE)cond[0], (PREDICATE)cond[1]);
                    case 3:
                        return new AND3<>((PREDICATE)cond[0], (PREDICATE)cond[1], (PREDICATE)cond[2]);
                    default:
                        return new ANDn<>((PREDICATE[])cond);
                }

        }
    }

    protected abstract PREDICATE<X> first();
    protected abstract PREDICATE<X> last();


    @Override
    public final float cost() {
        return cost;
    }


    /** chase the last of the last of the last(...etc.) condition in any number of recursive AND's */
    public static <X> PREDICATE<X> last(PREDICATE<X> b) {
        while (b instanceof AND)
            b = ((AND<X>)b).last();
        return b;
    }

     /** chase the last of the first of the first (...etc.) condition in any number of recursive AND's */
    public static <X> PREDICATE<X>  irst(PREDICATE<X>  b) {
        while (b instanceof AND)
            b = ((AND<X> )b).first();
        return b;
    }
    @Nullable public static <X> PREDICATE<X>  first(AND<X>  b, Predicate<PREDICATE<X> > test) {
        int s = b.subs();
        return IntStream.range(0, s).mapToObj(i -> (PREDICATE<X>) b.sub(i)).filter(test).findFirst().orElse(null);
    }

    /** recursive */
    @Override public PREDICATE<X> transform(Function<PREDICATE<X>, PREDICATE<X>> f) {
        //return transform((Function)f, f);
        throw new UnsupportedOperationException();
    }

//    public PREDICATE<D> transform(Function<AND<D>, PREDICATE<D>> outer, @Nullable Function<PREDICATE<D>, PREDICATE<D>> f) {
//        PREDICATE[] yy = transformedConditions(f);
//        PREDICATE<D> z = yy != cond ? AND.the(yy) : this;
//        if (z instanceof AND)
//            return outer.apply((AND)z);
//        else
//            return z;
//    }

//    public PREDICATE[] transformedConditions(@Nullable Function<PREDICATE<D>, PREDICATE<D>> f) {
//        if (f == null)
//            return cond;
//
//        final boolean[] changed = {false};
//        PREDICATE[] yy = Util.map(x -> {
//            PREDICATE<D> y = x.transform(f);
//            if (y != x)
//                changed[0] = true;
//            return y;
//        }, new PREDICATE[cond.length], cond);
//        if (!changed[0])
//            return cond;
//        else
//            return yy;
//    }


//    public @Nullable PREDICATE<D> without(PREDICATE<D> condition) {
//        PREDICATE[] x = ArrayUtils.remove(cond, PREDICATE[]::new, ArrayUtils.indexOf(cond, condition));
//        assert(x.length != cond.length);
//        return AND.the(x);
//    }



}
