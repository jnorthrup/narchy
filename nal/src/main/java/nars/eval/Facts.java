package nars.eval;

import com.google.common.collect.Streams;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.table.BeliefTables;
import nars.term.Term;
import nars.unify.Unify;
import nars.unify.UnifySubst;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

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
        Unify u = new UnifySubst(null, nar.random(), (m)-> { return false; /* HACK just one is enough */});

        return
                Streams.concat(
                        Stream.of(nar.concept(x)).filter(Objects::nonNull), //Stage 1
                        //TODO Stage 2
                        nar.concepts() //Stage 3
                )
                .filter(y -> {
                    //TODO prefilter
                    //TODO match implication predicate, store the association in a transition graph
                    return x.unify(y.term(), u.clear());
                })
                .filter(this::isTrue).map(Concept::term);
    }

    private boolean isTrue(@Nullable Concept concept) {
        BeliefTables table = beliefsOrGoals ? concept.beliefs() : concept.goals();
        if (table.isEmpty())
            return false;

        return table.streamTasks().anyMatch(t -> exp(((Task) t).expectation() ));
    }

    /** whether to accept the given expectation */
    protected boolean exp(float exp) {
        //TODO handle negative expectation
        return (exp >= this.expMin);
    }



}
