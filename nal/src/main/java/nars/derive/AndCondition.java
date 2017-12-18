package nars.derive;

import jcog.Util;
import nars.$;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * TODO generify beyond only Derivation
 */
public final class AndCondition<D> extends AbstractPred<D> {

    @Override
    public final boolean test(Object m) {
        for (PrediTerm x : cond) {
            boolean b = x.test(m);
            if (!b)
                return false;
        }
        return true;
    }
    AndCondition(PrediTerm<D>[] p) {
        super($.p((Term[]) p));
        assert (p.length >= 2) : "unnecessary use of AndCondition";
        this.cond = p;
    }

//    AndCondition(Collection<PrediTerm<D>> p) {
//        this(p.toArray(new PrediTerm[p.size()]));
//    }

    @NotNull
    public final PrediTerm<D>[] cond;

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
                                needsFlat[0] = true;//does this need to be recursed
                        }).toArray(PrediTerm[]::new);
                    }
                } while (needsFlat[0]);

                return new AndCondition(cond);
        }
    }

    public PrediTerm<D> first() {
        return cond[0];
    }
    public PrediTerm<D> last() {
        return cond[cond.length-1];
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


    @Override
    public PrediTerm<D> transform(Function<PrediTerm<D>, PrediTerm<D>> f) {
        return transform(o -> o, f);
    }

    public PrediTerm<D> transform(Function<AndCondition,PrediTerm<D>> outer, Function<PrediTerm<D>, PrediTerm<D>> f) {
        PrediTerm[] yy = transformedConditions(f);
        PrediTerm<D> z = yy != cond ? AndCondition.the(yy) : this;
        if (z instanceof AndCondition)
            return outer.apply((AndCondition)z);
        else
            return z;
    }

    public PrediTerm[] transformedConditions(Function<PrediTerm<D>, PrediTerm<D>> f) {
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


    /*public AndCondition(@NotNull BooleanCondition<C>[] p) {
        this(TermVector.the((Term[])p));
    }*/


    //    /** just attempts to evaluate the condition, causing any desired side effects as a result */
//    @Override public final void accept(@NotNull PremiseEval m, int now) {
//        booleanValueOf(m, now);
////        m.revert(now);
//    }


    public @Nullable PrediTerm<D> without(PrediTerm<D> condition) {
        PrediTerm[] x = ArrayUtils.remove(cond, ArrayUtils.indexOf(cond, condition)); assert(x.length != cond.length);
        return AndCondition.the(x);
    }

//    @Override
//    public PrediTerm exec(D d, CPU c) {
//
//        int i;
//        final int cacheLength = cache.length;
//        for (i = 0; i < cacheLength; i++) {
//            PrediTerm p = cache[i];
//
//            //if p.exec returns the same value (stored in 'q') and not a different or null, this is the signal that p.test FAILED
//            PrediTerm q = p.exec(d, c);
//            if (q == p)
//                break;
//        }
//
//        ((Derivation)d).use((1+i) * Param.TTL_PREDICATE);
//
//        return null;
//    }


}
