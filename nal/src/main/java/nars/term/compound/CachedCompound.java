package nars.term.compound;

import jcog.Util;
import nars.Op;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

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
    private final int hash;

    private final byte op;

    private final short _volume;
    private final int _structure;

    public static final class SimpleCachedCompound extends CachedCompound {

        public SimpleCachedCompound(Op op, Subterms subterms) {
            super(op, DTERNAL, subterms);
        }

        @Override
        public boolean equalsRoot(Term x) {
            return x instanceof SimpleCachedCompound ? equals(x) : equals(x.root());
        }

        @Override
        public boolean equalsNegRoot(Term x) {
            return x instanceof SimpleCachedCompound ? equalsNeg(x) : equalsNeg(x.root());
        }

        @Override
        public Term root() {
            return this;
        }

        @Override
        public Term concept() {
            return this;
        }

        @Override
        public boolean hasXternal() {
            return false;
        }

        @Override
        public boolean isTemporal() {
            return false;
        }

        @Override
        public int eventCount() {
            return 1;
        }

        @Override
        public int dtRange() {
            return 0;
        }

        @Override
        public int subTimeOnly(Term x) {
            return equals(x) ? 0 : DTERNAL;
        }


        @Override
        public int dt() {
            return DTERNAL;
        }

    }

    /** caches a reference to the root for use in terms that are inequal to their root */
    public static final  class TemporalCachedCompound extends CachedCompound  {
//        private transient Term rooted = null;
//        private transient Term concepted = null;
        final int dt;

        public TemporalCachedCompound(Op op, int dt, Subterms subterms) {
            super(op, dt, subterms);
            this.dt = dt;
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

        assert(op!=NEG); 

        int h = (this.subterms = subterms).hashWith(this.op = op.id);
        this.hash = (dt == DTERNAL) ? h : Util.hashCombine(h, dt);


        this._structure = subterms.structure() | op.bit;

        int _volume = subterms.volume();
        assert(_volume < Short.MAX_VALUE-1);
        this._volume = (short)_volume;
    }


    /** since Neg compounds are disallowed for this impl */
    @Override public final Term unneg() {
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
    public final int hashCode() {
        return hash;
    }


    abstract public int dt();

    @Override
    public final Op op() {
        
        return Op.ops[op];
    }


    @Override
    public String toString() {
        return Compound.toString(this);
    }


    @Override
    public final boolean equals(@Nullable Object that) {
        if (this == that) return true;

        if (!(that instanceof Compound) || hash != that.hashCode())
            return false;
        return Compound.equals(this, (Term) that);



    }









}
