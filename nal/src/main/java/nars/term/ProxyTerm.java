package nars.term;

import jcog.WTF;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.atom.Bool;
import nars.term.compound.SameSubtermsCompound;
import nars.term.util.TermException;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;


public abstract class ProxyTerm implements SameSubtermsCompound {

    public final /*HACK make unpublic */ Term ref;

    public ProxyTerm(Term t) {
        if (t == null)
            throw new NullPointerException();

        if (t instanceof ProxyTerm)
            throw new WTF(t + " instanceof ProxyTerm; caught attempt to proxy a proxy in " + getClass());

        if (t instanceof Bool)
            throw new TermException("Proxy to BOOL", t);

        this.ref = t;
    }


    @Override
    public abstract boolean the();

    @Override
    public abstract String toString();

    @Override
    public abstract Subterms subterms();

    @Override
    public abstract Subterms subtermsDirect();

    @Override
    public abstract int dt();

    @Override
    public abstract Op op();

    @Override
    public abstract Term unneg();

    public abstract Term ifDifferentElseThis(Term u);

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
    public @Nullable
    abstract Term replaceAt(ByteList path, Term replacement);

    @Override
    public @Nullable
    abstract Term replaceAt(ByteList path, int depth, Term replacement);

    @Override
    public @Nullable
    abstract Term normalize(byte varOffset);

    @Override
    public abstract int volume();

    @Override
    public abstract int complexity();

    @Override
    public abstract int structure();


    @Override public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract Term root();

    @Override
    public abstract Term concept();

    @Override
    public abstract boolean isCommutative();

    @Override
    public abstract void appendTo(Appendable w) throws IOException;

    @Override
    public abstract boolean isNormalized();

    @Override
    public abstract Term sub(int i);

    @Override
    public abstract int subs();

    @Override
    public abstract int compareTo(Term t);

    @Override
    public abstract int vars();

    @Override
    public abstract int varIndep();

    @Override
    public abstract int varDep();

    @Override
    public abstract int varQuery();

    @Override
    public abstract int varPattern();

}
