package nars.term.buffer;

import nars.Op;
import nars.term.Term;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.map.ByteAnonMap;

public class DirectTermBuffer extends TermBuffer {

    public DirectTermBuffer() {
        super(HeapTermBuilder.the, new ByteAnonMap(TermBuffer.INITIAL_ANON_SIZE));
    }

    @Override
    protected Term newCompound(Op o, int dt, Term[] subterms) {

        return HeapTermBuilder.the.
                //newCompound(o, dt, subterms);
                newCompoundN(o, dt, subterms,null); //more direct
    }
}
