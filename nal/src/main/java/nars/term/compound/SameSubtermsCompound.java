package nars.term.compound;

import nars.subterm.Subterms;
import nars.subterm.util.TermMetadata;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** provides access to subterms via its own methods (or dynamically)
 *  as opposed to forwarding to another Subterms instance. */
public interface SameSubtermsCompound extends Compound {

    @Override
    default boolean isNormalized() {
        return TermMetadata.normalized(this);
    }

    @Override
    default boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, @Nullable Compound superterm) {
        //copied from Subterms.java
        return !inSuperCompound.test(this) ||
                whileTrue.test(this) &&
                AND(s -> s.recurseTerms(inSuperCompound, whileTrue, this));
    }

    @Override
    default boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm) {
        return !aSuperCompoundMust.test(this) ||
            whileTrue.test(this, superterm) && AND(s -> s.recurseTerms(aSuperCompoundMust, whileTrue, this));
    }

    @Override
    default boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        //copied from Subterms.java
        if (inSuperCompound.test(this) && whileTrue.test(this)) {
            int s = subs();
            for (int i = 0; i < s; i++)
                if (!sub(i).recurseTermsOrdered(inSuperCompound, whileTrue, parent))
                    return false;
            return true;
        } else
            return false;
    }

    @Override
    default boolean subtermsContainsRecursively(Term x, boolean root, Predicate<Term> subTermOf) {
        //copied from Subterms.java
        int s = subs();
        if (s == 1 || !impossibleSubTerm(x)) {
            for (int i = 0; i < s; i++) {
                Term ii = sub(i);
                if (ii == x || (root ? ii.equalsRoot(x) : ii.equals(x)) || ii.containsRecursively(x, root, subTermOf))
                    return true;
            }
        }
        return false;
    }

    @Override
    default Subterms subtermsContainer() {
        return this;
    }
}
