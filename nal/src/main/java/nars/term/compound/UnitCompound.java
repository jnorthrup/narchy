package nars.term.compound;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.UnitSubterm;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.util.time.Tense.DTERNAL;

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
    public boolean contains(Term t) {
        return sub().equals(t);
    }

    @Override
    public boolean containsRoot(Term x) {
        return sub().equalsRoot(x);
    }

    @Override
    public final int hashCodeSubterms() {
        return Util.hashCombine1(sub().hashCode());
    }


    @Override
    public final int subs() {
        return 1;
    }

    @Override
    public String toString() {
        return Compound.toString(this);
    }

    @Override
    public final boolean equals(@Nullable Object that) {
        if (this == that) return true;
        if (that instanceof Term) {
            Term x = (Term) that;
            return x.subs() == 1 && hashCode() == that.hashCode() && opX() == x.opX() && sub().equals(x.sub(0));
        }
        return false;
    }

    @Override
    public void recurseTerms(/*@NotNull*/ Consumer<Term> v) {
        v.accept(this);
        sub().recurseTerms(v);
    }

    @Override
    public boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        if (!impossibleSubTerm(t) && inSubtermsOf.test(this)) {
            Term sub = sub();
            return (root ? sub.equalsRoot(t) : sub.equals(t)) || sub.containsRecursively(t, root, inSubtermsOf);
        }
        return false;
    }


    @Override
    public Term dt(int nextDT) {
        if (nextDT!=DTERNAL) throw new UnsupportedOperationException();
        return this;
    }

    @Override
    public boolean isTemporal() {
        return sub().isTemporal();
    }

    @Override
    public Subterms subterms() {
        return new UnitSubterm(sub());
    }

    @Override
    public boolean impossibleSubTermVolume(int otherTermVolume) {
        return otherTermVolume > sub().volume() /* volume() -  size() */;
    }

    @Override
    public boolean impossibleSubTerm(Termlike target) {
        Term sub = sub();
        return !sub.hasAll(target.structure()) || impossibleSubTermVolume(target.volume());
    }

    @Override
    public boolean isNormalized() {
        return sub().isNormalized();
    }

    @Override
    public final boolean isCommutative() {
        return false;
    }

    @Override
    public final int dt() {
        return DTERNAL;
    }

    @Override
    public int eventCount() {
        return 1;
    }

    @Override
    public boolean eventsWhile(LongObjectPredicate<Term> events, long offset, boolean decomposeConjParallel, boolean decomposeConjDTernal, boolean decomposeXternal, int level) {
        return events.accept(offset, this);
    }

    @Override
    public int dtRange() {
        return 0;
    }

    @Override public void append(ByteArrayDataOutput out) {

        Op o = op();
        out.writeByte(o.id);

        //avoids creating temporary Subterms instance:
        out.writeByte(1); //one subterm
        sub().append(out);

        if (o.temporal)
            out.writeInt(dt()); //can happen if ellipsis term

    }

    @Override
    public int volume() {
        return sub().volume()+1;
    }

    @Override
    public int complexity() {
        return sub().complexity()+1;
    }

    @Override
    public int structure() {
        return sub().structure() | op().bit;
    }

    @Override
    public int varPattern() {
        return sub().varPattern();
    }

    @Override
    public int varDep() {
        return sub().varDep();
    }

    @Override
    public int varIndep() {
        return sub().varIndep();
    }

    @Override
    public int varQuery() {
        return sub().varQuery();
    }

    @Override
    public int vars() {
        return sub().vars();
    }

    @Override
    public boolean hasVars() {
        return sub().hasVars();
    }
}
