package nars.term.compound;

import jcog.Util;
import nars.Op;
import nars.Param;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.TermException;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * flyweight Compound implementation for non-DTERNAL dt values.
 * wraps a referenced base Compound and caches only the adjusted hash value,
 * referring to the base for all other details.
 * TODO a CachedCompound version of this
 */
public final class LightDTCompound implements SeparateSubtermsCompound {

    /**
     * numeric (term or "dt" temporal relation)
     */
    private final int dt;
    private final int hashDT;
    private final Compound ref;

    private LightDTCompound(Compound base, int dt) {

        Op op = base.op();


        Subterms s = base.subterms();

        this.ref = base;

        if (!(dt == XTERNAL || Math.abs(dt) < Param.DT_ABS_LIMIT))
            throw new TermException(base.op(), dt, s, "exceeded DT limit");

        if (Param.DEBUG_EXTRA) {

            assert (getClass() != LightDTCompound.class /* a subclass */ || dt != DTERNAL);

            int size = s.subs();

            if (op.temporal && (op != CONJ && size != 2))
                throw new TermException(op, dt, "Invalid dt value for operator", s.arrayShared());

            if (dt != XTERNAL && op.commutative && size == 2) {
                if (sub(0).compareTo(sub(1)) > 0)
                    throw new RuntimeException("invalid ordering");
            }

        }


        if (dt != DTERNAL && dt < 0 && op == CONJ && s.subs() == 2) {

            if (s.sub(0).equals(s.sub(1)))
                dt = -dt;
        }


        assert dt == DTERNAL || dt == XTERNAL || (Math.abs(dt) < Param.DT_ABS_LIMIT) : "abs(dt) limit reached: " + dt;

        this.dt = dt;

        int baseHash = base.hashCode();
        this.hashDT = dt != DTERNAL ? Util.hashCombine(baseHash, dt) : baseHash;
    }

//    @Override
//    public Term the() {
//        throw new TODO();
//    }

    @Override
    public Term the() {
        //throw new TODO();
        return op().the(dt(), arrayShared());
    }

    @Override
    public int varQuery() {
        return ref.varQuery();
    }

    @Override
    public int varDep() {
        return ref.varDep();
    }

    @Override
    public int varIndep() {
        return ref.varIndep();
    }

    @Override
    public int varPattern() {
        return ref.varPattern();
    }


    @Override
    public boolean contains(Term t) {
        return ref.contains(t);
    }

    @Override
    public boolean containsRecursively(Term x, boolean root, Predicate<Term> inSubtermsOf) {
        return ref.containsRecursively(x, root, inSubtermsOf);
    }

    @Override
    public boolean containsRecursively(Term t) {
        return ref.containsRecursively(t);
    }


    @Override
    public final Op op() {
        return ref.op();
    }


    @Override
    public final int structure() {
        return ref.structure();
    }

    @NotNull
    @Override
    public String toString() {
        return Compound.toString(this);
    }


    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (!(that instanceof Compound) || (hashDT != that.hashCode()))
            return false;

        if (that instanceof LightDTCompound) {
            LightDTCompound cthat = (LightDTCompound) that;
            Compound thatRef = cthat.ref;
            Compound myRef = this.ref;


            if (myRef != thatRef) {
                if (!myRef.equals(thatRef))
                    return false;


            }

            return (dt == cthat.dt);

        } else {
            return Compound.equals(this, that);
        }

    }


    @Override
    public final Subterms subterms() {
        return ref.subterms();
    }

    @Override
    public final int subs() {
        return ref.subs();
    }

    @Override
    public final int volume() {
        return ref.volume();
    }

    @Override
    public final int complexity() {
        return ref.complexity();
    }

    @Override
    public final int hashCode() {
        return hashDT;
    }

    @Override
    public final int dt() {
        return dt;
    }


}
