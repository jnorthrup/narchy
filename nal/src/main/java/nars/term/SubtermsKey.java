package nars.term;

import jcog.data.byt.RawBytes;
import nars.index.term.NewCompound;

public class SubtermsKey extends RawBytes {
    transient Term[] terms;

    public static SubtermsKey the(Term[] terms) {
        return new SubtermsKey(new NewCompound(terms).update(), terms);
    }

    private SubtermsKey(byte[] key, Term[] subs) {
        super(key);
        this.terms = subs;
    }

    public Term[] commit() {
        Term[] t = terms;
        this.terms = null; //dont hold these refs
        return t;
    }
}
