package nars.concept.action;

import nars.NAR;
import nars.Task;
import nars.link.TermLinker;
import nars.table.BeliefTables;
import nars.table.dynamic.SensorBeliefTables;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.util.Answer;
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
    protected volatile @Nullable TruthPolation action;
    protected volatile @Nullable Truth actionTruth;

    /** truth calculated (in attempt to) excluding curiosity */
    protected volatile @Nullable Truth actionTruthAuthentic;


    public AbstractGoalActionConcept(Term c, TermLinker linker, NAR n) {
        super(c, linker, n);
    }

    protected AbstractGoalActionConcept(Term term, SensorBeliefTables sensorBeliefTables, BeliefTables newTable, NAR n) {
        super(term, sensorBeliefTables, newTable, n);


    }

    @Override
    public float dexterity(long start, long end, NAR n) {
        Truth t = this.actionTruthAuthentic;
        return t!=null ? t.conf() : 0;
    }

    @Override
    public void update(long prev, long now, long next, NAR n) {



        //TODO mine truthpolation .stamp()'s and .cause()'s for clues

        Predicate<Task> withoutCuriosity = t -> !(t instanceof SeriesBeliefTable.SeriesTask);  /* filter curiosity tasks? */


        long rad = (now - prev) / 2;
        long s = now - rad;
        long e = now + rad;

        TruthPolation aWithCuri = Answer.
                relevance(true, s, e, term, null, n).match(goals()).truthpolation();
        Truth actionNonAuthentic;
        if (aWithCuri!=null) {
            aWithCuri = aWithCuri.filtered();
            actionNonAuthentic = aWithCuri.truth();
        } else
            actionNonAuthentic = null;

        TruthPolation aWithoutCuri = Answer.
                relevance(true, s, e, term, withoutCuriosity, n).match(goals()).truthpolation();
        if (aWithoutCuri!=null) {
            aWithoutCuri = aWithoutCuri.filtered();
            actionTruth = actionTruthAuthentic = aWithoutCuri.truth();
            action = aWithoutCuri;
        } else {
            actionTruthAuthentic = null;
            actionTruth = actionNonAuthentic;
            action = aWithCuri;
        }

//        if (a!=null) {
//            System.out.println(a);
//        }
    }



    @Nullable SeriesBeliefTable.SeriesTask curiosity(Truth goal, long pStart, long pEnd, NAR n) {
        SeriesBeliefTable.SeriesTask curiosity = new SeriesBeliefTable.SeriesTask(term, GOAL, goal, pStart, pEnd,
                n.evidence());

        if (curiosity!=null) {
            curiosity.pri(n.priDefault(GOAL));
            return curiosity;
            //return curiosity.input(c);
        } else {
            return null;
        }

    }

    @Nullable public SeriesBeliefTable.SeriesRemember feedback(@Nullable Truth f, long now, long next, NAR nar) {
        return ((SensorBeliefTables) beliefs()).add(f, now, next, this, nar);
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

}
