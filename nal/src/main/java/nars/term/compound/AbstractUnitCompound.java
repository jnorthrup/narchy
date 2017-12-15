package nars.term.compound;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.sub.ProxySubterms;
import nars.term.sub.Subterms;
import nars.term.sub.UnitSubterm;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.time.Tense.DTERNAL;

public interface AbstractUnitCompound extends Compound {

    Term sub();

    @Override
    int structure();

    @Override
    default void recurseTerms(/*@NotNull*/ Consumer<Term> v) {
        v.accept(this);
        sub().recurseTerms(v);
    }

    @Override
    default boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        Term sub = sub();
        return inSubtermsOf.test(this) && (sub.equals(t) || sub.containsRecursively(t, root, inSubtermsOf));
    }

    @Override
    int hashCode();

    @Override
    default Term dt(int nextDT) {
        switch (nextDT) {
            case DTERNAL:
//            case XTERNAL:
//            case 0:
                return this;
        }
        throw new UnsupportedOperationException();
    }

    default boolean equivalent(@Nullable Term tt) {
        return tt.subs()==1 && opX()==tt.opX() && sub().equals(tt.sub(0)); //elides dt() comparison
    }

    @Override
    Op op();


    @Override
    default Subterms subterms() {

        return new UnitSubterm(sub());
        //return new TermVector1(sub, this);
        //return new SubtermView(this);
    }

    @Override
    default boolean impossibleSubTermVolume(int otherTermVolume) {
        return otherTermVolume > sub().volume() /* volume() -  size() */;
    }

    @Override
    default boolean isNormalized() {
        return sub().isNormalized();
    }

    @Override
    default int dt() {
        return DTERNAL;
    }
}
