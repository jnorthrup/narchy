package nars.concept.util;

import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.CurveBag;
import nars.Op;
import nars.Param;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.link.TaskLinkCurveBag;
import nars.link.TemplateTermLinker;
import nars.link.TermLinker;
import nars.table.BeliefTables;
import nars.table.eternal.EternalTable;
import nars.table.question.HijackQuestionTable;
import nars.table.question.QuestionTable;
import nars.table.temporal.RTreeBeliefTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;
import java.util.function.Consumer;

import static nars.Op.goalable;

public class DefaultConceptBuilder extends ConceptBuilder {


    private final Consumer<Concept> alloc;

    public DefaultConceptBuilder(Consumer<Concept> allocator) {
        this.alloc = allocator;
    }


    @Override public TermLinker termlinker(Term term) {
        return TemplateTermLinker.of(term);
    }


    @Override
    public NodeConcept nodeConcept(Term t) {
        return new NodeConcept(t, this);
    }



    private static Map newBagMap(int volume) {


        //return new UnifiedMap(0, 0.99f);
        return new UnifiedMap(0);
        //return new HashMap(0);

    }

    @Override
    public Bag[] newLinkBags(Term t) {
        int v = t.volume();


        Map shared = newBagMap(v);
        
        Bag<Term, PriReference<Term>> termbag =
                new CurveBag<>(Param.termlinkMerge, shared, 0);

        ArrayBag<nars.link.TaskLink,nars.link.TaskLink> taskbag =
                new TaskLinkCurveBag(Param.tasklinkMerge, shared, 0);

        return new Bag[]{termbag, taskbag};


    }


    @Override
    public BeliefTables newTable(Term c, boolean beliefOrGoal) {
        if (c.op().beliefable && !c.hasAny(Op.VAR_QUERY) && (beliefOrGoal || goalable(c))) {
            return new BeliefTables(newTemporalTable(c, beliefOrGoal), newEternalTable(c));
        } else {
            return BeliefTables.Empty;
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
        alloc.accept(c);
    }


}
