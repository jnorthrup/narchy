package nars.term.anon;

import nars.term.Term;

import java.util.Iterator;

public class AnomArrayIterator implements Iterator<Term> {

    private int current;
    private final byte[] values;

    public AnomArrayIterator(byte[] subterms) {
        this.values = subterms;
    }

    @Override
    public boolean hasNext() {
        return this.current < values.length;
    }

    @Override
    public Term next() {
        return Anom.the[values[this.current++]];
    }

}
