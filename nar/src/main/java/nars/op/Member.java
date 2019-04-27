package nars.op;

import nars.The;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.util.transform.InlineFunctor;

import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.True;

/** equivalent to prolog member/2:
 *      member(U,S)  |-   U is in S
 */
public final class Member extends Functor implements The, InlineFunctor {

    public static final Functor the = new Member();

    private Member() {
        super("member");
    }

    @Override
    public Term apply(Evaluation evaluation, Subterms terms) {
        if (terms.subs()!=2) return null;

        Term x = terms.sub(0);
        Term y = terms.sub(1);
        if (x.equals(y))
            return True;

        boolean xVar = x instanceof Variable;
        boolean yVar = y instanceof Variable;
        if (xVar && yVar)
            return null; //can no be determined

        Subterms yy;
        if (y instanceof Compound) {
            yy = y.subterms();
            if (yy.contains(x))
                return True;
        } else {
            yy = null;

        }


        if (xVar) {
            if (y instanceof Compound)
                evaluation.canBe(x, yy);
            else
                evaluation.is(x, y);
            return null;
        }

        if (yVar) {
            return null; //can no be determined
        }

        return False;
    }
}
