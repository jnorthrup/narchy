package nars.util.term;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.DynBytes;
import jcog.data.byt.HashCachedBytes;
import jcog.pri.PriProxy;
import jcog.pri.UnitPri;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;

import java.util.Arrays;
import java.util.function.Supplier;

public final class InternedCompound extends UnitPri implements PriProxy<InternedCompound, Term> {

    public final byte op;
    public final int dt;
    final byte[] subs;
    private final int hash;
    public transient Supplier<Term[]> rawSubs;

    public Term y = null;

    /** for look-up */
    public InternedCompound(Compound x) {
        DynBytes key = new DynBytes(4 * x.volume() /* ESTIMATE */);
        key.writeByte((this.op = x.op().id));
        key.writeInt((this.dt = x.dt()));
        x.forEach(s -> s.appendTo((ByteArrayDataOutput) key));
        this.subs = key.array();
        this.hash = key.hashCode();
        this.rawSubs = x::arrayShared;
    }

    public InternedCompound(Op o, int dt, Term... subs) {
        HashCachedBytes key = new HashCachedBytes(32 * subs.length /* ESTIMATE */);
        key.writeByte((this.op = o.id));
        key.writeInt((this.dt = dt));
        for (Term s : subs)
            s.appendTo((ByteArrayDataOutput) key);

        this.subs = key.array();
        this.hash = key.hashCode();
        this.rawSubs = ()->subs;

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
        return hash==p.hash && Arrays.equals(subs, p.subs);
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
        return compute(Op.terms.compoundInstance(Op.ops[op], dt, this.rawSubs.get()));
    }

    public Term compute(Term computed) {
        this.rawSubs = null;
        return computed;
    }

}
