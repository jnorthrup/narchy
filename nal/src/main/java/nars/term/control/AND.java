package nars.term.control;

import jcog.Util;
import nars.$;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** AND/while predicate chain */
public class AND<D> extends AbstractPred<D> {

    /*@Stable*/
    public final PREDICATE[] cond;

    @Override
    public final boolean test(Object m) {
        for (PREDICATE x : cond) {
            boolean b = x.test(m);
            if (!b)
                return false;
        }
        return true;
    }

    protected AND(PREDICATE<D>[] p) {
        super(
                //HeapTermBuilder.the.compound(PROD, p)
                $.pFast(p)
        );
        assert (p.length >= 2) : "unnecessary use of AndCondition";
        this.cond = p;
    }

    public static @Nullable <D> PREDICATE<D> the(List<PREDICATE<D>> cond) {
        int s = cond.size();
        switch (s) {
            case 0: return null;
            case 1: return cond.get(0);
            default: return the(cond.toArray(new PREDICATE[s]));
        }
    }

    public static @Nullable <D> PREDICATE<D> the(PREDICATE<D>... cond) {
        int s = cond.length;
        switch (s) {
            case 0: return null;
            case 1: return cond[0];
            default:
                final boolean[] needsFlat = {false};
                do {
                    for (Term c : cond) {
                        if (c instanceof AND) {
                            needsFlat[0] = true;
                            break;
                        }
                    }
                    if (needsFlat[0]) {
                        needsFlat[0] = false;
                        cond = Stream.of(cond).flatMap(x -> {
                            if (x instanceof AND)
                                return Stream.of(((AND) x).cond);
                            else
                                return Stream.of(x);
                        }).peek(x -> {
                            if ((x instanceof AND))
                                needsFlat[0] = true;
                        }).toArray(PREDICATE[]::new);
                    }
                } while (needsFlat[0]);

                return new AND(cond);
        }
    }

    public PREDICATE<D> first() {
        return Util.first(cond);
    }
    public PREDICATE<D> last() {
        return Util.last(cond);
    }

    /** chase the last of the last of the last(...etc.) condition in any number of recursive AND's */
    public static PREDICATE last(PREDICATE b) {
        while (b instanceof AND)
            b = ((AND)b).last();
        return b;
    }

     /** chase the last of the first of the first (...etc.) condition in any number of recursive AND's */
    public static PREDICATE first(PREDICATE b) {
        while (b instanceof AND)
            b = ((AND)b).first();
        return b;
    }
    @Nullable public static PREDICATE first(AND b, Predicate<PREDICATE> test) {
        int s = b.subs();
        for (int i = 0; i < s; i++) {
            PREDICATE x = (PREDICATE) b.sub(i);
            if (test.test(x))
                return x;
        }
        return null;
    }

    /** recursive */
    @Override public PREDICATE<D> transform(Function<PREDICATE<D>, PREDICATE<D>> f) {
        return transform((Function)f, f);
    }

    public PREDICATE<D> transform(Function<AND<D>, PREDICATE<D>> outer, @Nullable Function<PREDICATE<D>, PREDICATE<D>> f) {
        PREDICATE[] yy = transformedConditions(f);
        PREDICATE<D> z = yy != cond ? AND.the(yy) : this;
        if (z instanceof AND)
            return outer.apply((AND)z);
        else
            return z;
    }

    public PREDICATE[] transformedConditions(@Nullable Function<PREDICATE<D>, PREDICATE<D>> f) {
        if (f == null)
            return cond;

        final boolean[] changed = {false};
        PREDICATE[] yy = Util.map(x -> {
            PREDICATE<D> y = x.transform(f);
            if (y != x)
                changed[0] = true;
            return y;
        }, new PREDICATE[cond.length], cond);
        if (!changed[0])
            return cond;
        else
            return yy;
    }

    @Override
    public float cost() {
        return Util.sum((FloatFunction<PREDICATE>) PREDICATE::cost, cond);
    }

    public @Nullable PREDICATE<D> without(PREDICATE<D> condition) {
        PREDICATE[] x = ArrayUtils.remove(cond, PREDICATE[]::new, ArrayUtils.indexOf(cond, condition));
        assert(x.length != cond.length);
        return AND.the(x);
    }



}
