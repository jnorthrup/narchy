package nars.term.util.cache;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.DynBytes;
import jcog.data.byt.util.IntCoding;
import jcog.memoize.byt.ByteKey;
import nars.IO;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;

import static jcog.data.byt.RecycledDynBytes.tmpKey;
import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;

/** interned terms and subterms implementations */
public class Intermed extends ByteKey.ByteKeyExternal  {

    protected Intermed(DynBytes key) {
        super(key);
    }
    protected Intermed() {
        this(tmpKey());
    }

    public abstract static class InternedCompoundByComponents extends Intermed {
        public final byte op;
        public final int dt;

        public InternedCompoundByComponents(Op o, int dt) {
            super();
            assert(o!=NEG);
            this.op = o.id; this.dt = dt;
            write(o, dt);
        }

        public abstract Term[] subs();

    }

    public static final class InternedCompoundByComponentsArray extends InternedCompoundByComponents {
        public final transient Term[] subs;

        public InternedCompoundByComponentsArray(Op o, int dt, Term... subs) {
            super(o, dt);
            this.subs = subs;
            write(subs);
            commit();
        }

        @Override
        public Term[] subs() {
            return subs;
        }
    }

    public static final class InternedCompoundByComponentsSubs extends InternedCompoundByComponents {

        private final Term x;

        public InternedCompoundByComponentsSubs(Term x) {
            super(x.op(), x.dt());
            this.x = x;
            write(x.subterms());
            commit();
        }
        @Override
        public Term[] subs() {
            return x.arrayShared();
        }
    }

    public static final class InternedSubterms extends Intermed {

        public final transient Term[] subs;

        public InternedSubterms(Term[] s) {
            super();
            this.subs = s;
            write(s);
            commit();
        }
    }

    public static final class InternedCompoundTransform extends Intermed {
        public final Term term;

        public InternedCompoundTransform(Term x) {
            super();
            this.term = x;

            x.appendTo((ByteArrayDataOutput) key);
            //write(x);
            commit();
        }


    }

//    protected void write(Term x) {
//        write(x.op(), x.dt());
//        if (x instanceof UnitCompound) {
//            key.writeByte(1);
//            write(x.sub(0)); //.appendTo((ByteArrayDataOutput) key);
//        }
//        else
//            write(x.subterms());
//    }

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

    protected void write(Subterms subs) {
        subs.appendTo(key);

//        int n = subs.subs();
        //assert(n < Byte.MAX_VALUE);
        //key.writeByte(n);
        //subs.forEachWith((s,z) -> s.appendTo(z), (ByteArrayDataOutput)key);

//        for (Term s : subs) {
//            s.appendTo((ByteArrayDataOutput) key);
//        }
    }

    public static class SubtermsKey extends ByteKeyExternal {
        public final Subterms subs;
    
        public SubtermsKey(Subterms s) {
            super(tmpKey());
            s.appendTo(key);
            this.subs = s;
            commit();
        }
    }
}
