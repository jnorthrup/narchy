package nars.util.term;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.HashCachedBytes;
import jcog.memoize.HijackMemoize;
import jcog.pri.UnitPri;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;

import java.util.Arrays;

public final class InternedSubterms extends UnitPri implements HijackMemoize.Computation<InternedSubterms, Subterms> {
    private final int hash;

    public final byte[] subs;

    public transient Term[] rawSubs;

    
    public Subterms y = null;

    public InternedSubterms(Term... subs) {
        this.rawSubs = subs;

        HashCachedBytes key = new HashCachedBytes(32 * subs.length);
        for (Term s : subs)
            s.append((ByteArrayDataOutput) key);

        this.subs = key.array();
        this.hash = key.hashCode();
    }

    @Override
    public Subterms get() {
        return y;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        
        InternedSubterms p = (InternedSubterms) obj;
        return hash == p.hash && Arrays.equals(subs, p.subs);
    }


    @Override
    public final InternedSubterms x() {
        return this;
    }

    public Subterms compute() {
        Term[] rawSubs = this.rawSubs;
        this.rawSubs = null;
        return Op.terms.subtermsInstance(rawSubs);
    }
}
