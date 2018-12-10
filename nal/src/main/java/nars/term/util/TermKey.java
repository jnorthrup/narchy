package nars.term.util;

import jcog.memoize.byt.ByteKey;
import nars.IO;
import nars.term.Term;

public final class TermKey extends ByteKey.ByteKeyExternal {

    public final Term term;

    public TermKey(Term t) {
        super(IO.termToDynBytes(t));
        this.term = t;
    }

}
