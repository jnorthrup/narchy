package nars.term.util;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.DynBytes;
import jcog.data.byt.util.IntCoding;
import jcog.memoize.byt.ByteKey;
import nars.IO;
import nars.Op;
import nars.term.Term;

import static nars.term.util.builder.InterningTermBuilder.tmpKey;
import static nars.time.Tense.DTERNAL;

public final class InternedCompound extends ByteKey.ByteKeyExternal  {

    public final byte op;
    public final int dt;

    public final transient Term[] rawSubs;

    private InternedCompound(DynBytes key, Op o, int dt, Term[] rawSubs) {
        super(key);
        this.op = o.id; this.dt = dt; this.rawSubs = rawSubs;
    }
    public static InternedCompound get(Op o, Term... subs) {
        return get(o, DTERNAL, subs);
    }

    public Term sub0() {
        return rawSubs[0];
    }

    //TODO conslidate the following two highly similar procedures
    /** for look-up */
    public static InternedCompound get(Term x) {

        return get(x.op(), x.dt(), x.arrayShared());
//        ByteArrayDataOutput out = tmpKey();
//
//        Op o = x.op();
//
//        int dt = x.dt();
//
//        boolean temporal = o.temporal && dt!=DTERNAL;
//        out.writeByte(o.id | (temporal ? IO.TEMPORAL_BIT : 0));
//
//
//        if (x instanceof UnitCompound) {
//            out.writeByte(1);
//            x.sub(0).appendTo(out);
//        } else if (x instanceof LighterCompound) {
//            //HACK
//            int s = x.subs();
//            out.writeByte(s);
//            for (int i = 0; i < s; i++)
//                x.sub(i).appendTo(out);
//        } else {
//            Subterms xx = x.subterms();
//            out.writeByte(xx.subs());
//            xx.forEachWith(Term::appendTo, out);
//        }
//
//        if (temporal)
//            IntCoding.writeZigZagInt(dt, out);
//        //else assert(dt == DTERNAL);
//
//        return new InternedCompound((DynBytes) out, o, dt, x.arrayShared());
    }


    public static InternedCompound get(Op o, int dt, Term... subs) {
        DynBytes out = tmpKey();

        boolean temporal = o.temporal && dt!=DTERNAL;
        out.writeByte(o.id | (temporal ? IO.TEMPORAL_BIT : 0));


        int n = subs.length;
        //assert(n < Byte.MAX_VALUE);
        out.writeByte(n);
        for (Term s : subs)
            s.appendTo((ByteArrayDataOutput) out);

        if (temporal)
            IntCoding.writeZigZagInt(dt, out);
        //else assert(dt==DTERNAL): "can not store temporal dt on " + o;

        return new InternedCompound(out, o, dt, subs);
    }



}
