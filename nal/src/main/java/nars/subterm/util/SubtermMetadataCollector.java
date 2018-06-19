package nars.subterm.util;

import jcog.Util;
import nars.Op;
import nars.Param;
import nars.term.Term;

public final class SubtermMetadataCollector {
    public int structure = 0;
    public short vol = 1;
    public byte varPattern = 0, varQuery = 0, varDep = 0, varIndep = 0;
    public int hash = 1;

    public SubtermMetadataCollector() {

    }

    public SubtermMetadataCollector(Term... terms) {
        assert (terms.length <= Param.COMPOUND_SUBTERMS_MAX);
        for (Term x : terms)
            x.collectMetadata(this);
    }

    public void collectNonVar(Op type, int hash) {
        this.vol++;
        this.structure |= type.bit;
        this.hash = Util.hashCombine(this.hash, hash);
    }
}
