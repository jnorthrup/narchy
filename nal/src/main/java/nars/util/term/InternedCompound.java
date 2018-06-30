package nars.util.term;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.DynBytes;
import jcog.memoize.byt.ByteKey;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.compound.LighterCompound;
import nars.term.compound.UnitCompound;

import java.util.function.Supplier;

import static nars.time.Tense.DTERNAL;

public final class InternedCompound extends ByteKey  {

    public final byte op;
    public final int dt;
    public transient Supplier<Term[]> rawSubs;

    public InternedCompound(DynBytes key, Op o, int dt, Supplier<Term[]> rawSubs) {
        super(key);
        this.op = o.id; this. dt = dt; this.rawSubs = rawSubs;
    }

    /** for look-up */
    public static InternedCompound get(Term x) {

        DynBytes key = new DynBytes(4 * x.volume() /* ESTIMATE */);
        Op o = x.op();
        key.writeByte(o.id);

        int dt = x.dt();
        if (o.temporal)
            key.writeInt(dt);
        else {
            assert(dt == DTERNAL);
        }

        if (x instanceof LighterCompound || x instanceof UnitCompound) {
            //HACK
            int s = x.subs();
            key.writeByte(s);
            for (int i = 0; i < s; i++)
                x.sub(i).appendTo((ByteArrayDataOutput) key);
        } else {
            Subterms xx = x.subterms();
            key.writeByte(xx.subs());
            xx.forEach(s -> s.appendTo((ByteArrayDataOutput) key));
        }

        return new InternedCompound(key, o, dt, x::arrayShared);
    }

    public static InternedCompound get(Op o, Term... subs) {
        return get(o, DTERNAL, subs);
    }

    public static InternedCompound get(Op o, int dt, Term... subs) {
        DynBytes key = new DynBytes(32 * subs.length /* ESTIMATE */);

        key.writeByte((o.id));

        if (o.temporal)
            key.writeInt(dt);
        else
            assert(dt==DTERNAL): "can not store temporal dt on " + o;

        int n = subs.length;
        assert(n < Byte.MAX_VALUE);
        key.writeByte(n);
        for (Term s : subs)
            s.appendTo((ByteArrayDataOutput) key);

        Supplier<Term[]> rawSubs = ()->subs;

        return new InternedCompound(key, o, dt, rawSubs);
    }


}
