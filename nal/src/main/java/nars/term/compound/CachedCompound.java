package nars.term.compound;

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
abstract public class CachedCompound implements Compound, The {

    /**
     * subterm vector
     */
    private final Subterms subterms;


    /**
     * content hash
     */
    private final int hash;

    protected final Op op;

    private final int _volume;
    private final int _structure;

    public static CachedCompound the(/*@NotNull*/ Op op, Subterms subterms) {
        //HACK predict if compound will differ from its root
        if (!op.temporal && !subterms.isTemporal()) { //TODO there are more cases
            return new CachedNontemporalCompound(op, subterms);
        } else {
            return new CachedUnrootedCompound(op, subterms);
        }
    }

//    private static final AtomicInteger SERIAL = new AtomicInteger();
//    public final int serial = SERIAL.getAndIncrement();

    private static class CachedNontemporalCompound extends CachedCompound {
        CachedNontemporalCompound(Op op, Subterms subterms) {
            super(op, subterms);
        }

        @Override
        public boolean equalsRoot(Term x) {
            return equals(x);
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
        public boolean isTemporal() {
            return op.temporal;
        }

        @Override
        public int eventCount() {
            return 1;
        }

        @Override
        public int dtRange() {
            return 0;
        }

        //TODO other assumptions
    }

    /** caches a reference to the root for use in terms that are inequal to their root */
    private static class CachedUnrootedCompound extends CachedCompound {
        private transient Term rooted = null;
        private transient Term concepted = null;

        CachedUnrootedCompound(Op op, Subterms subterms) {
            super(op, subterms);
        }

//        @Override
//        protected void equivalent(CachedUnrootedCompound them) {
//
//            if (them.rooted != null && this.rooted != this) this.rooted = them.rooted;
//            if (this.rooted != null && them.rooted != them) them.rooted = this.rooted;
//
//            if (them.concepted != null && this.concepted != this) this.concepted = them.concepted;
//            if (this.concepted != null && them.concepted != them) them.concepted = this.concepted;
//
//        }

        @Override
        public Term root() {
            Term rooted = this.rooted;
            return (rooted != null) ? rooted : (this.rooted = super.root());
        }

        @Override
        public Term concept() {
            Term concepted = this.concepted;
            return (concepted != null) ? concepted : (this.concepted = super.concept());
        }

    }


    private CachedCompound(/*@NotNull*/ Op op, Subterms subterms) {

        assert(op!=NEG); //makes certain assumptions that it's not NEG op, use Neg.java for that

        this.op = op;

        this.hash = (this.subterms = subterms).hashWith(op);

        this._structure = subterms.structure() | op.bit;
        this._volume = subterms.volume();
    }

    /** since Neg compounds are disallowed for this impl */
    @Override public final Term unneg() {
        return this;
    }

//    @Override
//    abstract public Term root();
//
//    @Override
//    abstract public Term concept();

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


    @Override
    public final int dt() {
        return DTERNAL;
    }


    @Override
    public boolean isCommutative() {
        return op().commutative && subs() > 1;
    }

    @Override
    public final Op op() {
        return op;
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

//        if (that instanceof CachedCompound) {
////            CachedCompound them = (CachedCompound) that;
////            Subterms mySubs = subterms;
////            Subterms theirSubs = them.subterms;
////            if (mySubs != theirSubs) {
////                if (!mySubs.equals(theirSubs))
////                    return false;
////
////                if (mySubs instanceof TermVector && theirSubs instanceof TermVector) {
////                    //prefer the earlier instance for sharing
////                    if ((((TermVector) mySubs).serial) < (((TermVector) theirSubs).serial)) {
////                        them.subterms = mySubs;
////                    } else {
////                        this.subterms = theirSubs;
////                    }
////                }
////
////            }
////
////            //assert(dt()==DTERNAL && them.dt()==DTERNAL);
////            if (op != them.op)
////                return false;
////
////            equivalent((CachedCompound) that);
////            return true;
//
//        } else {
//
//
//            return Compound.equals(this, (Term) that);
//        }
    }

//    /**
//     * data sharing: call if another instance is known to be equivalent to share some clues
//     */
//    protected void equivalent(CachedUnrootedCompound them) {
//
//    }


}
