package nars.term;

import jcog.TODO;
import jcog.WTF;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.compound.SameSubtermsCompound;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Predicate;


public class ProxyTerm implements SameSubtermsCompound {

    public final /*HACK make unpublic */ Term ref;

    public ProxyTerm(Term t) {
        if (t instanceof ProxyTerm)
            throw new WTF(t + " instanceof ProxyTerm; caught attempt to proxy a proxy in " + getClass());
        this.ref = t;
    }


    @Override
    public final boolean the() {
        return false;
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
    public final int dt() {
        return ref.dt();
    }

    @Override
    public Op op() {
        return ref.op();
    }

    @Override
    public Term unneg() {
        return ifDifferentElseThis(ref.unneg());
    }

    final Term ifDifferentElseThis(Term u) {
        if (u == ref)
            return this; //continue proxying
        else
            return u;
    }

//
//    @Override
//    public @Nullable Term replace(Map<? extends Term, Term> m) {
//        return ifDifferentElseThis(ref.replace(m));
//    }
//
//    @Override
//    public Term replace(Term from, Term to) {
//        return ifDifferentElseThis(ref.replace(from, to));
//    }

    @Override
    public @Nullable Term replaceAt(ByteList path, Term replacement) {
        throw new TODO();
    }

    @Override
    public @Nullable Term replaceAt(ByteList path, int depth, Term replacement) {
        throw new TODO();
    }

    @Override
    public @Nullable Term normalize(byte varOffset) {
        return ifDifferentElseThis(ref.normalize(varOffset));
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
        if (o instanceof ProxyTerm)
            o = ((ProxyTerm)o).ref;
        if (o instanceof Termed)
            o = ((Termed)o).term();
        return ref.equals(o);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public Term root() {
        return ifDifferentElseThis(ref.root());
    }

    @Override
    public Term concept() {
        return ifDifferentElseThis(ref.concept());
    }

    @Override
    public boolean isCommutative() {
        return ref.isCommutative();
    }

    @Override
    public void appendTo(Appendable w) throws IOException {
        ref.appendTo(w);
    }

    @Override
    public boolean isNormalized() {
        return ref.isNormalized();
    }

    @Override
    public Term sub(int i) {
        return ref.sub(i);
    }

    @Override
    public int subs() {
        return ref.subs();
    }

    @Override
    public boolean contains(Term t) {
        return ref.contains(t);
    }

    @Override
    public boolean containsRecursively(Term x, boolean root, Predicate<Term> inSubtermsOf) {
        return ref.containsRecursively(x, root, inSubtermsOf);
    }

    @Override
    public final int compareTo(Term t) {
        return ref.compareTo(t);
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

}
