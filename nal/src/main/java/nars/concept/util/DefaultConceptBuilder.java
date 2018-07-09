package nars.concept.util;

import jcog.bag.Bag;
import jcog.bag.impl.CurveBag;
import jcog.pri.PriReference;
import nars.Op;
import nars.Param;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.link.TaskLink;
import nars.link.TaskLinkCurveBag;
import nars.link.TemplateTermLinker;
import nars.link.TermLinker;
import nars.table.*;
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



    protected Map newBagMap(int volume) {


        float loadFactor = 0.99f;
//        if (volume < 8) {
//            return new HashMap(0, loadFactor);
//        } else {
        return new UnifiedMap(0, loadFactor);
//        }


    }

    @Override
    public Bag[] newLinkBags(Term t) {
        int v = t.volume();


        Bag<Term, PriReference<Term>> termbag =
                new CurveBag<>(Param.termlinkMerge, newBagMap(v), 0);

        CurveBag<TaskLink> taskbag =
                new TaskLinkCurveBag(Param.tasklinkMerge, newBagMap(v), 0);

        return new Bag[]{termbag, taskbag};


    }


    @Override
    public BeliefTable newTable(Term c, boolean beliefOrGoal) {
        if (c.op().beliefable && !c.hasAny(Op.VAR_QUERY) && (beliefOrGoal || goalable(c))) {
            return new DefaultBeliefTable(newEternalTable(c), newTemporalTable(c));
        } else {
            return BeliefTable.Empty;
        }
    }

    @Override public EternalTable newEternalTable(Term c) {
        return new EternalTable(0);
    }

    @Override
    public TemporalBeliefTable newTemporalTable(Term c) { return RTreeBeliefTable.build(c);     }

    @Override
    public QuestionTable questionTable(Term term, boolean questionOrQuest) {
        Op o = term.op();
        if (questionOrQuest ? o.beliefable : o.goalable) {
            return new QuestionTable.HijackQuestionTable(0, 3);
        } else {
            return QuestionTable.Empty;
        }
    }

    @Override
    public void start(Concept c) {
        alloc.accept(c);
    }

}
