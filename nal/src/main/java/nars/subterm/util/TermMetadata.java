package nars.subterm.util;

import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import nars.term.anon.AnonID;
import nars.term.var.NormalizedVariable;

/**
 * cached values for term/subterm metadata
 */
abstract public class TermMetadata implements Termlike {

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

    protected TermMetadata(Term... terms) {
        this(new SubtermMetadataCollector(terms));
    }

    protected TermMetadata(SubtermMetadataCollector s) {

        this.hash = s.hash;
        this.structure = s.structure;
        int varTot =
                (this.varPattern = s.varPattern) +
                        (this.varQuery = s.varQuery) +
                        (this.varDep = s.varDep) +
                        (this.varIndep = s.varIndep);
        this.complexity = (short) ((this.volume = s.vol) - varTot);

    }


    protected static boolean testIfInitiallyNormalized(Subterms x) {
        //depth first traversal, determine if variables encountered are monotonically increasing

        final int[] minID = {0};
        final byte[] typeToMatch = {-1};
        return x.recurseTerms(Termlike::hasVars, (v) -> {
            if (v instanceof Variable) {
                if (v instanceof NormalizedVariable) {

                    NormalizedVariable nv = (NormalizedVariable) v;
                    byte varID = nv.id;
                    if (varID == minID[0]) {
                        //same order, ok
                        byte type = nv.anonType();
                        if (typeToMatch[0] == -1)
                            typeToMatch[0] = type;
                        else {
                            if (typeToMatch[0] != type)
                                return false; //same # differnt type, needs normalized
                        }
                    } else if (varID == minID[0] + 1) {
                        //increase the order, ok, set new type
                        typeToMatch[0] = nv.anonType();
                        minID[0]++;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            return true;

        }, null);
    }

    /**
     * for AnonVector
     */
    protected static boolean testIfInitiallyNormalized(short[] subterms) {
        /* checks for monotonically increasing variable numbers starting from 1,
         which will indicate that the subterms is normalized
         */

        int minID = 0;
        int typeToMatch = -1;
        for (short x: subterms) {
            if (x < 0) x = (short) -x;
            int varID = AnonID.isVariable(x, -1);
            if (varID == -1) {
                /*..*/
            } else if (varID == minID) {
                //same order, ok
                int type = AnonID.mask(x);
                if (typeToMatch == -1)
                    typeToMatch = type;
                else if (typeToMatch!=type)
                    return false; //same id different type, needs normalized
            } else if (varID == minID + 1) {
                //increase the order, ok, set new type
                typeToMatch = AnonID.mask(x);
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
    abstract public boolean equals(Object obj);
}
