package nars.term.anon;

import nars.term.Term;

import java.util.Iterator;

class AnonArrayIterator implements Iterator<Term> {

    private int current;
    private final short[] values;

    public AnonArrayIterator(short[] subterms) {
        this.values = subterms;
    }

    @Override
    public boolean hasNext() {
        return this.current < values.length;
    }

    @Override
    public Term next() {

        return AnonID.idToTerm(values[this.current++]);
    }

}
