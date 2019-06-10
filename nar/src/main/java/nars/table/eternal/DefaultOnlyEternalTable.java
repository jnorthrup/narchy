package nars.table.eternal;

import jcog.math.LongInterval;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.table.BeliefTables;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.EternalTask;
import nars.task.util.Answer;
import nars.truth.Truth;

import static nars.Op.BELIEF;

/** provides an overriding eternal default answer only if the Answer has found no other options in other tables.
 *  should be added only to the end of BeliefTables */
public class DefaultOnlyEternalTable extends EternalTable {

    private DefaultOnlyEternalTable(Concept c, Truth t, NAR n) {
        super(1);


        //TODO just direct insert
        Task tt = new EternalTask(c.term(), BELIEF, t, n);
        tt.pri(n.priDefault(BELIEF));
        addEnd(tt, 1);

        //n.input(belief);
        //w.in.put(belief);
//        assert(!belief.isDeleted());
//        assert(!isEmpty());
    }

    public static DefaultOnlyEternalTable add(Concept c, float freq, NAR n) {
        return add(c, $.t(freq, n.beliefConfDefault.conf()), n);
    }

    public static DefaultOnlyEternalTable add(Concept c, Truth t, NAR n) {
        DefaultOnlyEternalTable tb = new DefaultOnlyEternalTable(c, t, n);

        BeliefTables tables = (BeliefTables) c.beliefs();
        assert(!tables.isEmpty()): "other tables should precede this in BeliefTables chain";

        tables.add(tb);

        return tb;
    }

    @Override
    public void match(Answer a) {
        for (Task x : a.tasks)
            if (x instanceof SeriesBeliefTable.SeriesTask && a.time./*intersects*/ contains((LongInterval)x))
                return; //dont match if no other signal task has been matched

        super.match(a);
    }

    //    @Override
//    public void match(Answer a) {
//        if (preFilter(a))
//            super.match(a);
//    }
//
//    @Override
//    public void sample(Answer a) {
//        if (preFilter(a))
//            super.sample(a);
//    }

//    private boolean preFilter(Answer a) {
////        if (!a.tasks.isEmpty()) {
////            long s = a.time.start;
////            if (s != ETERNAL) {
////                long e = a.time.end;
////                Task ee = get(0);
////                if (a.filter==null || a.filter.test(ee)) {
//////                    float minEvi = ee.evi();
////                    if (!a.tasks.whileEach(t -> !(t.isInput() && t.intersects(s, e)))) {//break if FOUND relevant match, otherwise continue
////                        //TODO the matched eternal should depend on how much of the range is covered. use the ETE to fill in the empty amounts as one correctly evidenced aggregate
////                        return true;
////                    }
////
////                    a.tasks.clear(); //eternal overrides
////                    a.triesRemain = 1;
////                }
////            }
////        }
//        return true;
//    }

}
