package nars.concept.action;

import nars.NAR;
import nars.Op;
import nars.Task;
import nars.control.proto.Remember;
import nars.link.TermLinker;
import nars.table.BeliefTables;
import nars.table.dynamic.SensorBeliefTables;
import nars.table.dynamic.SeriesBeliefTable;
import nars.table.temporal.RTreeBeliefTable;
import nars.task.signal.SignalTask;
import nars.task.util.Answer;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.polation.TruthPolation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class AbstractGoalActionConcept extends ActionConcept {



    /** current calculated goalTask */
    protected volatile @Nullable Truth actionTruth;

    /** truth calculated (in attempt to) excluding curiosity */
    protected volatile @Nullable Truth actionDex;


    public AbstractGoalActionConcept(Term c, TermLinker linker, NAR n) {
        super(c, linker, n);
    }

    protected AbstractGoalActionConcept(Term term, SensorBeliefTables sensorBeliefTables, BeliefTables newTable, NAR n) {
        super(term, sensorBeliefTables, newTable, n);


    }

    @Override
    public float dexterity() {
        Truth t = this.actionDex;
        return t!=null ? t.conf() : 0;
    }


    /** in cycles; controls https://en.wikipedia.org/wiki/Legato vs. https://en.wikipedia.org/wiki/Staccato */
    int actionSustain =
            //0;
            -1;

    public AbstractGoalActionConcept actionDur(int actionDur) {
        this.actionSustain = actionDur;
        return this;
    }


    @Override
    public void update(long prev, long now, long next, NAR n) {



        //TODO mine truthpolation .stamp()'s and .cause()'s for clues

        Predicate<Task> withoutCuriosity = t -> !(t instanceof CuriosityTask);  /* filter curiosity tasks? */


        int actionDur = this.actionSustain;
        if (actionDur < 0) actionDur = n.dur();

        //long rad = (now - prev);
        //long s = prev, e = now;
        //long s = now, e = next;
        long s = prev, e = next;
//        long s = now - rad;
//        long e = now + rad;
//                //0;
//                //1;
//                //Tense.occToDT(rad); //controls fall-off / bleed-through of goal across time

        int limit = Answer.TASK_LIMIT_DEFAULT;

        TruthPolation a = Answer.
                relevance(true, limit, s, e, term, null, n).match(goals()).truthpolation(actionDur);
        if (a!=null) {
            a = a.filtered();
            actionTruth = a.truth();

//            System.out.println(actionNonAuthentic);
//            aWithCuri.print();
//            System.out.println();
//            System.out.println();

        } else
            actionTruth = null;

        TruthPolation aWithoutCuri = Answer.
                relevance(true, limit, s, e, term, withoutCuriosity, n).match(goals()).truthpolation(actionDur);
        if (aWithoutCuri!=null) {


            //aWithoutCuri = aWithoutCuri.filtered();
            actionDex = aWithoutCuri.truth();
        } else {
            actionDex = null;
        }


//        if (a!=null) {
//            System.out.println(a);
//        }
    }


    @Override
    public void add(Remember t, NAR n) {
        if (t.input.isGoal()) {
            RTreeBeliefTable gg = goals().tableFirst(RTreeBeliefTable.class);
            if (gg.size()+1 >= gg.capacity()) {
                //HACK
                //search for oldest curiosity task to eliminate
                TaskRegion bb = gg.bounds();
                if (bb!=null) {
                    long s = bb.start();
                    Task tc = Answer.relevance(true, 1,
                            s, s, term, x -> x instanceof CuriosityTask, n)
                            .match(gg).any();
                    if (tc!=null) {
                        gg.removeTask(tc);
                        t.forget(tc );
                        return;
                    }
                }

//                if (bb!=null) {
//                    bb.start()
//                }
            }
        }
        super.add(t, n);
    }

    @Nullable SignalTask curiosity(Truth goal, long pStart, long pEnd, NAR n) {
//        if (goal!=null) {
//            SignalTask curiosity = new CuriosityTask(term, goal, n, pStart, pEnd);
//
//            if (curiosity != null) {
//                curiosity.pri(n.priDefault(GOAL));
//                return curiosity;
//                //return curiosity.input(c);
//            }
//        }

        return null;

    }

    @Nullable public SeriesBeliefTable.SeriesRemember feedback(@Nullable Truth f, long now, long next, float dur, NAR nar) {
        return ((SensorBeliefTables) beliefs()).add(f, now, next, this, dur, nar);
    }


    public Truth actionTruth() {
        return actionTruth;
    }

    protected float curiConf;
    protected volatile float curiosityRate = 0;
    public void curiosity(float curiRate, float curiConf) {
        this.curiosityRate = curiRate;
        this.curiConf = curiConf;
    }

    public static class CuriosityTask extends SignalTask {
        public CuriosityTask(Term term, Truth goal, NAR n, long pStart, long pEnd) {
            super(term, Op.GOAL, goal, n.time(), pStart, pEnd, n.evidence());
        }
    }

}
