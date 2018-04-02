package nars.op.data;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * recursively collects the contents of set/list compound term argument's
 * into a list, to one of several resulting term types:
 *      product
 *      set (TODO)
 *      conjunction (TODO)
 *
 * TODO recursive version with order=breadth|depth option
 */
public abstract class flat extends Functor.UnaryFunctor {

    protected flat() {
        super("flat");
    }

    @Override
    public @Nullable Term apply(Term x) {
        if (x instanceof Subterms) {
            List<Term> l = $.newArrayList(x.volume());
            collect(((Subterms)x).arrayClone(), l);
            return result(l);
        } else {
            return null;
        }
    }

    @NotNull
    public static List<Term> collect(@NotNull Term[] x, @NotNull List<Term> l) {
        for (Term a : x) {
            if (a.op() == Op.PROD || a.op().isSet() || a.op() == Op.CONJ) {
                ((Subterms) a).copyInto(l);
            }
            else
                l.add(a);
        }
        return l;
    }

    public abstract Term result(List<Term> terms);

    public static flat flatProduct = new flat() {

        @Override
        public Term result(List<Term> terms) {
            return $.p(terms);
        }

    };

    //public Flat(boolean productOrSet, boolean breadthOrDepth) {
        //generate each of the 4 different operate names

    //}
}
