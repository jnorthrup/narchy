package nars.term.compound;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.sub.Subterms;
import nars.term.sub.UnitSubterm;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.time.Tense.DTERNAL;

public abstract class UnitCompound implements Compound {

    @Override
    abstract public Op op();

    abstract public Term sub();

    @Override
    public int hashCode() {
         return Util.hashCombine(
                 hashCodeSubterms(),
                 op().id);
    }

    @Override
    public final int hashCodeSubterms() {
        return Util.hashCombine1(sub().hashCode());
    }


    @Override
    public final int opX() {
        return Term.opX(op(), 1);
    }

    @Override
    public final int subs() {
        return 1;
    }

    /*@NotNull*/
    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }

    @Override
    public final boolean equals(@Nullable Object that) {
        if (this == that) return true;
        if (!(that instanceof Term) || hashCode() != that.hashCode())
            return false;
        return ((Term)that).subs()==1 && opX()== ((Term)that).opX() && sub().equals(((Term)that).sub(0)); //elides dt() comparison
    }



    @Override
    public void recurseTerms(/*@NotNull*/ Consumer<Term> v) {
        v.accept(this);
        sub().recurseTerms(v);
    }

    @Override
    public boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        if (inSubtermsOf.test(this)) {
            Term sub = sub();
            if (sub.equals(t) || sub.containsRecursively(t, root, inSubtermsOf))
                return true;
        }
        return false;
    }


    @Override
    public Term dt(int nextDT) {
        switch (nextDT) {
            case DTERNAL:
//            case XTERNAL:
//            case 0:
                return this;
        }
        throw new UnsupportedOperationException();
    }


    @Override
    public Subterms subterms() {

        return new UnitSubterm(sub());
        //return new TermVector1(sub, this);

    }

    @Override
    public boolean impossibleSubTermVolume(int otherTermVolume) {
        return otherTermVolume > sub().volume() /* volume() -  size() */;
    }

    @Override
    public boolean isNormalized() {
        return sub().isNormalized();
    }

    @Override
    public boolean isCommutative() {
        return false;
    }

    @Override
    public int dt() {
        return DTERNAL;
    }
}
