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

    private final long[] sharedCuriosityEvidence;

    public AbstractGoalActionConcept(Term c, TermLinker linker, NAR n) {
        super(c, linker, n);
        this.sharedCuriosityEvidence =
                n.evidence();
                //Stamp.UNSTAMPED;
    }

    protected AbstractGoalActionConcept(Term term, SensorBeliefTables sensorBeliefTables, BeliefTables newTable, NAR n) {
        super(term, sensorBeliefTables, newTable, n);
        this.sharedCuriosityEvidence = n.evidence();
    }

    @Override
    public float dexterity(long start, long end, NAR n) {
        Truth t = this.actionTruth;
        return t!=null ? t.conf() : 0;
    }

    @Override
    public void update(long prev, long now, long next, NAR nar) {

        //TODO mine truthpolation .stamp()'s and .cause()'s for clues

        Predicate<Task> withoutCuriosity = t -> t.stamp() != sharedCuriosityEvidence;  /* filter curiosity tasks? */

        TruthPolation a = Answer.relevance(true, prev, now /*next*/, term, withoutCuriosity, nar).match(goals()).truthpolation();
        if (a == null) {
            //try again , allowing curiosity
            a = Answer.relevance(true, prev, now /*next*/, term, null, nar).match(goals()).truthpolation();
        }

        if (a!=null)
            a = a.filtered();

        action = a;
        actionTruth = a!=null ? a.truth() : null;
//        if (a!=null) {
//            System.out.println(a);
//        }
    }


    @Nullable SeriesBeliefTable.SeriesTask curiosity(Truth goal, long pStart, long pEnd, NAR nar) {
        SeriesBeliefTable.SeriesTask curiosity = new SeriesBeliefTable.SeriesTask(term, GOAL, goal, pStart, pEnd, sharedCuriosityEvidence);

//        SeriesBeliefTable.SeriesTask curiosity =
//                goals().series.add(term, GOAL, pStart, pEnd, truth, nar.dur(), nar);

        if (curiosity!=null) {
            curiosity.pri(nar.priDefault(GOAL));
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

}
