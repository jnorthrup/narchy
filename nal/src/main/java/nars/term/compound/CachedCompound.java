package nars.term.compound;

import jcog.Util;
import nars.Op;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;


/**
 * on-heap, caches many commonly used methods for fast repeat access while it survives
 */
abstract public class CachedCompound extends SeparateSubtermsCompound implements The {

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

    public static Compound newCompound(Op op, int dt, Subterms subterms) {
//        if (subterms instanceof DisposableTermList)
//            throw new WTF();
//        boolean hasTemporal = op.temporal || subterms.hasAny(Op.Temporal);
//        boolean isNormalized = subterms.isNormalized();

        //HACK TEMPORARY
//        if (op==CONJ && subterms.subs()==2 && subterms.sub(0).volume() < subterms.sub(1).volume())
//            throw new WTF();

        Compound c;
        if (!op.temporal && !subterms.hasAny(Op.Temporal) && subterms.isNormalized()) {
            assert (dt == DTERNAL);
//            if (key!=null && subterms.volume() < Param.TERM_BYTE_KEY_CACHED_BELOW_VOLUME) {
//                return new CachedCompound.SimpleCachedCompoundWithBytes(op, subterms, key);
//            } else {
            c = new SimpleCachedCompound(op, subterms);
//            }
        } else {
            c = new TemporalCachedCompound(op, dt, subterms);
        }

//        //TEMPORARY
//        try {
//            assert(c.anon()!=null);
//        } catch (Throwable t) {
//            throw new WTF(t);
//        }

        return c;
    }

    private static class SimpleCachedCompound extends CachedCompound {

        SimpleCachedCompound(Op op, Subterms subterms) {
            super(op, DTERNAL, subterms);
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
//            if (dt!=XTERNAL && dt > 2147470000)//TEMPORARY
//                throw new WTF();
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

        int h = (this.subterms = subterms).hashWith(this.op = op.id);
        this.hash = (dt == DTERNAL) ? h : Util.hashCombine(h, dt);


        this._structure = subterms.structure() | op.bit;

        this._volume = (short) subterms.volume();
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
    public final int opBit() {
        return 1<<op;
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
