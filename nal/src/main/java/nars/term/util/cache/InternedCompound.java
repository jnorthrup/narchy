package nars.term.util.cache;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.DynBytes;
import jcog.data.byt.util.IntCoding;
import jcog.memoize.byt.ByteKey;
import nars.IO;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.compound.UnitCompound;

import static nars.term.util.builder.InterningTermBuilder.tmpKey;
import static nars.time.Tense.DTERNAL;

public class InternedCompound extends ByteKey.ByteKeyExternal  {

    protected InternedCompound(DynBytes key) {
        super(key);
    }
    protected InternedCompound() {
        this(tmpKey());
    }

    public static final class InternedCompoundByComponents extends InternedCompound {
        public final byte op;
        public final int dt;
        public final transient Term[] subs;

        public InternedCompoundByComponents(Op o, int dt, Term... subs) {
            super();
            this.op = o.id; this.dt = dt; this.subs = subs;
            write(o, dt);
            write(subs);
            commit();
        }

        public InternedCompoundByComponents(Term x) {
            this(x.op(), x.dt(), x.arrayShared());
        }
    }

    public static final class InternedSubterms extends InternedCompound {

        public final transient Term[] subs;

        public InternedSubterms(Term[] s) {
            super();
            this.subs = s;
            write(s);
            commit();
        }
    }

    public static final class InternedCompoundTransform extends InternedCompound {
        public final Term term;

        public InternedCompoundTransform(Term x) {
            super();
            this.term = x;
            write(x);
            commit();
        }


    }

    protected void write(Term x) {
        write(x.op(), x.dt());
        if (x instanceof UnitCompound)
            write((UnitCompound)x);
        else
            write(x.subterms());
    }

    protected void write(Op o, int dt) {

        boolean temporal = o.temporal && dt != DTERNAL;
        this.key.writeByte(o.id | (temporal ? IO.TEMPORAL_BIT : 0));

        if (temporal)
            IntCoding.writeZigZagInt(dt, this.key);
        //else assert(dt==DTERNAL): "can not store temporal dt on " + o;
    }

    protected void write(Term[] subs) {
        int n = subs.length;
        //assert(n < Byte.MAX_VALUE);
        key.writeByte(n);
        for (Term s : subs)
            s.appendTo((ByteArrayDataOutput) key);
    }

    protected void write(UnitCompound u) {
        key.writeByte(1);
        u.sub().appendTo((ByteArrayDataOutput) key);
    }

    protected void write(Subterms subs) {
        int n = subs.subs();
        //assert(n < Byte.MAX_VALUE);
        key.writeByte(n);
        for (Term s : subs)
            s.appendTo((ByteArrayDataOutput) key);
    }

}
