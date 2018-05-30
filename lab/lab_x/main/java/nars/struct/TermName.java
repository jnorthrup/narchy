package nars.struct;

/**
 * Created by me on 8/28/15.
 */
public class TermName extends Union {
    public final UTF8String literal = new UTF8String(TermStructTest.MAX_ATOM_LENGTH);
    


    final int SUBTERM_EMPTY = -1;

    public TermName set(final String l) {
        literal.set(l);
        return this;
    }
    public TermName set(final byte[] l) {
        literal.set(l);
        return this;
    }

















}
