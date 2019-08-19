package nars.table.temporal;

import jcog.Skill;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.pri.Prioritized;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Task;
import nars.control.CauseMerge;
import nars.table.BeliefTable;
import nars.task.NALTask;
import nars.truth.proj.TruthProjection;

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
        whileEach(x -> {
            if (!x.isDeleted() && x.intersects(minT, maxT))
                return each.test(x);
            else
                return true;
        });
    }

    static void budget(TruthProjection sources, Task xy) {

        //sources.removeNulls();
        Task[] tr = sources.arrayCommit(); assert(ArrayUtil.indexOfIdentity(tr, null)==-1);

        ((NALTask)xy).cause(CauseMerge.AppendUnique.merge(NAL.causeCapacity.intValue(), tr));

        float priSum = Util.sum(Prioritized::priElseZero, tr);
        float priMean = priSum/tr.length; //mean
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

        forEachTask(finalStart, finalEnd, t->{
            if (remove.test(t))
                deleteAfter.add(t);
        });

        if (!deleteAfter.isEmpty())
            deleteAfter.forEach((t) -> removeTask(t, true));
    }
}
