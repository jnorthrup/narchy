package nars.term.compound;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import nars.Op;
import nars.The;
import nars.subterm.ArrayTermVector;
import nars.subterm.Subterms;
import nars.subterm.UniSubterm;
import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

public abstract class UnitCompound implements Compound {

    @Override
    abstract public Op op();

    protected abstract Term sub();


    @Override
    public final Term sub(int i) {
        if (i!=0)
            throw new ArrayIndexOutOfBoundsException();
        return sub();
    }


    @Override
    public int hashCode() {
        return Compound.hashCode(this);
    }

    @Override
    public boolean the() {
        return this instanceof The && sub().the();
    }

    @Override
    public boolean subIs(int i, Op o) {
        return i == 0 && sub().op()==o;
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
        return Compound.equals(this, that);
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
    public boolean containsRecursively(Term x, boolean root, Predicate<Term> inSubtermsOf) {
        if (!impossibleSubTerm(x) && inSubtermsOf.test(this)) {
            Term sub = sub();
            return (root ? sub.equalsRoot(x) : sub.equals(x)) || sub.containsRecursively(x, root, inSubtermsOf);
        }
        return false;
    }


    @Override
    public Term dt(int nextDT) {
        if (nextDT == XTERNAL) {
            if (op()==CONJ) {
                //only case it's allowed
                return CachedCompound.newCompound(CONJ, XTERNAL, new ArrayTermVector(sub()));
            }
        }
        assert(nextDT == DTERNAL);
        return this;
    }

    @Override
    public boolean hasXternal() {
        return dt()==XTERNAL || sub().hasXternal();
    }

    @Override
    public final Subterms subterms() {
        return new UniSubterm(sub());
    }

    @Override
    public @Nullable Term sub(int start, int end, byte... path) {
        if (end == start)
            return this;
        byte a = path[start];
        if (a!=0)
            return null;
        Term s = sub();
        return ((end - start) == 1) ? s : s.sub(start + 1, end, path);
    }

//    @Override
//    public Term transform(TermTransform f) {
//        Term x = sub();
//        Term y = f.transform(x);
//        if (x == y)
//            return this; //no change
//        else {
//            if (y == Null)
//                return Null;
//            else
//                return f.the(op(), DTERNAL, new Term[]{y});
//        }
//    }

//    @Override
//    public Term transform(TermTransform f, Op newOp, int newDT) {
//        if (newOp==null || (newOp==op()&&dt()==newDT))
//            return transform(f);
//        else
//            return Compound.super.transform(f, newOp, newDT);
//    }

    @Override
    public boolean impossibleSubVolume(int otherTermVolume) {
        return otherTermVolume > sub().volume() /* volume() -  size() */;
    }

    @Override
    public final boolean impossibleSubStructure(int structure) {
        return !sub().hasAll(structure);
    }

    @Override
    public final int dt() {
        return DTERNAL;
    }

    @Override
    public final boolean isNormalized() {
        return sub().isNormalized();
    }



    @Override
    public boolean eventsWhile(LongObjectPredicate<Term> events, long dt, boolean decomposeConjParallel, boolean decomposeConjDTernal, boolean decomposeXternal, int level) {
        return events.accept(dt, this);
    }

    @Override
    public void appendSubtermsTo(ByteArrayDataOutput out) {
        out.writeByte(1);
        sub().appendTo(out);
    }

    @Override
    public int volume() {
        return sub().volume()+1;
    }

    @Override
    public final int complexity() {
        return sub().complexity()+1;
    }

    @Override
    public final float voluplexity() {
        return sub().voluplexity()+1;
    }

    @Override
    public int structure() {
        return sub().structure() | opBit();
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
    public int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return reduce.intValueOf(v, sub()); 
    }

    @Override
    public boolean recurseTerms(Predicate<Term> aSuperCompoundMust, Predicate<Term> whileTrue, @Nullable Term superterm) {
        return (!aSuperCompoundMust.test(this)) || (whileTrue.test(this) && sub().recurseTerms(aSuperCompoundMust, whileTrue, this));
    }
    @Override
    public boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term,Compound> whileTrue, @Nullable Compound superterm) {
        return (!aSuperCompoundMust.test(this)) || (whileTrue.test(this,superterm) && sub().recurseTerms(aSuperCompoundMust, whileTrue, this));
    }



}
