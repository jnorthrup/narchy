package nars.eval;

import nars.NAR;
import nars.Op;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.unify.UnifyAny;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static nars.Op.IMPL;

/**
 * adapter for reifying NARS beliefs (above a certain confidence threshold) as
 * target-level facts for use during evaluation
 */
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
            1. resolve the target itself
            2. check its termlinks, and local neighborhood graph termlinks
            3. exhaustive concept index scan
        */
        Op xo = x.op();
        UnifyAny u = new UnifyAny(nar.random());

        return
                Stream.concat(
                        Stream.of(nar.concept(x)).filter(Objects::nonNull), //Stage 1
                        //TODO Stage 2
                        nar.concepts() //Stage 3
                )
                        .filter(c -> {
                            if (!(c instanceof TaskConcept))
                                return false;

                            Term yt = c.term();
                            Op yo = yt.op();
                            if (beliefsOrGoals && yo == IMPL) {
                                Term head = yt.sub(1);
                                return head.op() == xo && head.unify(x, u.clear());
                            }

                            //TODO prefilter
                            //TODO match implication predicate, store the association in a transition graph
                            return xo == yo && x.unify(yt, u.clear());


                        })
                        .map(c -> {


                            BeliefTable table = beliefsOrGoals ? c.beliefs() : c.goals();
                            if (table.isEmpty())
                                return null;

                            boolean t = polarized(table, true), f = polarized(table, false);
                            if (t == f)
                                return null;

                            Term ct = c.term();
                            if (t && !f) {
                                return ct;
                            } else /*if (!t && f)*/ {
                                return ct.neg();
                            }

                        }).filter(Objects::nonNull);
    }

    private boolean polarized(BeliefTable table, boolean trueOrFalse) {
        return table.taskStream().anyMatch(t -> exp(trueOrFalse ? t.expectation() : 1 - t.expectation()));
    }

    /**
     * whether to accept the given expectation
     */
    protected boolean exp(float exp) {
        //TODO handle negative expectation
        return exp >= this.expMin;
    }


}
