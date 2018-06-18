package nars.term.compound;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.UnitSubterm;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Terms;
import nars.unify.Unify;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.time.Tense.DTERNAL;

public abstract class UnitCompound implements Compound {

    @Override
    abstract public Op op();

    abstract public Term sub();


    @Override
    public final Term sub(int i) {
        if (i!=0) throw new ArrayIndexOutOfBoundsException();
        return sub();
    }

    @Override
    public int hashCode() {
         return Util.hashCombine(
                 hashCodeSubterms(),
                 op().id);
    }

    @Override
    public boolean subIs(int i, Op o) {
        return sub().op()==o;
    }

    @Override
    public boolean contains(Term t) {
        return sub().equals(t);
    }

    @Override
    public boolean containsNeg(Term x) {
        return sub().equalsNeg(x);
    }

    @Override
    public boolean containsRoot(Term x) {
        return sub().equalsRoot(x);
    }

    @Override
    public final int hashCodeSubterms() {
        return Util.hashCombine1(sub());
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
    public boolean equals(@Nullable Object that) {
        if (this == that) return true;
        if (that instanceof Term) {
            Term x = (Term) that;
            return hashCode() == that.hashCode() && opX() == x.opX() && x.subs() == 1 && sub().equals(x.sub(0));
        }
        return false;
    }

    @Override
    public boolean ANDrecurse(Predicate<Term> p) {
        return p.test(this) && p.test(sub());
    }
    @Override
    public boolean ORrecurse(Predicate<Term> p) {
        return p.test(this) || p.test(sub());
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
    public final boolean unifySubterms(Term y, Unify u) {
        if ((y.subs()!=1) || !Terms.commonStructureTest(this, y, u))
            return false;
        return sub().unify(y.sub(0), u);
    }


    @Override
    public Term dt(int nextDT) {
        assert(nextDT == DTERNAL);
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
    public boolean hasXternal() {
        return sub().hasXternal();
    }

    @Override
    public final int dt() {
        return DTERNAL;
    }

    @Override
    public boolean isNormalized() {
        Term s = sub();
        switch (s.op()) {
            case ATOM: return true;
            case VAR_PATTERN: return s == $.varPattern(1);
            case VAR_DEP: return s == $.varDep(1);
            case VAR_INDEP: return s == $.varIndep(1);
            case VAR_QUERY: return s == $.varQuery(1);
        }
        return s.isNormalized();
    }



    @Override
    public int eventCount() {
        return 1;
    }

    @Override
    public boolean eventsWhile(LongObjectPredicate<Term> events, long dt, boolean decomposeConjParallel, boolean decomposeConjDTernal, boolean decomposeXternal, int level) {
        return events.accept(dt, this);
    }

    @Override
    public int dtRange() {
        return 0;
    }

    @Override public void appendTo(ByteArrayDataOutput out) {

        Op o = op();
        out.writeByte(o.id);

        
        out.writeByte(1); 
        sub().appendTo(out);

        if (o.temporal)
            out.writeInt(dt()); 

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

    @Override
    public int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return reduce.intValueOf(v, sub()); 
    }

    @Override
    public boolean recurseTerms(Predicate<Term> aSuperCompoundMust, Predicate<Term> whileTrue, @Nullable Term parent) {
        return (!aSuperCompoundMust.test(this)) || (sub().recurseTerms(aSuperCompoundMust, whileTrue, this));
    }



}
