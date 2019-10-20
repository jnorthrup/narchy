package nars.concept.util;

import nars.Op;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.EmptyBeliefTable;
import nars.table.dynamic.ImageBeliefTable;
import nars.table.eternal.EternalTable;
import nars.table.question.HijackQuestionTable;
import nars.table.question.QuestionTable;
import nars.table.temporal.RTreeBeliefTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.util.function.Consumer;

public class DefaultConceptBuilder extends ConceptBuilder {


    private final Consumer<Concept> alloc;

    public DefaultConceptBuilder(Consumer<Concept> allocator) {
        this.alloc = allocator;
    }

    @Override protected Concept nodeConcept(Term t) {
        return new NodeConcept(t);
    }

    @Override
    public BeliefTable newTable(Term x, boolean beliefOrGoal, BeliefTable... overlay) {

        if (x.op().beliefable && (x instanceof Atomic || (!x.hasAny(Op.VAR_QUERY) && (beliefOrGoal || !x.hasAny(Op.IMPL))))) {

            //HACK ImageBeliefTable proxies for all other sub-tables
            if (overlay.length == 1 && overlay[0] instanceof ImageBeliefTable)
                return overlay[0];
            else {
                var b = new BeliefTables(overlay.length + 2);
                b.addAll(overlay);
                b.add(newTemporalTable(x, beliefOrGoal));
                b.add(newEternalTable(x));
                return b;
            }
        } else {
            assert(overlay.length == 0);
            return EmptyBeliefTable.Empty;
        }
    }

    @Override public EternalTable newEternalTable(Term c) {
        return new EternalTable(0);
    }

    @Override
    public TemporalBeliefTable newTemporalTable(Term c, boolean beliefOrGoal) {
        return new RTreeBeliefTable();
    }

    @Override
    public QuestionTable questionTable(Term term, boolean questionOrQuest) {
        var o = term.op();
		return (questionOrQuest ? o.beliefable : o.goalable) ?
            new HijackQuestionTable(0, 2) :
            QuestionTable.Empty;
    }

    @Override
    public void start(Concept c) {
        super.start(c);
        alloc.accept(c);
    }


}
