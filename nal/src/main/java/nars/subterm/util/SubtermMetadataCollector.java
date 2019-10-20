package nars.subterm.util;

import jcog.Util;
import nars.Op;
import nars.term.Term;
import nars.term.atom.Atomic;

import static nars.Op.*;

public final class SubtermMetadataCollector {
    public int structure = 0;
    public short vol = (short) 1;
    public short varPattern = (short) 0;
    public short varQuery = (short) 0;
    public short varDep = (short) 0;
    public short varIndep = (short) 0;
    public int hash = 1;

    public SubtermMetadataCollector() {

    }

    public SubtermMetadataCollector(Term[] terms) {
        //assert (terms.length <= Param.COMPOUND_SUBTERMS_MAX);
        for (Term x : terms)
            collectMetadata(x);
    }

    private void collectNonVar(Op type, int hash) {
        this.vol++;
        this.structure |= type.bit;
        this.hash = Util.hashCombine(this.hash, hash);
    }

    private void collectVar(Op type) {
        switch (type) {
            case VAR_DEP:
                varDep++;
                break;
            case VAR_INDEP:
                varIndep++;
                break;
            case VAR_QUERY:
                varQuery++;
                break;
            case VAR_PATTERN:
                varPattern++;
                break;
        }
    }

    public void collectMetadata(Term x) {
        int hash = x.hashCode();
        if (x instanceof Atomic) {

            Op xo = x.op();

            collectNonVar(xo, hash);

            if (xo.var)
                collectVar(xo);

        } else {
            this.hash = Util.hashCombine(this.hash, hash);

            int xstructure = x.structure();
            this.structure |= xstructure;

            if ((xstructure & VAR_PATTERN.bit) != 0)
                this.varPattern = (short) ((int) this.varPattern + x.varPattern());
            if ((xstructure & VAR_DEP.bit) != 0)
                this.varDep = (short) ((int) this.varDep + x.varDep());
            if ((xstructure & VAR_INDEP.bit) != 0)
                this.varIndep = (short) ((int) this.varIndep + x.varIndep());
            if ((xstructure & VAR_QUERY.bit) != 0)
                this.varQuery = (short) ((int) this.varQuery + x.varQuery());

            this.vol = (short) ((int) this.vol + x.volume());

        }

    }
}
