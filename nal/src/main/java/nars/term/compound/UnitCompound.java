package nars.term.compound;

import nars.The;
import nars.subterm.Subterms;
import nars.subterm.UniSubterm;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.builder.TermBuilder;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

public abstract class UnitCompound implements SameSubtermsCompound {

    protected abstract Term sub();

    public final Term[] arrayClone() {
        return new Term[]{sub()};
    }

    @Override
    public final Term sub(int i) {
        if (i!=0)
            throw new ArrayIndexOutOfBoundsException();
        return sub();
    }

    @Override
    public Term sub(int i, Term ifOutOfBounds) {
        if (i!=0)
            return ifOutOfBounds;
        return sub();
    }


    @Override
    public boolean the() {
        return this instanceof The && sub().the();
    }


    @Override
    public boolean containsNeg(Term x) {
        return sub().equalsNeg(x);
    }

    @Override
    public int hashCode() {
        return Compound.hash(this);
    }

    @Override
    public final int hashCodeSubterms() {
        return Subterms.hash(sub());
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
        return Compound.equals(this, that, false);
    }

    @Override
    public final boolean ANDrecurse(Predicate<Term> p) {
        if (!p.test(this)) return false;
        Term s = sub();
        return s instanceof Compound ? ((Compound) s).ANDrecurse(p) : p.test(s);
    }
    @Override
    public final boolean ORrecurse(Predicate<Term> p) {
        if (p.test(this)) return true;
        Term s = sub();
        return s instanceof Compound ? ((Compound) s).ORrecurse(p) : p.test(s);
    }

    @Override
    public Term dt(int nextDT, TermBuilder builder) {
        if (nextDT == XTERNAL) {
            //only case it's allowed
            assert(opID()==CONJ.id);
            return builder.newCompound(CONJ, XTERNAL, sub());
        }
        assert(nextDT == DTERNAL);
        return this;
    }

    @Override
    public final Subterms subterms() {
        return new UniSubterm(sub());
    }

    @Override
    public @Nullable Term subPath(int start, int end, byte... path) {
        if (end == start)
            return this;
        byte a = path[start];
        if (a!=0)
            return null;
        Term s = sub();
        return ((end - start) == 1) ? s : s.subPath(start + 1, end, path);
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
    public final int dt() {
        return DTERNAL;
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
    public final int structureSurface() {
        return sub().opBit();
    }

    @Override
    public boolean hasVars() {
        return sub().hasVars();
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
    public int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
        return reduce.intValueOf(v, sub()); 
    }

    @Override
    public int intifyRecurse(int v, IntObjectToIntFunction<Term> reduce) {
        return reduce.intValueOf(sub().intifyRecurse(v, reduce), this);
    }

    @Override
    public boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound superterm) {
        return !inSuperCompound.test(this) || (whileTrue.test(this) && sub().recurseTerms(inSuperCompound, whileTrue, this));
    }
    @Override
    public boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term,Compound> whileTrue, @Nullable Compound superterm) {
        return !aSuperCompoundMust.test(this) || (whileTrue.test(this,superterm) && sub().recurseTerms(aSuperCompoundMust, whileTrue, this));
    }

    @Override
    public boolean recurseTermsOrdered(Predicate<Term> aSuperCompoundMust, Predicate<Term> whileTrue, Compound superterm) {
        return !aSuperCompoundMust.test(this) || (whileTrue.test(this) && sub().recurseTermsOrdered(aSuperCompoundMust, whileTrue, this));
    }

//    @Override
//    public boolean unifySubterms(Compound y, Unify u) {
//        return y.subs()==1 && sub().unify(y.sub(0), u);
//    }

//    @Override
//    public Term transform(RecursiveTermTransform f, Op newOp, int ydt) {
//        Term x = sub();
//        Term y = x.transform(f);
//        if (y!=x) {
//            if (y == Null)
//                return Null;
//            else
//                return f.compound(op(), DTERNAL, new Term[] { y });// (newOp == null ? op() : newOp).the(y);
//        } else
//            return this;
//    }
}
