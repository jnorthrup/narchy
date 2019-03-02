package nars.term.compound;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class SeparateSubtermsCompound implements Compound {
    @Override
    public int hashCode() {
        return Compound.hashCode(this);
    }

    @Override
    public final int hashCodeSubterms() {
        return subterms().hashCodeSubterms();
    }

    /*@NotNull*/
    @Override
    public Term[] arrayClone() {
        return subterms().arrayClone();
    }

    @Override
    public Term[] arrayShared() {
        return subterms().arrayShared();
    }

    @Override
    public Term[] arrayClone(Term[] x, int from, int to) {
        return subterms().arrayClone(x, from, to);
    }

    @Override
    public int subs() {
        return subterms().subs();
    }

    @Override
    public Term sub(int i) {
        return subterms().sub(i);
    }

    @Override
    public boolean subIs(int i, Op o) {
        return subterms().subIs(i, o);
    }

    @Override
    public void forEach(/*@NotNull*/ Consumer<? super Term> action, int start, int stop) {
        subterms().forEach(action, start, stop);
    }

    @Override
    public @Nullable Term subPath(int start, int end, byte... path) {
        return end==start ? this : subterms().subSub(start, end, path);
    }

    @Override
    public Iterator<Term> iterator() {
        return subterms().iterator();
    }

    @Override
    public int subs(Op matchingOp) {
        return subterms().subs(matchingOp);
    }

    @Override
    public int subs(Predicate<Term> match) {
        return subterms().subs(match);
    }


    @Override
    public void forEach(/*@NotNull*/ Consumer<? super Term> c) {
        subterms().forEach(c);
    }

    @Override
    public boolean contains(Term t) {
        return subterms().contains(t);
    }

    @Override
    public boolean containsNeg(Term x) {
        return subterms().containsNeg(x);
    }

    @Override
    public int structure() {
        return subterms().structure() | opBit();
    }

    @Override
    public int complexity() {
        return subterms().complexity();
    }

    @Override
    public int volume() {
        return subterms().volume();
    }

    @Override
    public int varQuery() {
        return subterms().varQuery();
    }

    @Override
    public int varPattern() {
        return subterms().varPattern();
    }
    @Override
    public int varDep() {
        return subterms().varDep();
    }

    @Override
    public int varIndep() {
        return subterms().varIndep();
    }

    @Override
    public int vars() {
        return subterms().vars();
    }

    @Override
    public int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return subterms().intifyRecurse(reduce, v);
    }

    @Override
    public int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return subterms().intifyShallow(reduce, v);
    }


    @Override
    public boolean OR(/*@NotNull*/ Predicate<Term> p) {
        return subterms().OR(p);
    }

    @Override
    public boolean AND(/*@NotNull*/ Predicate<Term> p) {
        return subterms().AND(p);
    }

}
