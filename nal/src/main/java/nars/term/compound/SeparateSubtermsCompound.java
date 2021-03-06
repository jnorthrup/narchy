package nars.term.compound;

import jcog.data.list.FasterList;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.*;
import java.util.stream.Stream;

import static nars.time.Tense.XTERNAL;

/** delegates certain methods to a specific impl */
public abstract class SeparateSubtermsCompound implements Compound {

    private final int hash;

//    protected SeparateSubtermsCompound(Op o, Subterms x) {
//        this(x.hashWith(o.id));
//    }

    SeparateSubtermsCompound(byte op, Subterms x) {
        this(x.hashWith((int) op));
    }

    SeparateSubtermsCompound(int hash) {
        this.hash = hash;
    }

    @Override
    public boolean isNormalized() {
        return subterms().isNormalized();
    }

    public final int subStructure() {
        return subterms().structure();
    }


    @Override
    public String toString() {
        return Compound.toString(this);
    }

    @Override
    public final boolean equals(Object obj) {
        return Compound.equals(this, obj,true);
    }

    @Override
    public final int hashCode() {
        //return Compound.hashCode(this);
        return hash;
    }

    @Override
    public final int hashCodeSubterms() {
        return subterms().hashCodeSubterms();
    }

    @Override
    public boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound superterm) {
        return !inSuperCompound.test(this) ||
                whileTrue.test(this) && subterms().recurseTerms(inSuperCompound, whileTrue, this);
    }

    @Override
    public boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return !inSuperCompound.test(this) ||
                whileTrue.test(this) &&
                subterms().recurseTermsOrdered(inSuperCompound, whileTrue, this);
    }

    @Override
    public boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm) {
        return !aSuperCompoundMust.test(this) ||
                whileTrue.test(this, superterm) && subterms().recurseTerms(aSuperCompoundMust, whileTrue, this);
    }

    public boolean subtermsContainsRecursively(Term x, boolean root, Predicate<Term> inSubtermsOf) {
        return subterms().containsRecursively(x, root, inSubtermsOf);
    }



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
    public final Stream<Term> subStream() {
        return subterms().subStream();
    }

    @Override
    public final int count(Op matchingOp) {
        return subterms().count(matchingOp);
    }

    @Override
    public final int count(Predicate<Term> match) {
        return subterms().count(match);
    }

    @Override
    public final void forEach( Consumer<? super Term> c) {
        for (Term term : subterms())
            c.accept(term);
    }
    @Override
    public final void forEach( Consumer<? super Term> action, int start, int stop) {
        subterms().forEach(action, start, stop);
    }
    @Override
    public final void forEachI(ObjectIntProcedure<Term> t) {
        subterms().forEachI(t);
    }

    @Override
    public final <X> void forEachWith(BiConsumer<Term, X> t, X argConst) {
        subterms().forEachWith(t, argConst);
    }

    @Override
    public int addAllTo(Term[] t, int offset) {
        return subterms().addAllTo(t, offset);
    }

    @Override
    public void addAllTo(Collection target) {
        subterms().addAllTo(target);
    }

    @Override
    public void addAllTo(FasterList target) {
        subterms().addAllTo(target);
    }

    @Override
    public int indexOf(Term t, int after) {
        return subterms().indexOf(t, after);
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
    public boolean hasXternal() {
        return dt()==XTERNAL || subterms().hasXternal();
    }

    @Override
    public int structure() {
        return subStructure() | opBit();
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
    public final int intifyRecurse(int v, IntObjectToIntFunction<Term> reduce) {
        return subterms().intifyRecurse(v, reduce);
    }

    @Override
    public final int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
        return subterms().intifyShallow(v, reduce);
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
    public final @Nullable Subterms transformSubs(UnaryOperator<Term> f, Op superOp) {
        return subterms().transformSubs(f, superOp);
    }

    @Override
    public boolean OR( Predicate<Term> p) {
        return subterms().OR(p);
    }

    @Override
    public boolean AND( Predicate<Term> p) {
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
    public <X> boolean ANDwithOrdered(BiPredicate<Term, X> p, X param) {
        return subterms().ANDwithOrdered(p, param);
    }


    @Override
    public boolean ANDi(ObjectIntPredicate<Term> p) {
        return subterms().ANDi(p);
    }
    @Override
    public boolean ORi(ObjectIntPredicate<Term> p) {
        return subterms().ORi(p);
    }
}
