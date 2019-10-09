package nars.util.var;

import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;

abstract class VarIntroduction {

    /** returns null if not applicable */
    @Nullable Term apply(final Compound x, Random rng, @Nullable Map<Term,Term> retransform) {

        if (x.complexity() < 2)
            return null;


        Term[] uu = select(x.subtermsDirect());
        if (uu.length == 0)
            return null;

        Term u = uu.length > 1 ? choose(uu, rng) : uu[0];
        Term v = introduce(x, u);
        if (v != null && !(v instanceof Bool)) {
            Term y = x.replace(u, v);
            if (y != null && y.op().conceptualizable && !y.equals(x)) {
                if (retransform!=null)
                    retransform.put(u, v);
                return y;
            }
        }

        return null;
    }

    /** determine the choice of subterms which can be replaced with a variable */
    @Nullable protected abstract Term[] select(Subterms input);

    protected abstract Term choose(Term[] x, Random rng);

    /**
     * provides the next terms that will be substituted in separate permutations; return null to prevent introduction
     */
    @Nullable abstract protected Term introduce(Term input, Term selection);
}
