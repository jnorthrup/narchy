package nars.op;

import nars.term.Term;
import nars.term.atom.Bool;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;

abstract class VarIntroduction {

    Pair<Term, Map<Term, Term>> apply(final Term x, Random r) {

        if (x.complexity() < 2) 
            return null;


        Term u = select(x, r);
        if (u != null) {


            int vars = x.vars();
            assert (vars < 127 - 1);

            Term v = introduce(x, u, (byte) (vars + 1));
            if (v != null && !(v instanceof Bool)) {
                Term y = x.replace(u, v);
                if (y != null && y.op().conceptualizable && !y.equals(x)) {
                    return Tuples.pair(y, Map.of(u, v));
                }
            }
        }

        return null;

    }


    /** choose the subterm that can be replaced with a variable */
    @Nullable abstract protected Term select(Term input, Random shuffle);


    /**
     * provides the next terms that will be substituted in separate permutations; return null to prevent introduction
     */
    abstract protected Term introduce(Term input, Term selection, byte order);
}
