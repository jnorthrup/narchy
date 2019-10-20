package nars.op;

import nars.The;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.transform.InlineFunctor;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.theBool.Null;


/**
 * if STRICT is 4th argument, then there will only be a valid result
 * if the input has changed (not if nothing changed, and not if the attempted change had no effect)
 */
public class Replace extends Functor implements InlineFunctor<Evaluation>, The {

    public static final Replace replace = new Replace("replace");

    protected Replace(String id) {
        this((Atom)Atomic.the(id));
    }

    protected Replace(Atom id) {
        super(id);
    }

    @Override
    public @Nullable Term apply(Evaluation e, Subterms xx) {

        var input = xx.sub(0);

        var x = xx.sub(1);

        var y = xx.sub(2);

        return apply(xx, input, x, y);
    }

    protected static Term apply(Subterms xx, Term input, Term x, Term y) {
        var strict = xx.subEquals(3, UniSubst.NOVEL);

        var result = !x.equals(y) ?
                input.replace(x, y)
                :
                (strict ? Null : input);

        return (result == Null) || (strict && input.equals(result)) ? Null : result;
    }






}
