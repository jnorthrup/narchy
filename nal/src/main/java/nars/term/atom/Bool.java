package nars.term.atom;

import nars.Op;
import nars.The;
import nars.term.Evaluation;
import nars.term.Term;
import nars.unify.Unify;
import org.jetbrains.annotations.NotNull;

import static nars.Op.BOOL;
import static nars.Op.Null;


/** special/reserved/keyword representing fundamental absolute boolean truth states:
 *      True - absolutely true
 *      False - absolutely false
 *      Null - absolutely nonsense
 *
 *  these represent an intrinsic level of truth that exist within the context of
 *  an individual term.  not to be confused with Task-level Truth
 */
abstract public class Bool extends AtomicConst implements The {

    private final String id;

    protected Bool(String id) {
        super(BOOL, id);
        this.id = id;
    }

    @Override
    public final Term conceptualizableOrNull() {
        return Null;
    }

    @Override
    public /*@NotNull*/ Op op() {
        return BOOL;
    }

    @Override
    public String toString() {
        return id;
    }

    @NotNull
    @Override
    abstract public Term unneg();

    @Override
    public final boolean equals(Object u) {
        return u == this;
    }

    @Override
    abstract public boolean equalsNeg(Term t);

    @Override
    public final Term concept() {
        return Null;
    }

    @Override
    public final Term eval(Evaluation.TermContext context) {
        return this;
    }

    @Override
    public final Term evalSafe(Evaluation.TermContext context, Op supertermOp, int subterm, int remain) {
        return this;
    }

    @Override
    public final boolean unify(Term y, Unify subst) {
        return this == y;
    }

    @Override
    public final Term dt(int dt) {
        return this; //allow
        //throw never("dt");
    }


//    UnsupportedOperationException never(String eval) {
//        return new UnsupportedOperationException(this + " Bool leak attemping: " + eval);
//    }

}
