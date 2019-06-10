package nars.table.eternal;

import jcog.Util;
import jcog.math.LongInterval;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTaskTable;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.EternalTask;
import nars.task.util.Answer;
import nars.truth.Truth;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

/** provides an overriding eternal default answer only if the Answer has found no other options in other tables.
 *  should be added only to the end of BeliefTables */
public class EternalDefaultTable extends DynamicTaskTable {

    private final Task strong, weak;
//    boolean applyEternal = true;
//    boolean applyPast = true;
//    boolean applyFuture = true;

    private EternalDefaultTable(Concept c, Truth t, NAR n) {
        super(c.term(), true);

        long[] stamp = n.evidence();
        long creation = n.time();

        {
            Task tt = new EternalTask(c.term(), BELIEF, t, creation, stamp);
            tt.pri(n.priDefault(BELIEF));
            this.strong = tt;
        }

        {
            Truth tWeak = //t.eternalized(1, n);
                    $.t(t.freq(), Util.lerp(0.1f, n.confMin.floatValue(), t.conf()));
            Task tt = new EternalTask(c.term(), BELIEF, tWeak, creation, stamp);
            tt.pri(n.priDefault(BELIEF));
            this.weak = tt;
        }
        //n.input(belief);
        //w.in.put(belief);
//        assert(!belief.isDeleted());
//        assert(!isEmpty());
    }

    public static EternalDefaultTable add(Concept c, float freq, NAR n) {
        return add(c, $.t(freq, n.beliefConfDefault.conf()), n);
    }

    public static EternalDefaultTable add(Concept c, Truth t, NAR n) {
        EternalDefaultTable tb = new EternalDefaultTable(c, t, n);

        BeliefTables tables = (BeliefTables) c.beliefs();
        assert(!tables.isEmpty()): "other tables should precede this in BeliefTables chain";

        tables.add(tb);

        return tb;
    }

    @Override
    public void match(Answer a) {
        boolean weak = false;
        boolean ae = a.time.start == ETERNAL;
        if (ae) {
//            if (!applyEternal)
//                return;
        } else {
            long now = a.nar.time();
            int dur = a.dur;
            if (a.time.end >= now-dur/2 ) {
                weak = true;

            }
//            if (!applyPast && a.time.start <= now - dur / 2)
//                return;
        }

        if (!weak) {
            for (Task x : a.tasks)
                if (x instanceof SeriesBeliefTable.SeriesTask && a.time.intersects /*contains*/((LongInterval) x)) {
                    weak = true;
                    break;
                }
        }


        a.tryAccept(weak ? this.weak : strong);
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
