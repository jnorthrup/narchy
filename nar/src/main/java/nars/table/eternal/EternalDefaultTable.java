package nars.table.eternal;

import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTaskTable;
import nars.task.EternalTask;
import nars.task.util.Answer;
import nars.truth.MutableTruth;
import nars.truth.Truth;

import static nars.Op.BELIEF;

/** provides an overriding eternal default answer only if the Answer has found no other options in other tables.
 *  should be added only to the end of BeliefTables
 *  TODO separate the strong,weak functionality into a subclass. leave the superclass use the default functionality
 *  */
public class EternalDefaultTable extends DynamicTaskTable {

    private final EternalTask task;
    public final MutableTruth truth;

    private EternalDefaultTable(Concept c, Truth t, byte punc, NAR n) {
        super(c.term(), (int) punc == (int) BELIEF);

        long[] stamp = n.evidence();
        long creation = n.time();

        {
            EternalTask tt = new EternalTask(c.term(), punc, truth = new MutableTruth(t), creation, stamp);
            tt.pri(n.priDefault(punc));
            this.task = tt;
        }


    }

    public static EternalDefaultTable add(Concept c, float freq, NAR n) {
        return add(c, $.t(freq, n.beliefConfDefault.conf()), n);
    }

    public static EternalDefaultTable add(Concept c, Truth t, NAR n) {
        return add(c, t, BELIEF, n);
    }

    public static EternalDefaultTable add(Concept c, Truth t, byte punc, NAR n) {
        EternalDefaultTable tb = new EternalDefaultTable(c, t, punc, n);

        BeliefTables tables = (BeliefTables) c.table(punc);

        tables.add(tb);

        return tb;
    }

    @Override
    public void match(Answer a) {
        a.test(task);
    }

}
