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
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

import static nars.Op.goalable;

public class DefaultConceptBuilder implements ConceptBuilder {

    @Override
    public Concept build(Term t) {
        return Task.validTaskTerm(t) ? taskConcept(t) : nodeConcept(t);
    }

    private Concept nodeConcept(Term t) {
        return new NodeConcept(t, newLinkBags(t));
    }

    private TaskConcept taskConcept(final Term t) {
        DynamicTruthModel dmt = ConceptBuilder.unroll(t);

        if (dmt != null) {
            return new TaskConcept(t,
                    new DynamicTruthBeliefTable(t, newTemporalTable(t), dmt, true),
                    goalable(t) ?
                            new DynamicTruthBeliefTable(t, newTemporalTable(t), dmt, false) :
                            BeliefTable.Empty,
                    this);

        } else {
            return new TaskConcept(t, this);
        }
    }


    private final ConceptState init;
    private final ConceptState awake;
    private final ConceptState sleep;

    public DefaultConceptBuilder() {
        this(
                new DefaultConceptState("sleep", 32, 16, 3),
                new DefaultConceptState("awake", 64, 32, 6)
        );
    }

    public DefaultConceptBuilder(ConceptState sleep, ConceptState awake) {
        this.sleep = sleep;
        this.init = sleep;
        this.awake = awake;
    }


    static Map newBagMap(int volume) {
        int initialCap = 2;
        float loadFactor = 0.75f;

//        if (concurrent()) {
////            //return new ConcurrentHashMap(defaultInitialCap, 1f);
////            //return new NonBlockingHashMap(cap);
////            return new org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe<>();
////            //ConcurrentHashMapUnsafe(cap);
////        } else {
////            return new HashMap(defaultInitialCap, 1f);
//            //   if (volume < 16) {
//            return new ConcurrentHashMap(0, loadFactor);
//
////            } else if (volume < 32) {
////                return new SynchronizedHashMap(0, loadFactor);
////                //return new TrieMap();
////            } else {
////                return new SynchronizedUnifiedMap(0, loadFactor);
////            }
//        } else {

        return new UnifiedMap(initialCap, loadFactor);
        //return new HashMap(initialCap, loadFactor);
//        }

    }

    @Override
    public Bag[] newLinkBags(Term t) {
        int v = t.volume();
        //if (/*v > 3 && */v < 16) {
//        Map sharedMap = newBagMap(v);

        Bag<Term, PriReference<Term>> termbag =
                new CurveBag<>(Param.termlinkMerge, newBagMap(v), 0);
        CurveBag<TaskLink> taskbag =
                new TaskLinkCurveBag(newBagMap(v));

        return new Bag[]{termbag, taskbag};
//        } else {
//            return new Bag[]{
//                    new MyDefaultHijackBag(Param.termlinkMerge),
//                    new MyDefaultHijackBag(Param.tasklinkMerge)
//            };
//        }

    }


    @Override
    public BeliefTable newTable(Term c, boolean beliefOrGoal) {
        //TemporalBeliefTable newTemporalTable(final int tCap, NAR nar) {
        //return new HijackTemporalBeliefTable(tCap);
        //return new RTreeBeliefTable(tCap);
        if (c.op().beliefable && !c.hasAny(Op.VAR_QUERY) && (beliefOrGoal || goalable(c))) {
            return new DefaultBeliefTable(newTemporalTable(c));
        } else {
            return BeliefTable.Empty;
        }
    }

    @Override
    public TemporalBeliefTable newTemporalTable(Term c) {
//        if (c.complexity() < 12) {
        return RTreeBeliefTable.build(c);
        //c.complexity() < 6 ? new DisruptorBlockingQueue() : new LinkedBlockingQueue<>()/
//        } else {
//            return new HijackTemporalBeliefTable();
//        }
    }

    @Override
    public QuestionTable questionTable(Term term, boolean questionOrQuest) {
        Op o = term.op();
        if (questionOrQuest ? o.beliefable : o.goalable) {
            //return new HijackQuestionTable(0, 4);
            return new QuestionTable.DefaultQuestionTable();
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

//    public boolean concurrent() {
//        return nar.exe.concurrent();
//    }

//    private class MyDefaultHijackBag extends DefaultHijackBag {
//        public MyDefaultHijackBag(PriMerge merge) {
//            super(merge, 0, 5);
//        }
//
//        @Override
//        protected Random random() {
//            return nar.random();
//        }
//    }
}
