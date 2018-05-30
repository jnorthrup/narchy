package nars.util.term;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.HashCachedBytes;
import jcog.memoize.HijackMemoize;
import jcog.pri.UnitPri;
import nars.Op;
import nars.term.Term;

import java.util.Arrays;

public final class InternedCompound extends UnitPri implements HijackMemoize.Computation<InternedCompound, Term> {
    
    public final Op op;
    public final int dt;
    private final int hash;

    
    final byte[] subs;

    public transient Term[] rawSubs;

    
    public Term y = null;

    public InternedCompound(Op o, int dt, Term... subs) {
        this.op = o;
        this.dt = dt;
        this.rawSubs = subs;

        HashCachedBytes key = new HashCachedBytes(4 * subs.length);
        key.writeByte(o.id);
        key.writeInt(dt);
        for (Term s : subs)
            s.append((ByteArrayDataOutput) key);

        this.subs = key.array();
        this.hash = key.hashCode();
    }

    @Override
    public Term get() {
        return y;
    }

    




















    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        
        InternedCompound p = (InternedCompound) obj;
        return hash == p.hash && Arrays.equals(subs, p.subs);
    }

    public float value() {
        return 0.5f;














        
    }

    @Override
    public InternedCompound x() {
        return this;
    }

    public void set(Term y) {
        this.y = y;



















    }

    public Term compute() {
        return compute(Op.terms.compoundInstance(op, dt, this.rawSubs));
    }

    public Term compute(Term computed) {
        this.rawSubs = null;
        return computed;
    }

}
