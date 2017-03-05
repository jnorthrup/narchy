package nars.term.transform;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** I = input term type, T = transformable subterm type */
public interface CompoundTransform<I extends Compound, T extends Term> extends Predicate<Term> {

    @Nullable Term apply(@Nullable I parent, @NotNull T subterm);

    /** enable predicate determined by the superterm, tested before processing any subterms */
    default boolean testSuperTerm(@NotNull I terms) {
        return true;
    }

//    CompoundTransform<Compound,Term> None = new CompoundTransform<Compound,Term>() {
//        @Override
//        public boolean test(Term o) {
//            return true;
//        }
//
//        @Nullable
//        @Override
//        public Term apply(Compound parent, Term subterm) {
//            return subterm;
//        }
//    };

}
