package nars.term.control;

import jcog.TODO;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.ProxyTerm;
import nars.term.Term;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Created by me on 4/21/17.
 */
public abstract class AbstractPred<X> extends ProxyTerm implements PREDICATE<X> {

    protected AbstractPred(Term term) {
        super(term);
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
    public Subterms subtermsDirect() {
        return ((Compound)ref).subtermsDirect();
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

     public final Term ifDifferentElseThis(Term u) {
		//continue proxying
		return u == ref ? this : u;
    }

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
//        if (o instanceof Termed)
//            o = ((Termed)o).term();
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
