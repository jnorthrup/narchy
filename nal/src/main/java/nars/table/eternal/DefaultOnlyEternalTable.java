package nars.table.eternal;

import nars.$;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.task.EternalTask;
import nars.truth.Truth;

import java.util.List;

import static nars.Op.BELIEF;

/** provides an overriding eternal default answer only if the Answer has found no other options in other tables.
 *  should be added only to the end of BeliefTables */
public class DefaultOnlyEternalTable extends EternalTable {

    public DefaultOnlyEternalTable(Concept c, Truth t, What w) {
        super(1);

        List<BeliefTable> tables = ((BeliefTables) c.beliefs());
        assert(!tables.isEmpty()): "other tables should precede this in BeliefTables chain";

        tables.add(this);

        //TODO just direct insert
        Task belief = new EternalTask(c.term(), BELIEF, $.t(t.freq(), t.conf()), w.nar);
        w.in.put(belief);
//        assert(!belief.isDeleted());
//        assert(!isEmpty());
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
