package nars.term.compound;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.transform.TermTransform;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** delegates certain methods to a specific impl */
public abstract class SeparateSubtermsCompound implements Compound {

    @Override
    public String toString() {
        return Compound.toString(this);
    }

    @Override
    public boolean equals(Object obj) {
        return Compound.equals(this, obj,true);
    }

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
    public final int subs() {
        return subterms().subs();
    }

    @Override
    public final Term sub(int i) {
        return subterms().sub(i);
    }

    @Override
    public final Term sub(int i, Term ifOutOfBounds) {
        return subterms().sub(i, ifOutOfBounds);
    }
    @Override
    public final boolean subIs(int i, Op o) {
        return subterms().subIs(i, o);
    }

    @Override
    public final boolean subIsOrOOB(int i, Op o) {
        return subterms().subIsOrOOB(i, o);
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
    public int count(Op matchingOp) {
        return subterms().count(matchingOp);
    }

    @Override
    public int count(Predicate<Term> match) {
        return subterms().count(match);
    }

    @Override
    public final void forEach(/*@NotNull*/ Consumer<? super Term> c) {
        subterms().forEach(c);
    }
    @Override
    public final void forEach(/*@NotNull*/ Consumer<? super Term> action, int start, int stop) {
        subterms().forEach(action, start, stop);
    }
    @Override
    public final void forEachWith(ObjectIntProcedure<Term> t) {
        subterms().forEachWith(t);
    }

    @Override
    public final <X> void forEachWith(BiConsumer<Term, X> t, X argConst) {
        subterms().forEachWith(t, argConst);
    }

    @Override
    public final boolean contains(Term t) {
        return subterms().contains(t);
    }

    @Override
    public final boolean containsNeg(Term x) {
        return subterms().containsNeg(x);
    }

    @Override
    public final boolean containsAny(Subterms ofThese) {
        return subterms().containsAny(ofThese);
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
    public final int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return subterms().intifyRecurse(reduce, v);
    }

    @Override
    public final int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return subterms().intifyShallow(reduce, v);
    }

    @Override
    public final @Nullable Term subSub(int start, int end, byte[] path) {
        return subterms().subSub(start, end, path);
    }

    @Override
    public final @Nullable Term subSub(byte[] path) {
        return subterms().subSub(path);
    }

    @Override
    public final @Nullable Term subSubUnsafe(int start, int end, byte[] path) {
        return subterms().subSubUnsafe(start, end, path);
    }

    @Override
    public final @Nullable Subterms transformSubs(TermTransform f, Op superOp) {
        return subterms().transformSubs(f, superOp);
    }

    @Override
    public boolean OR(/*@NotNull*/ Predicate<Term> p) {
        return subterms().OR(p);
    }

    @Override
    public boolean AND(/*@NotNull*/ Predicate<Term> p) {
        return subterms().AND(p);
    }

    @Override
    public <X> boolean ORwith(BiPredicate<Term, X> p, X param) {
        return subterms().ORwith(p, param);
    }

    @Override
    public <X> boolean ANDwith(BiPredicate<Term, X> p, X param) {
        return subterms().ANDwith(p, param);
    }

    @Override
    public boolean ANDith(ObjectIntPredicate<Term> p) {
        return subterms().ANDith(p);
    }
    @Override
    public boolean ORith(ObjectIntPredicate<Term> p) {
        return subterms().ORith(p);
    }
}
