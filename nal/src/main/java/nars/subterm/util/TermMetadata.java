package nars.subterm.util;

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
    private final short volume;
    /**
     * stored as complexity+1 as if this termvector were already wrapped in its compound
     */
    private final short complexity;
    private final byte varPattern;
    private final byte varDep;
    private final byte varQuery;
    private final byte varIndep;

    /**
     * normal high-entropy "content" hash of the terms
     */
    public final int hash;

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
}
