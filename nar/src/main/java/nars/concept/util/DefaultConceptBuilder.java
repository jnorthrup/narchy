package nars.concept.util;

import nars.Op;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.link.DynamicTermLinker;
import nars.link.TermLinker;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.eternal.EternalTable;
import nars.table.question.HijackQuestionTable;
import nars.table.question.QuestionTable;
import nars.table.temporal.RTreeBeliefTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;
import java.util.function.Consumer;

import static nars.link.TermLinker.NullLinker;

public class DefaultConceptBuilder extends ConceptBuilder {


    private final Consumer<Concept> alloc;

    public DefaultConceptBuilder(Consumer<Concept> allocator) {
        this.alloc = allocator;
    }





    @Override
    public Concept nodeConcept(Term t) {
        return new NodeConcept(t, this);
    }



    private static Map newBagMap(int volume) {


        return new UnifiedMap(0, 0.99f);
        //return new UnifiedMap(0);
        //return new HashMap(0);

    }


    @Override
    public BeliefTable newTable(Term c, boolean beliefOrGoal) {
        if (c.op().beliefable && !c.hasAny(Op.VAR_QUERY) && (beliefOrGoal || !c.hasAny(Op.IMPL))) {
            return new BeliefTables(
                //(Supplier)()->
                        newEternalTable(c),
                //(Supplier)()->
                        newTemporalTable(c, beliefOrGoal)
            );
        } else {
            return BeliefTable.Empty;
        }
    }

    @Override public EternalTable newEternalTable(Term c) {
        return new EternalTable(0);
    }

    @Override
    public TemporalBeliefTable newTemporalTable(Term c, boolean beliefOrGoal) {
        return //c.hasXternal() ?
                new RTreeBeliefTable()
                //: new RTreeBeliefTable.EternalizingRTreeBeliefTable(beliefOrGoal)
        ;
    }

    @Override
    public QuestionTable questionTable(Term term, boolean questionOrQuest) {
        Op o = term.op();
        if (questionOrQuest ? o.beliefable : o.goalable) {
            return new HijackQuestionTable(0, 3);
        } else {
            return QuestionTable.Empty;
        }
    }

    @Override
    public void start(Concept c) {
        super.start(c);
        alloc.accept(c);
    }


}
