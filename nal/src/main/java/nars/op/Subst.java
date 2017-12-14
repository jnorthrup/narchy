package nars.op;

import nars.$;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.container.Subterms;
import nars.term.subst.MapSubst1;
import org.jetbrains.annotations.Nullable;

import static nars.Op.Null;


public class Subst extends Functor {

    //TODO use special symbol encoding to avoid collision with equivalent normal input
    final static Term STRICT = Atomic.the("strict");
    final static Term INDEP_VAR = $.quote("$");
    final static Term DEP_VAR = $.quote("#");
    final static Term SAME = Atomic.the("same");


    public static final Subst the = new Subst();

    private Subst() {
        super((Atom) $.the("substitute"));
    }

    protected Subst(Atom id) {
        super(id);
    }

    @Nullable @Override public Term apply( Subterms xx) {

        final Term input = xx.sub(0); //term to possibly transform

        final Term x = xx.sub(1); //original term (x)

        final Term y = xx.sub(2); //replacement term (y)

        Term result;
        if (x.equals(y) || !input.containsRecursively(x)) {
            result = xx.subEquals(3, STRICT) ? Null : input; //no change would be applied
        }else if (input.equals(x)) { //direct replacement
            result = y; //TODO add STRICT condition here?
        } else {
            result = input.replace(x, y);
        }

//        if (!(result instanceof Bool && !result.equals(input))) {
////            //add mapping in parent
////            if (!onChange(input, x, y, result))
////                return Null;
//        }

        return result;
    }

//    /** called if substitution was successful */
//    protected boolean onChange(Term from, Term x, Term y, Term to) {
//        return true;
//    }

}
