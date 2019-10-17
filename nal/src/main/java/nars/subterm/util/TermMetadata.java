package nars.subterm.util;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import nars.term.anon.Intrin;
import nars.term.util.Image;
import nars.term.util.TermException;
import nars.term.var.NormalizedVariable;

/**
 * cached values for target/subterm metadata
 */
public abstract class TermMetadata implements Termlike {

    /**
     * bitvector of subterm types, indexed by Op.id and OR'd into by each subterm
     * low-entropy, use 'hash' for normal hash operations.
     */
    public final int structure;
    /**
     * normal high-entropy "content" hash of the terms
     */
    public final int hash;
    /**
     * stored as volume+1 as if this termvector were already wrapped in its compound
     */
    private final short volume;
    /**
     * stored as complexity+1 as if this termvector were already wrapped in its compound
     */
    private final short complexity;
    private final byte varPattern;
    private final byte varDep;
    private final byte varQuery;
    private final byte varIndep;

    protected TermMetadata(Term[] terms) {
        this(new SubtermMetadataCollector(terms));
    }

    protected TermMetadata(SubtermMetadataCollector s) {

        if (s.varPattern > Byte.MAX_VALUE || s.varQuery > Byte.MAX_VALUE || s.varDep > Byte.MAX_VALUE || s.varIndep > Byte.MAX_VALUE)
            throw new TermException("variable overflow");

        int varTot =
                (this.varPattern = (byte) s.varPattern) +
                (this.varQuery = (byte) s.varQuery) +
                (this.varDep = (byte) s.varDep) +
                (this.varIndep = (byte) s.varIndep);

        this.complexity = (short) ((this.volume = s.vol) - varTot);

        this.hash = s.hash;
        this.structure = s.structure;

    }


    /** not a conclusive test but meant to catch most cases where the target is already normalized */
    public static boolean normalized(Subterms x) {

        //the product containing an image itself may be normalized but superterms containing it are not automatically considered normal
        if (Image.imageNormalizable(x))
            return false;

        int vars = x.vars();
        if (vars==0)
            return true;

        //depth first traversal, determine if variables encountered are monotonically increasing

        final ByteArrayList types = new ByteArrayList(vars);
        return x.recurseTermsOrdered(Term::hasVars, v -> {
            if (!(v instanceof Variable))
                return true;

            if (v instanceof NormalizedVariable) {
                NormalizedVariable nv = (NormalizedVariable) v;
                byte varID = nv.id();
                int nTypes = types.size();
                if (varID <= nTypes) {
                    return types.getByte(varID-1) == nv.anonType();
                } else if (varID == 1 + nTypes) {
                    types.add(nv.anonType());
                    return true;
                }
            }

            return false;

        }, null);
    }

    /**
     * for AnonVector
     */
    protected static boolean normalized(short[] subterms) {
        /* checks for monotonically increasing variable numbers starting from 1,
         which will indicate that the subterms is normalized
         */

        int minID = 0;
        int typeToMatch = -1;
        for (short x: subterms) {
            boolean neg = x < 0;
            if (neg) {
                x = (short) -x;
            }
            int varID = Intrin.isVariable(x, -1);
            if (varID == -1) {
                /*..*/
            } else if (varID == minID) {
                //same order, ok
                int type = Intrin.group(x);
                if (typeToMatch == -1)
                    typeToMatch = type;
                else if (typeToMatch!=type)
                    return false; //same id different type, needs normalized
            } else if (varID == minID + 1) {
                //increase the order, ok, set new type
                typeToMatch = Intrin.group(x);
                minID++;
            } else if (varID > minID + 1) {
                return false; //cant be sure
            }
        }
        return true;
    }

    public final int vars() {
        return varDep + varIndep + varQuery + varPattern;
    }

    public final int varQuery() {
        return varQuery;
    }

    public final int varDep() {
        return varDep;
    }

    public final int varIndep() {
        return varIndep;
    }

    public final int varPattern() {
        return varPattern;
    }

    @Override
    public final boolean hasVarQuery() {
        return varQuery > 0;
    }

    @Override
    public final boolean hasVarIndep() {
        return varIndep > 0;
    }

    @Override
    public final boolean hasVarDep() {
        return varDep > 0;
    }

    @Override
    public boolean hasVarPattern() {
        return varPattern > 0;
    }

    public final int structure() {
        return structure;
    }

    public final int volume() {
        return volume;
    }

    public final int complexity() {
        return complexity;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public abstract boolean equals(Object obj);
}
