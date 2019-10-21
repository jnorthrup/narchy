package nars.table.temporal;

import jcog.Skill;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.pri.Prioritized;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Task;
import nars.control.Why;
import nars.table.BeliefTable;
import nars.task.AbstractTask;
import nars.truth.proj.TruthProjection;
import org.eclipse.collections.api.block.procedure.Procedure;

import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * https://en.wikipedia.org/wiki/Compressed_sensing
 */
@Skill("Compressed_sensing")
public interface TemporalBeliefTable extends BeliefTable {


    void setTaskCapacity(int temporals);

    long tableDur(long now);

    void whileEach(Predicate<? super Task> each);

    /**
     * finds all temporally intersectnig tasks.  minT and maxT inclusive.  while the predicate remains true, it will continue scanning
     * TODO contains/intersects parameter
     */
    default void whileEach(long minT, long maxT, Predicate<? super Task> each) {
        whileEach(new Predicate<Task>() {
            @Override
            public boolean test(Task x) {
                return !x.isDeleted() && x.intersects(minT, maxT) ? each.test(x) : true;
            }
        });
    }

    static void budget(TruthProjection sources, Task xy) {

        //sources.removeNulls();
        Task[] tr = sources.arrayCommit(); assert(ArrayUtil.indexOfInstance(tr, null)==-1);

        ((AbstractTask)xy).why(Why.why(tr, NAL.causeCapacity.intValue()));

        float priSum = Util.sum(Prioritized::priElseZero, tr);
        float priMean = priSum/ (float) tr.length; //mean
        xy.priAdd(priMean);

//        //factor in the evidence loss (and originality?) loss to reduce priority
//        float exy = TruthIntegration.evi(xy);
//        long xys = xy.start();
//        long xye = xy.end();
//        float eXplusY = sources.TruthIntegration.evi(x, xys, xye, 0) + TruthIntegration.evi(y, xys, xye, 0);
//
//        //assert(eXplusY >= exy);
//        float pFactor = Math.min(1, exy / eXplusY);
//
//        //assert(pFactor <= 1f);
//        //float oxy = xy.originality();
//        //float px = Util.unitize(exy/ eviInteg(x) ); // * (oxy * x.originality()));
//        //float py = Util.unitize(exy/ eviInteg(y) ); // * (oxy * y.originality()));
//        xy.take(x, pFactor/2, false, false);
//        r.forget(x);
//        xy.take(y, pFactor/2, false, false);
//        r.forget(y);


    }


	default void removeIf(Predicate<Task> remove, long finalStart, long finalEnd) {
        FasterList<Task> deleteAfter = new FasterList<Task>(0);

        forEachTask(finalStart, finalEnd, new Consumer<Task>() {
            @Override
            public void accept(Task t) {
                if (remove.test(t))
                    deleteAfter.add(t);
            }
        });

        if (!deleteAfter.isEmpty())
            deleteAfter.forEach(new Procedure<Task>() {
                @Override
                public void value(Task t) {
                    TemporalBeliefTable.this.removeTask(t, true);
                }
            });
    }
}
