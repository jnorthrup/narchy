package nars.subterm;

import nars.subterm.TermList;
import nars.term.Term;

/** for temporary, single-use only */
public class DisposableTermList extends TermList {

    public DisposableTermList(int initialCap) {
        super(initialCap);
    }

    public DisposableTermList(Term[] t) {
        super(t);
    }

    public DisposableTermList(int initialCap, int startingSize) {
        super(initialCap);
        this.size = startingSize;
    }



    /** can only be used once, and it stops this TermList from further use */
    @Override public Term[] arrayShared() {

        Term[] l = arrayKeep();

        
        this.items = null;
        this.size = -1;

        return l;
    }

}
