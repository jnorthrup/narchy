package nars.term.compound;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;

/** use with extreme caution when op is not PROD */
public class LightCompound extends SeparateSubtermsCompound implements AbstractLightCompound {

    private final Subterms subs;
    private final int hash;
    private final byte op;


    public LightCompound(Op o, Term... s) {
        this(o, $.vFast(s));
    }

    protected LightCompound(byte o, Term... s) {
        this(o, $.vFast(s));
    }

    public LightCompound(Op o, Subterms s) {
        this(o.id, s);
    }

    protected LightCompound(byte o, Subterms s) {
        this.op = o;
        this.subs = s;
        this.hash = s.hashWith(o);
    }


    @Override
    public boolean equals(Object obj) {
        return Compound.equals(this, obj,true);
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    @Override
    public String toString() {
        return Compound.toString(this);
    }

    @Override
    public final Op op() {
        return Op.ops[op];
    }


    @Override
    public final Subterms subterms() {
        return subs;
    }



}
