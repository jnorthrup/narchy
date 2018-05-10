package nars.subterm.util;

import jcog.Util;
import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.Termlike;

/** cached values for term/subterm metadata */
abstract public class TermMetadata implements Termlike {

    /**
     * bitvector of subterm types, indexed by Op.id and OR'd into by each subterm
     * low-entropy, use 'hash' for normal hash operations.
     */
    public final int structure;

    /**
     * stored as volume+1 as if this termvector were already wrapped in its compound
     */
    public final short volume;
    /**
     * stored as complexity+1 as if this termvector were already wrapped in its compound
     */
    public final short complexity;
    public final byte varPattern;
    public final byte varDep;
    public final byte varQuery;
    public final byte varIndep;

    /**
     * normal high-entropy "content" hash of the terms
     */
    public final int hash;

    public static final class SubtermMetadataCollector {
        public int structure = 0;
        public int vol = 1;
        public int varPattern = 0, varQuery = 0, varDep = 0, varIndep = 0;
        public int hash = 1;

        public void collectNonVar(Op type, int hash) {
            this.vol++;
            this.structure |= type.bit;
            this.hash = Util.hashCombine(this.hash, hash);
        }
    }

    public TermMetadata(Term... terms) {
        assert (terms.length <= Param.COMPOUND_SUBTERMS_MAX);

        SubtermMetadataCollector s = new SubtermMetadataCollector();
        for (Term x : terms)
            x.collectMetadata(s);

        this.hash = s.hash;
        this.structure = s.structure;
        int varTot = (this.varPattern = (byte) s.varPattern) +
                (this.varQuery = (byte) s.varQuery) +
                (this.varDep = (byte) s.varDep) +
                (this.varIndep = (byte) s.varIndep);
        this.complexity = (short) ((this.volume = (short) s.vol) - varTot);
    }

//    public TermMetadata(int structure, byte varPattern, byte varDep, byte varQuery, byte varIndep, short complexity, short volume, int hash) {
//        this.structure = structure;
//        this.varPattern = varPattern;
//        this.varDep = varDep;
//        this.varQuery = varQuery;
//        this.varIndep = varIndep;
//        this.complexity = complexity;
//        this.volume = volume;
//        this.hash = hash;
//    }

    public final int vars() {
        return varDep + varIndep + varQuery;
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

    public final int structure() {
        return structure;
    }

    public final int volume() {
        return volume;
    }


    public final int complexity() {
        return complexity;
    }
}
