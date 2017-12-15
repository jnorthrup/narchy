package nars.term.sub;

import nars.term.Term;

/** reversed view of a TermContainer */
public class ReverseSubterms extends ProxySubterms {

    private final byte _size;

    public ReverseSubterms(Subterms ref) {
        super(ref);
        this._size = (byte) ref.subs();
    }

    @Override
    public Term sub(int i) {
        return ref.sub(_size-1-i);
    }
}
