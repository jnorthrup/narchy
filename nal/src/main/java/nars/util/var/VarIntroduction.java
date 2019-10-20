package nars.util.var;

import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.theBool;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;

abstract class VarIntroduction {

    /** returns null if not applicable */
    @Nullable Term apply(Compound x, Random rng, @Nullable Map<Term,Term> retransform) {

        if (x.complexity() < 2)
            return null;


        var uu = select(x.subtermsDirect());
        if (uu.length == 0)
            return null;

        var u = uu.length > 1 ? choose(uu, rng) : uu[0];
        var v = introduce(x, u);
        if (v != null && !(v instanceof theBool)) {
            var y = x.replace(u, v);
            if (y != null && y.op().conceptualizable && !y.equals(x)) {
                if (retransform!=null)
                    retransform.put(u, v);
                return y;
            }
        }

        return null;
    }

    /** determine the choice of subterms which can be replaced with a variable */
    protected abstract @Nullable Term[] select(Subterms input);

    protected abstract Term choose(Term[] x, Random rng);

    /**
     * provides the next terms that will be substituted in separate permutations; return null to prevent introduction
     */
    protected abstract @Nullable Term introduce(Term input, Term selection);
}
