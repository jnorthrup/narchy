package nars.term.compound;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;

import static nars.Op.*;


/** 1-element Compound impl */
public class CachedUnitCompound extends SemiCachedUnitCompound {

    private final byte op;

    /** structure including this compound's op (cached) */
    transient private final int cstruct;
    private final short volume;

    public CachedUnitCompound(/*@NotNull*/ Op op, /*@NotNull*/ Term sub) {
        super(sub, Compound.hash1(op.id, sub));
        assert(op!=NEG && op!=CONJ);

        this.op = op.id;
        this.cstruct = sub.structure() | op.bit;

        int v = sub.volume() + 1;
        assert(v < Short.MAX_VALUE);
        this.volume = (short) v;
    }

    @Override
    public final int volume() {
        return volume;
    }

    @Override
    public final int structure() {
        return cstruct;
    }

    public final int opID() {
        return op;
    }

    @Override
    public final Op op() {
        return Op.the(op);
    }

    @Override
    public int varPattern() {
        return hasAny(Op.VAR_PATTERN) ? sub().varPattern() : 0;
    }

    @Override
    public int varDep() {
        return hasAny(Op.VAR_DEP) ? sub().varDep() : 0;
    }

    @Override
    public int varIndep() {
        return hasAny(Op.VAR_INDEP) ? sub().varIndep() : 0;

    }

    @Override
    public int varQuery() {
        return hasAny(Op.VAR_QUERY) ? sub().varQuery() : 0;
    }

    @Override
    public int vars() {
        return hasVars() ? sub().vars() : 0;
    }

    @Override
    public boolean hasVars() {
        return hasAny(VAR_PATTERN.bit | VAR_INDEP.bit | VAR_DEP.bit | VAR_QUERY.bit);
    }
}
