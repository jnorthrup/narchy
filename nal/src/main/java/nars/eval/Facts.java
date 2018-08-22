package nars.eval;

import nars.NAR;
import nars.Op;
import nars.concept.Concept;
import nars.table.BeliefTables;
import nars.term.Term;
import nars.unify.Unify;
import nars.unify.UnifySubst;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static nars.Op.IMPL;

/** adapter for reifying NARS beliefs (above a certain confidence threshold) as
 * term-level facts for use during evaluation */
public class Facts implements Function<Term, Stream<Term>> {
    private final NAR nar;
    private final float expMin;
    private final boolean beliefsOrGoals;

    public Facts(NAR nar, float expMin, boolean beliefsOrGoals) {
        this.nar = nar;
        this.expMin = expMin;
        this.beliefsOrGoals = beliefsOrGoals;
    }

    @Override
    public Stream<Term> apply(Term x) {
        //TODO filter or handle temporal terms appropriately
        /* stages
            1. resolve the term itself
            2. check its termlinks, and local neighborhood graph termlinks
            3. exhaustive concept index scan
        */
        Op xo = x.op();
        Unify u = new UnifySubst(null, nar.random(), (m)-> { return false; /* HACK just one is enough */});

        return
                Stream.concat(
                        Stream.of(nar.concept(x)).filter(Objects::nonNull), //Stage 1
                        //TODO Stage 2
                        nar.concepts() //Stage 3
                )
                .filter(y -> {
                    Term yt = y.term();
                    Op yo = yt.op();
                    if (yo ==IMPL) {
                        Term head = yt.sub(1);
                        return (head.op()==xo) && head.unify(x, u.clear());
                    }

                    //TODO prefilter
                    //TODO match implication predicate, store the association in a transition graph
                    return (xo == yo) && x.unify(yt, u.clear());

                })
                .filter(this::trueEnough).map(Concept::term);
    }

    private boolean trueEnough(@Nullable Concept concept) {
        BeliefTables table = beliefsOrGoals ? concept.beliefs() : concept.goals();
        if (table.isEmpty())
            return false;

        return table.streamTasks().anyMatch(t -> exp(t.expectation() ));
    }

    /** whether to accept the given expectation */
    protected boolean exp(float exp) {
        //TODO handle negative expectation
        return (exp >= this.expMin);
    }



}
