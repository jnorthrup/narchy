package nars.term.compound;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.Subterms;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;


/** on-heap, caches many commonly used methods for fast repeat access while it survives */
public class CachedCompound implements Compound {

    /**
     * subterm vector
     */
    private final Subterms subterms;


    /**
     * content hash
     */
    public final int hash;

    public final Op op;

    final int _volume;
    final int _structure;

    private transient Term rooted = null;
    private transient Term concepted = null;


    public CachedCompound(/*@NotNull*/ Op op, Subterms subterms) {

        this.op = op;

        this.hash = Util.hashCombine((this.subterms = subterms).hashCode(), op.id);

        this._structure = Compound.super.structure();
        this._volume = Compound.super.volume();
    }

    @Override public Term root() {
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
    public final int volume() { return _volume;     }

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
    public int dt() {
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

        if (!(that instanceof Term) || hash != that.hashCode())
            return false;

        if (Compound.equals(this, (Term) that)) {
            if (that instanceof CachedCompound) {
                equivalent((CachedCompound) that);
            }
            return true;
        }
        return false;
    }

    /**
     * data sharing
     */
    private void equivalent(CachedCompound that) {
//        TermContainer otherSubterms = that.subterms;
//        TermContainer mySubterms = this.subterms;
//        if (mySubterms!=otherSubterms) {
//            if (System.identityHashCode(mySubterms) < System.identityHashCode(otherSubterms))
//                that.subterms = mySubterms;
//            else
//                this.subterms = otherSubterms;
//        }

        if (that.rooted != null && this.rooted!=this) this.rooted = that.rooted;
        if (this.rooted != null && that.rooted!=that) that.rooted = this.rooted;

        if (that.concepted != null && this.concepted!=this) this.concepted = that.concepted;
        if (this.concepted != null && that.concepted!=that) that.concepted = this.concepted;

    }


}
