package nars.op;

import nars.term.Term;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;

public abstract class VarIntroduction {

    public Pair<Term, Map<Term, Term>> apply(final Term x, Random r) {

        if (x.complexity() < 2) //exclude vars (.volume()) in this count
            return null;


        Term u = select(x, r);
        if (u == null)
            return null;

        int vars = x.vars();
        assert (vars < 127 - 1);
        //ensure the variables wont collide with existing variables by assigning an introduced variable to unique ID
        Term v = introduce(x, u, (byte)(vars+1));

        Term y = x.replace(u, v);
        if (y != null && !y.equals(x)) {
            if (x.isNormalized()) {

                Term yy = y.normalize();
                if (yy == null)
                    throw new RuntimeException("could not normalize result of variable introduction: " + y);

                y = yy;
            }

            return Tuples.pair(y, Map.of(u, v));
        } else {
            return null;
        }
    }


    /** choose the subterm that can be replaced with a variable */
    @Nullable abstract protected Term select(Term input, Random shuffle);


    /**
     * provides the next terms that will be substituted in separate permutations; return null to prevent introduction
     */
    abstract protected Term introduce(Term input, Term selection, byte order);
}
