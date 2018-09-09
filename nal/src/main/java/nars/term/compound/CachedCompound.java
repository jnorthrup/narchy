package nars.term.compound;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import jcog.data.byt.DynBytes;
import nars.IO;
import nars.Op;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;


/**
 * on-heap, caches many commonly used methods for fast repeat access while it survives
 */
abstract public class CachedCompound implements SeparateSubtermsCompound, The {

    /**
     * subterm vector
     */
    private final Subterms subterms;


    /**
     * content hash
     */
    protected final int hash;

    private final byte op;

    private final short _volume;
    private final int _structure;

    public static class SimpleCachedCompound extends CachedCompound {

        public SimpleCachedCompound(Op op, Subterms subterms) {
            super(op, DTERNAL, subterms);
        }

        @Override
        public final boolean equalsRoot(Term x) {
            //return x instanceof SimpleCachedCompound ? equals(x) : (!x.hasAny(Op.Temporal) ? false : equals(x.root()));
            return equals(x);
        }

        @Override
        public final boolean equalsNegRoot(Term x) {
            //return x instanceof SimpleCachedCompound ? equalsNeg(x) : equalsNeg(x.root());
            return equalsNeg(x);
        }

        @Override
        public final Term root() {
            return this;
        }

        @Override
        public final Term concept() {
            return this;
        }

        @Override
        public final boolean hasXternal() {
            return false;
        }


        @Override
        public final int eventCount() {
            return 1;
        }

        @Override
        public final int eventRange() {
            return 0;
        }

        @Override
        public final int subTimeOnly(Term event) {
            return equals(event) ? 0 : DTERNAL;
        }

        @Override
        public final int dt() {
            return DTERNAL;
        }

    }

    public static final class SimpleCachedCompoundWithBytes extends SimpleCachedCompound {

        final byte[] key;

        public SimpleCachedCompoundWithBytes(Op op, Subterms subterms) {
            this(op, subterms, null);
        }

        public SimpleCachedCompoundWithBytes(Op op, Subterms subterms, @Nullable byte[] knownKey) {
            super(op, subterms);

            if (knownKey == null) {
                DynBytes d = new DynBytes(IO.termBytesEstimate(subterms) + 1);
                super.appendTo((ByteArrayDataOutput) d);
                key = d.array();
            } else {
                key = knownKey;
            }
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) return true;
            if (that instanceof SimpleCachedCompoundWithBytes) {
                SimpleCachedCompoundWithBytes tha = ((SimpleCachedCompoundWithBytes) that);
                return hash == tha.hash && Arrays.equals(key, tha.key);
            } else {
                return Compound.equals(this, that);
            }
        }

        @Override
        public void appendTo(ByteArrayDataOutput out) {
            out.write(key);
        }
    }


    /**
     * caches a reference to the root for use in terms that are inequal to their root
     */
    public static class TemporalCachedCompound extends CachedCompound {
        //        private transient Term rooted = null;
//        private transient Term concepted = null;
        protected final int dt;

        public TemporalCachedCompound(Op op, int dt, Subterms subterms) {
            super(op, dt, subterms);
            this.dt = dt;

//            //TEMPORARY for debug:
//            anon();
        }

        @Override
        public int dt() {
            return dt;
        }

//        @Override
//        public Term root() {
//            Term rooted = this.rooted;
//            return (rooted != null) ? rooted : (this.rooted = super.root());
//        }
//
//        @Override
//        public Term concept() {
//            Term concepted = this.concepted;
//            return (concepted != null) ? concepted : (this.concepted = super.concept());
//        }

    }


    private CachedCompound(/*@NotNull*/ Op op, int dt, Subterms subterms) {

        assert (op != NEG) : "can not construct " + CachedCompound.class + " for NEG: dt=" + dt + ", subs=" + subterms;

        int h = (this.subterms = subterms).hashWith(this.op = op.id);
        this.hash = (dt == DTERNAL) ? h : Util.hashCombine(h, dt);


        this._structure = subterms.structure() | op.bit;

        int _volume = subterms.volume();
        assert (_volume < Short.MAX_VALUE - 1);
        this._volume = (short) _volume;
    }


    abstract public int dt();

    /**
     * since Neg compounds are disallowed for this impl
     */
    @Override
    public final Term unneg() {
        return this;
    }

    @Override
    public final int volume() {
        return _volume;
    }

    @Override
    public final int structure() {
        return _structure;
    }

    @Override
    public final Subterms subterms() {
        return subterms;
    }

    @Override
    public final int varPattern() {
        return hasVarPattern() ? subterms().varPattern() : 0;
    }

    @Override
    public final int varQuery() {
        return hasVarQuery() ? subterms().varQuery() : 0;
    }

    @Override
    public final int varDep() {
        return hasVarDep() ? subterms().varDep() : 0;
    }

    @Override
    public final int varIndep() {
        return hasVarIndep() ? subterms().varIndep() : 0;
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    @Override
    public final Op op() {

        return Op.ops[op];
    }


    @Override
    public final String toString() {
        return Compound.toString(this);
    }


    @Override
    public boolean equals(@Nullable Object that) {
        return Compound.equals(this, that);
    }


}
