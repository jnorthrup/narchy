package nars.term.util;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.DynBytes;
import jcog.memoize.byt.ByteKey;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.compound.LighterCompound;
import nars.term.compound.UnitCompound;

import java.util.function.Supplier;

import static nars.term.util.builder.InterningTermBuilder.tmpKey;
import static nars.time.Tense.DTERNAL;

public final class InternedCompound extends ByteKey.ByteKeyExternal  {

    public final byte op;
    public final int dt;
    public transient Supplier<Term[]> rawSubs;

    private InternedCompound(DynBytes key, Op o, int dt, Supplier<Term[]> rawSubs) {
        super(key);
        this.op = o.id; this. dt = dt; this.rawSubs = rawSubs;
    }

    public Term sub0() {
        return rawSubs.get()[0];
    }

    /** for look-up */
    public static InternedCompound get(Term x) {

        ByteArrayDataOutput tmp = tmpKey();

        Op o = x.op();
        tmp.writeByte(o.id);

        int dt = x.dt();
        if (o.temporal)
            tmp.writeInt(dt);
        else {
            assert(dt == DTERNAL);
        }

        if (x instanceof UnitCompound) {
            tmp.writeByte(1);
            x.sub(0).appendTo(tmp);
        } else if (x instanceof LighterCompound) {
            //HACK
            int s = x.subs();
            tmp.writeByte(s);
            for (int i = 0; i < s; i++)
                x.sub(i).appendTo(tmp);
        } else {
            Subterms xx = x.subterms();
            tmp.writeByte(xx.subs());
            xx.forEachWith(Term::appendTo, tmp);
        }

        return new InternedCompound((DynBytes) tmp, o, dt, x::arrayShared);
    }

    public static InternedCompound get(Op o, Term... subs) {
        return get(o, DTERNAL, subs);
    }

    public static InternedCompound get(Op o, int dt, Term... subs) {
        DynBytes key = tmpKey();

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
