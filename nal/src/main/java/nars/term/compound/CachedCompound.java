package nars.term.compound;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.Subterms;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;


/**
 * on-heap, caches many commonly used methods for fast repeat access while it survives
 */
public class CachedCompound implements Compound {

    /**
     * subterm vector
     */
    private Subterms subterms;


    /**
     * content hash
     */
    public final int hash;

    public final Op op;

    final int _volume;
    final int _structure;

    private static volatile int SERIAL = 0;
    public final int serial = SERIAL++;

    private transient Term rooted = null;
    private transient Term concepted = null;


    public CachedCompound(/*@NotNull*/ Op op, Subterms subterms) {

        this.op = op;

        this.hash = Util.hashCombine((this.subterms = subterms).hashCode(), op.id);

        this._structure = Compound.super.structure();
        this._volume = Compound.super.volume();
    }

    @Override
    public Term root() {
        return (rooted != null) ? rooted
                :
                (this.rooted = Compound.super.root());
    }


    @Override
    public Term conceptual() {
        return (concepted != null) ? concepted
                :
                (this.concepted = Compound.super.conceptual());
    }

    @Override
    public final int volume() {
        return _volume;
    }

    @Override
    public final int structure() {
        return _structure;
    }

    //    @Override
//    public boolean isDynamic() {
//        return dynamic;
//    }

    @NotNull
    @Override
    public final Subterms subterms() {
        return subterms;
    }


    @Override
    public int hashCode() {
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
        return IO.Printer.stringify(this).toString();
    }

    @Override
    public final boolean equals(@Nullable Object that) {
        if (this == that) return true;

        if (that instanceof CachedCompound) {
            CachedCompound them = (CachedCompound) that;
            Subterms mySubs = subterms;
            Subterms theirSubs = them.subterms;
            if (mySubs != theirSubs) {
                if (!mySubs.equals(theirSubs))
                    return false;

                if (mySubs instanceof TermVector && theirSubs instanceof TermVector) {
                    //prefer the earlier instance for sharing
                    if ((((TermVector) mySubs).serial) < (((TermVector) theirSubs).serial)) {
                        them.subterms = mySubs;
                    } else {
                        this.subterms = theirSubs;
                    }
                }

            }

            //assert(dt()==DTERNAL && them.dt()==DTERNAL);
            if (op != them.op)
                return false;

            equivalent((CachedCompound) that);
            return true;

        } else {
            if (!(that instanceof Term) || hash != that.hashCode())
                return false;

            return Compound.equals(this, (Term) that);
        }
    }

    /**
     * data sharing: call if another instance is known to be equivalent to share some clues
     */
    protected void equivalent(CachedCompound them) {


        if (them.rooted != null && this.rooted != this) this.rooted = them.rooted;
        if (this.rooted != null && them.rooted != them) them.rooted = this.rooted;

        if (them.concepted != null && this.concepted != this) this.concepted = them.concepted;
        if (this.concepted != null && them.concepted != them) them.concepted = this.concepted;

    }


}
