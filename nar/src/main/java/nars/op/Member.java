package nars.op;

import nars.$;
import nars.The;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.util.transform.InlineFunctor;

import static nars.Op.SETe;
import static nars.term.atom.Bool.*;

/** equivalent to prolog member/2:
 *      member(U,S)  |-   U is in S
 */
public final class Member extends Functor implements The, InlineFunctor<Evaluation> {

    public static final Functor member = new Member();

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
        Term rewrite = null;
        if (y instanceof Compound) {
            yy = y.subterms();
            if (yy.contains(x)) {
                if (evaluation!=null)
                    return True; //an instance being generated
                else {
                    Term[] zz = yy.removing(x);
                    if (zz.length == 0)
                        return True;
                    Term zzz = SETe.the(zz);
                    rewrite = $.func(member, x, zzz);
                    yy = zzz.subterms();
                    y = zzz;
                }
            }
        } else {
            yy = null;
        }


        if (xVar) {
            if (evaluation!=null) {
                if (y instanceof Compound)
                    evaluation.canBe(x, yy);
                else {
                    if (!evaluation.is(x, y))
                        return Null;
                }
            }
            return rewrite;
        }

        if (yVar) {
            return rewrite; //can no be determined
        }

        return False;
    }
}
