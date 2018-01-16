package nars.term;

import com.google.common.io.ByteArrayDataOutput;
import nars.Op;
import nars.index.term.TermContext;
import nars.term.sub.Subterms;
import nars.term.subst.Unify;
import nars.term.transform.Retemporalize;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;


public class ProxyTerm<T extends Term> implements Term, CompoundDT {

    public final /*HACK make unpublic */ T ref;

    public ProxyTerm(T t) {
        this.ref = t;
    }

    @Override
    public String toString() {
        return ref.toString();
    }

    @Override
    public final Subterms subterms() {
        return ref.subterms();
    }

    @Override
    public @Nullable Term temporalize(Retemporalize r) {
        return ref.temporalize(r);
    }

    @Override
    public boolean isTemporal() {
        return ref.isTemporal();
    }

    @Override
    public final int dt() {
        return ref.dt();
    }

//    @Override
//    public final Term term() {
//        return this;
//    }

    @Override
    public Op op() {
        return ref.op();
    }

    @Override
    public int volume() {
        return ref.volume();
    }

    @Override
    public int complexity() {
        return ref.complexity();
    }

    @Override
    public int structure() {
        return ref.structure();
    }


    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ProxyTerm) {
            o = ((ProxyTerm)o).ref;
        }
        return ref.equals(o);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }


    @Override
    public Term root() {
        return ref.root();
    }

    @Override
    public Term conceptual() {
        return ref.conceptual();
    }
    
    @Override
    public boolean isCommutative() {
        return ref.isCommutative();
    }

    @Override
    public void append(ByteArrayDataOutput out) {
        ref.append(out);
    }

    @Override
    public boolean unify(Term y, Unify subst) {
        return ref.unify(y, subst);
    }

    @Override
    public boolean isAny(int bitsetOfOperators) {
        return ref.isAny(bitsetOfOperators);
    }

    @Override
    public boolean hasVarIndep() {
        return ref.hasVarIndep();
    }

    @Override
    public boolean hasVarDep() {
        return ref.hasVarDep();
    }

    @Override
    public boolean hasVarQuery() {
        return ref.hasVarQuery();
    }

    @Override
    public void append(Appendable w) throws IOException {
        ref.append(w);
    }



    @Override
    public boolean isNormalized() {
        return ref.isNormalized();
    }

    @Override
    public @Nullable byte[] pathTo(Term subterm) {
        return ref.pathTo(subterm);
    }

    @Override
    public ByteList structureKey() {
        return ref.structureKey();
    }

    @Override
    public ByteList structureKey(ByteArrayList appendTo) {
        return ref.structureKey(appendTo);
    }


    @Override
    public int opX() {
        return ref.opX();
    }

    @Override
    public Term evalSafe(TermContext context, int remain) {
        return ref.evalSafe(context, remain);
    }

    @Override
    public Term dt(int dt) {
        return ref.dt(dt);
    }


    @Override
    public int subs() {
        return ref.subs();
    }

    @Override
    public boolean contains(Term t) {
        return ref.contains(t);
    }

//    @Override
//    public boolean impossibleSubTerm(Termlike target) {
//        return ref.impossibleSubTerm(target);
//    }
//
//    @Override
//    public boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
//        return ref.impossibleSubTermOrEqualityVolume(otherTermsVolume);
//    }

    @Override
    public Term sub(int i, Term ifOutOfBounds) {
        return ref.sub(i, ifOutOfBounds);
    }

    @Override
    public boolean impossibleSubTermVolume(int otherTermVolume) {
        return ref.impossibleSubTermVolume(otherTermVolume);
    }

    @Override
    public boolean impossibleSubTermOrEquality(Term target) {
        return ref.impossibleSubTermOrEquality(target);
    }

    @Override
    public boolean AND(Predicate<Term> v) {
        return ref.AND(v);
    }

    @Override
    public boolean ANDrecurse(Predicate<Term> v) {
        return ref.ANDrecurse(v);
    }

    @Override
    public void recurseTerms(Consumer<Term> v) {
        ref.recurseTerms(v);
    }

    @Override
    public boolean OR(Predicate<Term> v) {
        return ref.OR(v);
    }

    @Override
    public boolean ORrecurse(Predicate<Term> v) {
        return ref.ORrecurse(v);
    }

    @Override
    public int vars() {
        return ref.vars();
    }

    @Override
    public int varIndep() {
        return ref.varIndep();
    }

    @Override
    public int varDep() {
        return ref.varDep();
    }

    @Override
    public int varQuery() {
        return ref.varQuery();
    }

    @Override
    public int varPattern() {
        return ref.varPattern();
    }

//    @Override
//    public boolean isDynamic() {
//        return ref.isDynamic();
//    }


//    @Override
//    public int vars(@Nullable Op type) {
//        return ref.vars(type);
//    }

}
