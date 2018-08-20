package nars.term.atom;

import nars.Op;
import nars.The;
import nars.term.Term;

import static nars.Op.BOOL;


/** special/reserved/keyword representing fundamental absolute boolean truth states:
 *      True - absolutely true
 *      False - absolutely false
 *      Null - absolutely nonsense
 *
 *  these represent an intrinsic level of truth that exist within the context of
 *  an individual term.  not to be confused with Task-level Truth
 */
abstract public class Bool extends AtomicConst implements The {

    private final String label;

    protected Bool(String label, byte code) {
        super(new byte[] { BOOL.id, code } );
        this.label = label;
    }

    @Override
    abstract public boolean equalsNegRoot(Term t);

    @Override
    public final boolean equalsRoot(Term x) {
        return equals(x);
    }

    @Override
    public final boolean equals(Object u) {
        return u == this;
    }

    @Override
    public /*@NotNull*/ Op op() {
        return BOOL;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    abstract public Term unneg();


    @Override
    abstract public boolean equalsNeg(Term t);

    @Override
    public final Term concept() {
        //return Null;
        throw new UnsupportedOperationException();
    }



    @Override
    public final Term dt(int dt) {
        //return this;
        throw new UnsupportedOperationException();
    }

}
