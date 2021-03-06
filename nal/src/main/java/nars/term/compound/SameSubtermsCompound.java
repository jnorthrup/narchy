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
                AND(new Predicate<Term>() {
                    @Override
                    public boolean test(Term s) {
                        return s.recurseTerms(inSuperCompound, whileTrue, SameSubtermsCompound.this);
                    }
                });
    }

    @Override
    default boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm) {
        return !aSuperCompoundMust.test(this) ||
                whileTrue.test(this, superterm) &&
                AND(new Predicate<Term>() {
                    @Override
                    public boolean test(Term s) {
                        return s.recurseTerms(aSuperCompoundMust, whileTrue, SameSubtermsCompound.this);
                    }
                });
    }

    @Override
    default boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        //copied from Subterms.java
        return inSuperCompound.test(this) && whileTrue.test(this) && AND(new Predicate<Term>() {
            @Override
            public boolean test(Term i) {
                return i.recurseTermsOrdered(inSuperCompound, whileTrue, parent);
            }
        });
    }

    @Override
    default boolean subtermsContainsRecursively(Term x, boolean root, Predicate<Term> subTermOf) {
        //copied from Subterms.java
        return (subs() == 1 || !impossibleSubTerm(x)) &&
            AND(new Predicate<Term>() {
                @Override
                public boolean test(Term ii) {
                    return ii == x || (root ? ii.equalsRoot(x) : ii.equals(x)) || ii.containsRecursively(x, root, subTermOf);
                }
            });
    }

    @Override
    default Subterms subtermsDirect() {
        return this;
    }
}
