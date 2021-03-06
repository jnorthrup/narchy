package nars.term.compound;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;

import static nars.time.Tense.DTERNAL;

/** use with extreme caution when op is not PROD */
public class LightCompound extends SeparateSubtermsCompound  {

    private final Subterms subs;
    private final byte op;


    public LightCompound(Op o, Term... s) {
        this(o.id, s);
    }

    private LightCompound(byte o, Term... s) {
        this(o, $.INSTANCE.vFast(s));
    }

    public LightCompound(Op o, Subterms s) {
        this(o.id, s);
    }

    protected LightCompound(byte o, Subterms s) {
        super(o, s);
        this.op = o;
        this.subs = s;
    }

    @Override
    public boolean the() {
        return false;
    }

    @Override
    public int dt() {
        return DTERNAL;
    }


    @Override
    public final Op op() {
        return Op.the((int) op);
    }


    @Override
    public final Subterms subterms() {
        return subs;
    }



}
