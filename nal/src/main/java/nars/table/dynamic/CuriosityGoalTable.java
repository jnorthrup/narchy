package nars.table.dynamic;

import nars.NAR;
import nars.Param;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.task.series.ConcurrentSkiplistTaskSeries;
import nars.util.task.series.TaskSeries;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentSkipListMap;

/** goal belief table with side-table for curiosity tasks.  this is so they can be overridden
 * by weaker derivations, and so that they dont interfere with weaker derivations once
 * capacity has been reched.  */
public class CuriosityGoalTable extends SeriesBeliefTable<SeriesBeliefTable.SeriesTask> {

    public CuriosityGoalTable(Term term, boolean beliefOrGoal, NAR n) {
        this(term, beliefOrGoal, n.conceptBuilder.newTemporalTable(term), n);
    }

    public CuriosityGoalTable(Term c, boolean beliefOrGoal, TemporalBeliefTable t, NAR n) {
        this(c, beliefOrGoal,
                //TODO impl time series with concurrent ring buffer from gluegen
                t, new ConcurrentSkiplistTaskSeries<SeriesBeliefTable.SeriesTask>(new ConcurrentSkipListMap<>(), /*@Deprecated*/ Param.CURIOSITY_BELIEF_TABLE_SERIES_SIZE) {

                    /** to prevent revision of curiosity, and further derivation of them */
                    //final long[] sharedStamp = n.evidence();

                    @Override
                    public SeriesBeliefTable.SeriesTask newTask(Term term, byte punc, long nextStart, long nextEnd, Truth next, NAR nar, boolean removePrev, long[] lastStamp) {
                        SeriesBeliefTable.SeriesTask t = new SeriesBeliefTable.SeriesTask(
                                term,
                                punc,
                                next,
                                nextStart, nextEnd,
//                                sharedStamp[0]
                                nar.evidence()
                                //Stamp.UNSTAMPED
                        );
                        //t.setCyclic(true); //further restrict
                        return t;
                    }
                });
    }

    CuriosityGoalTable(Term c, boolean beliefOrGoal, TemporalBeliefTable t, TaskSeries<SeriesTask> series) {
        super(c, beliefOrGoal, EternalTable.EMPTY, t, series);
    }

    /** prefer stored (derived) truth before dynamic (curiosity) */
    @Override @Nullable public Truth truth(long start, long end, Term template, NAR nar) {
        Truth stored = truthStored(start, end, template, nar);
        if (stored!=null)
            return stored;

        return truthDynamic(start, end, template, nar);
    }


//        @Override
//    public Task match(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
//        Task stored = taskStored(start, end, template, filter, nar);
//        if (stored!=null)
//            return stored;
//
//        return taskDynamic(start, end, template, nar);
//    }
}
