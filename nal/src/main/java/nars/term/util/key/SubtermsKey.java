package nars.term.util.key;

import jcog.memoize.byt.ByteKey;
import nars.IO;
import nars.subterm.Subterms;

public class SubtermsKey extends ByteKey.ByteKeyExternal {
    public final Subterms subs;

    public SubtermsKey(Subterms s) {
        super(IO.subsToDynBytes(s));
        this.subs = s;
    }
}
