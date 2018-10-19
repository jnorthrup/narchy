package nars.concept.action;

import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.link.TermLinker;
import nars.table.BeliefTable;
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

import static nars.Op.GOAL;


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

    protected AbstractGoalActionConcept(Term term, SensorBeliefTables sensorBeliefTables, BeliefTable newTable, NAR n) {
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

        Predicate<Task> withoutCuriosity = t -> !(t instanceof CuriosityTask) && !t.isEternal();  /* filter curiosity tasks? */



        //long s = prev, e = now;
        //long s = now, e = next;
        long s = prev, e = next;
        //long agentDur = (now - prev);
        //long s = now - agentDur/2, e = now + agentDur/2;

        int actionDur = this.actionSustain;
        if (actionDur < 0)
            actionDur =
                    n.dur();
                    //Tense.occToDT(agentDur);

        int limit = Answer.TASK_LIMIT_DEFAULT;

        BeliefTable table = goals();


        Answer o = Answer.
                relevance(true, limit, s, e, term, withoutCuriosity, n);
        o.triesRemain = table.size()+1; //dont give up, otherwise the filtered curiosity tasks will consume the remaining tries
        TruthPolation organic = o.match(table).truthpolation(actionDur);
        if (organic!=null) {
            actionDex = organic.filtered().truth();
        } else {
            actionDex = null;
        }


        TruthPolation raw = Answer.
                relevance(true, limit, s, e, term, null, n).match(table).truthpolation(actionDur);
        if (raw!=null) {
             actionTruth = raw.filtered().truth();
        } else
            actionTruth = null;



        //if this happens, for whatever reason..
        if (actionTruth == null && actionDex!=null)
            actionTruth = actionDex;
    }


    @Override
    public void add(Remember t, NAR n) {
        if (t.input.isGoal()) {
            RTreeBeliefTable gg = ((BeliefTables)goals()).tableFirst(RTreeBeliefTable.class);
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
        if (goal!=null) {
            SignalTask curiosity = new CuriosityTask(term, goal, n, pStart, pEnd);

            if (curiosity != null) {
                curiosity.pri(n.priDefault(GOAL));
                return curiosity;
                //return curiosity.input(c);
            }
        }

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
            super(term, GOAL, goal, n.time(), pStart, pEnd, n.evidence());
        }
    }

}
