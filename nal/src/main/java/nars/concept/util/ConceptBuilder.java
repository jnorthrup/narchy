package nars.concept.util;

import jcog.bag.Bag;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.table.TemporalBeliefTable;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Created by me on 3/23/16.
 */
public interface ConceptBuilder extends BiFunction<Term, Termed, Termed> {

    ConceptState init();
    ConceptState awake();
    ConceptState sleep();

    QuestionTable questionTable(Term term, boolean questionOrQuest);
    BeliefTable beliefTable(Term t, boolean beliefOrGoal);
    TemporalBeliefTable newTemporalTable(Term c);



    /** passes through terms without creating any concept anything */
    ConceptBuilder Null = new ConceptBuilder() {

        @Override
        public Termed build(Term term) {
            return term;
        }


        @Override
        public ConceptState init() {
            return ConceptState.Abstract;
        }

        @Override
        public ConceptState awake() {
            return ConceptState.Abstract;
        }

        @Override
        public ConceptState sleep() {
            return ConceptState.Abstract;
        }

        @Override
        public TemporalBeliefTable newTemporalTable(Term c) {
            return TemporalBeliefTable.Empty;
        }

        @Override
        public BeliefTable beliefTable(Term t, boolean beliefOrGoal) {
            return BeliefTable.Empty;
        }

        @Override
        public QuestionTable questionTable(Term term, boolean questionOrQuest) {
            return QuestionTable.Empty;
        }

        @Override
        public Bag[] newLinkBags(Term term) {
            return new Bag[] { Bag.EMPTY, Bag.EMPTY };
        }
    };


    Bag[] newLinkBags(Term term);

    Termed build(Term term);

    @Override
    default Termed apply(Term x, Termed prev) {
        if (prev != null) {
            //if (prev instanceof Concept) {
                Concept c = ((Concept) prev);
                if (!c.isDeleted())
                    return c;
            //}
        }

        return apply(x);
    }

    @Nullable
    default Termed apply(Term x) {

        x = x.the();

        Termed y = build(x);
        if (y == null) {
            return null;
        }

        Concept c = (Concept) y;
        ConceptState s = c.state();

        if (s == ConceptState.New || s == ConceptState.Deleted) {
            c.state(init());
        }

        return c;
    }

}
