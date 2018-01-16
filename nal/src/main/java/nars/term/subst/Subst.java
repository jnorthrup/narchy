package nars.term.subst;

import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.transform.CompoundTransform;
import org.jetbrains.annotations.Nullable;


public interface Subst extends CompoundTransform {

    /**
     * can be used to determine if this subst will have any possible effect on any transforms to any possible term,
     * used as a quick test to prevent transform initializations
     */
    boolean isEmpty();

    /**
     * the assigned value for x
     */
    @Nullable Term xy(Term t);


//    /** suggests to this to store a cached result, which can be ignored if the impl chooses */
//    void cache(@NotNull Term x /* usually a Variable */, @NotNull Term y);


//    @NotNull
//    default Term termOrOriginal(@NotNull Term t) {
//        Term x = term(t);
//        return x == null ? t : x;
//    }

    void clear();

    @Override
    default @Nullable Termed apply(Term nonCompound) {
        if (nonCompound instanceof Bool)//assert (!(x instanceof Bool));
            return nonCompound;

        Term y = xy(nonCompound);
        if (y != null) {
            return y; //an assigned substitution, whether a variable or other type of term
        } else {
            return nonCompound;
        }
    }


//    void forEach(@NotNull BiConsumer<? super Term, ? super Term> each);
//
//    /** returns true only if each evaluates true; if empty, returns true also */
//    default boolean forEach(@NotNull BiPredicate<? super Term, ? super Term> each) {
//        final boolean[] b = {true};
//        forEach((k,v)-> {
//            if (!each.test(k, v)) {
//                b[0] = false;
//            }
//        });
//        return b[0];
//    }


//
//    boolean match(final Term X, final Term Y);
//
//    /** matches when x is of target variable type */
//    boolean matchXvar(Variable x, Term y);
//
//    /** standard matching */
//    boolean next(Term x, Term y, int power);
//
//    /** compiled matching */
//    boolean next(TermPattern x, Term y, int power);
//
//    void putXY(Term x, Term y);
//    void putYX(Term x, Term y);
//


}
