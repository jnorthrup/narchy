package nars.concept.util;

import jcog.bag.Bag;
import jcog.bag.impl.CurveBag;
import jcog.pri.PriReference;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.TaskConcept;
import nars.concept.dynamic.DynamicTruthBeliefTable;
import nars.concept.dynamic.DynamicTruthModel;
import nars.link.TaskLink;
import nars.link.TaskLinkCurveBag;
import nars.table.*;
import nars.term.Conceptor;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.Op.PROD;
import static nars.Op.goalable;

public class DefaultConceptBuilder implements ConceptBuilder {

    private final ConceptState init;
    private final ConceptState awake;
    private final ConceptState sleep;

    final Map<Term, Conceptor> conceptors = new ConcurrentHashMap();

    public DefaultConceptBuilder() {
        this(
                new DefaultConceptState("sleep", 16, 12, 3),
                new DefaultConceptState("awake", 48, 32, 6)
        );
    }
    public DefaultConceptBuilder(ConceptState sleep, ConceptState awake) {
        this.sleep = sleep;
        this.init = awake;
        this.awake = awake;
    }

    @Override
    public Concept build(Term t) {
        return Task.validTaskTerm(t) ? taskConcept(t) : nodeConcept(t);
    }

    @Override
    public void on(Conceptor c) {
        conceptors.put(c.term, c);
    }

    private Concept nodeConcept(Term t) {
        return new NodeConcept(t, newLinkBags(t));
    }


    private TaskConcept taskConcept(final Term t) {
        DynamicTruthModel dmt = ConceptBuilder.dynamicModel(t);
        if (dmt != null) {
            return new TaskConcept(t,
                    new DynamicTruthBeliefTable(t, newEternalTable(), newTemporalTable(t), dmt, true),
                    goalable(t) ?
                            new DynamicTruthBeliefTable(t, newEternalTable(), newTemporalTable(t), dmt, false) :
                            BeliefTable.Empty,
                    this);

        } else {
            Term conceptor = Functor.funcName(t);
            if (conceptor!=Null) {
                @Nullable Termed conceptorc = conceptors.get(conceptor);
                if (conceptorc instanceof Conceptor) {
                    Term[] args = Functor.funcArgsArray(t);
                    if (args.length > 0) {
                        Concept x = ((Conceptor) conceptorc).apply(args[0],
                                args.length > 1 && args[1].op()==PROD ? args[1].subterms() : Op.EmptySubterms);
                        if (x!=null)
                            return (TaskConcept) x;
                    }
                }
            }

            return new TaskConcept(t, this);
        }
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
            return new DefaultBeliefTable(newEternalTable(), newTemporalTable(c));
        } else {
            return BeliefTable.Empty;
        }
    }

    public EternalTable newEternalTable() {
        return new EternalTable(0);
    }

    @Override
    public TemporalBeliefTable newTemporalTable(Term c) {

        return RTreeBeliefTable.build(c);


    }

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
    public ConceptState init() {
        return init;
    }

    @Override
    public ConceptState awake() {
        return awake;
    }

    @Override
    public ConceptState sleep() {
        return sleep;
    }


}
