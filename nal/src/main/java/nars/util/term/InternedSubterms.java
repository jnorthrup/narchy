package nars.util.term;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.HashCachedBytes;
import jcog.memoize.HijackMemoize;
import jcog.pri.AbstractPLink;
import nars.subterm.Subterms;
import nars.term.Term;

import java.util.Arrays;

public final class InternedSubterms extends AbstractPLink<Subterms> implements HijackMemoize.Computation<InternedSubterms, Subterms> {
    private final int hash;

    final byte[] subs;

    private transient Term[] rawSubs;

    //Y
    public Subterms y = null;

    public InternedSubterms(Term... subs) {
        super();
        this.rawSubs = subs;

        HashCachedBytes key = new HashCachedBytes(4 * subs.length);
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
        //op == p.op && dt == p.dt &&
        InternedSubterms p = (InternedSubterms) obj;
        return hash == p.hash && Arrays.equals(subs, p.subs);
    }

    public float value() {
        return 0.5f;
    }

    @Override
    public InternedSubterms x() {
        return this;
    }

    public void set(Subterms y) {
        this.y = y;
    }

    public Subterms compute() {
        Term[] rawSubs = this.rawSubs;
        this.rawSubs = null;
        return Subterms.subtermsInstance(rawSubs);
    }
}
