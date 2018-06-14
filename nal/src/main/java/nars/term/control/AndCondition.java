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


public class AndCondition<D> extends AbstractPred<D> {

    /*@Stable*/
    public final PrediTerm[] cond;

    @Override
    public final boolean test(Object m) {
        for (PrediTerm x : cond) {
            boolean b = x.test(m);
            if (!b)
                return false;
        }
        return true;
    }

    protected AndCondition(PrediTerm<D>[] p) {
        super($.pFast( p));
        assert (p.length >= 2) : "unnecessary use of AndCondition";
        this.cond = p;
    }

    public static @Nullable <D> PrediTerm<D> the(List<PrediTerm<D>> cond) {
        int s = cond.size();
        switch (s) {
            case 0: return null;
            case 1: return cond.get(0);
            default: return the(cond.toArray(new PrediTerm[s]));
        }
    }

    public static @Nullable <D> PrediTerm<D> the(PrediTerm<D>... cond) {
        int s = cond.length;
        switch (s) {
            case 0: return null;
            case 1: return cond[0];
            default:
                final boolean[] needsFlat = {false};
                do {
                    for (Term c : cond) {
                        if (c instanceof AndCondition) {
                            needsFlat[0] = true;
                            break;
                        }
                    }
                    if (needsFlat[0]) {
                        needsFlat[0] = false;
                        cond = Stream.of(cond).flatMap(x -> {
                            if (x instanceof AndCondition)
                                return Stream.of(((AndCondition) x).cond);
                            else
                                return Stream.of(x);
                        }).peek(x -> {
                            if ((x instanceof AndCondition))
                                needsFlat[0] = true;
                        }).toArray(PrediTerm[]::new);
                    }
                } while (needsFlat[0]);

                return new AndCondition(cond);
        }
    }

    public PrediTerm<D> first() {
        return Util.first(cond);
    }
    public PrediTerm<D> last() {
        return Util.last(cond);
    }

    /** chase the last of the last of the last(...etc.) condition in any number of recursive AND's */
    public static PrediTerm last(PrediTerm b) {
        while (b instanceof AndCondition)
            b = ((AndCondition)b).last();
        return b;
    }

     /** chase the last of the first of the first (...etc.) condition in any number of recursive AND's */
    public static PrediTerm first(PrediTerm b) {
        while (b instanceof AndCondition)
            b = ((AndCondition)b).first();
        return b;
    }
    @Nullable public static PrediTerm first(AndCondition b, Predicate<PrediTerm> test) {
        int s = b.subs();
        for (int i = 0; i < s; i++) {
            PrediTerm x = (PrediTerm) b.sub(i);
            if (test.test(x))
                return x;
        }
        return null;
    }

    @Override
    public PrediTerm<D> transform(Function<PrediTerm<D>, PrediTerm<D>> f) {
        return transform(o -> o, f);
    }

    public PrediTerm<D> transform(Function<AndCondition<D>,PrediTerm<D>> outer, @Nullable Function<PrediTerm<D>, PrediTerm<D>> f) {
        PrediTerm[] yy = transformedConditions(f);
        PrediTerm<D> z = yy != cond ? AndCondition.the(yy) : this;
        if (z instanceof AndCondition)
            return outer.apply((AndCondition)z);
        else
            return z;
    }

    public PrediTerm[] transformedConditions(@Nullable Function<PrediTerm<D>, PrediTerm<D>> f) {
        if (f == null)
            return cond;

        final boolean[] changed = {false};
        PrediTerm[] yy = Util.map(x -> {
            PrediTerm<D> y = x.transform(f);
            if (y != x)
                changed[0] = true;
            return y;
        }, new PrediTerm[cond.length], cond);
        if (!changed[0])
            return cond;
        else
            return yy;
    }

    @Override
    public float cost() {
        return Util.sum((FloatFunction<PrediTerm>) PrediTerm::cost, cond);
    }

    public @Nullable PrediTerm<D> without(PrediTerm<D> condition) {
        PrediTerm[] x = ArrayUtils.remove(cond, PrediTerm[]::new, ArrayUtils.indexOf(cond, condition));
        assert(x.length != cond.length);
        return AndCondition.the(x);
    }



}
