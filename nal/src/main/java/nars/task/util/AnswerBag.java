package nars.task.util;

import jcog.bag.impl.PLinkArrayBag;
import jcog.data.map.SynchronizedHashMap;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.op.Operator;
import org.eclipse.collections.api.tuple.Twin;
import org.jetbrains.annotations.NotNull;

/** stores Question & Answer pairs per concept */
public final class AnswerBag extends PLinkArrayBag<Twin<Task>> {

    private final NAR nar;

    public AnswerBag(@NotNull NAR nar, int capacity) {
        super(capacity, PriMerge.max, new SynchronizedHashMap<>(capacity));

        this.nar = nar;
    }

    @Override
    public void onAdd(@NotNull PriReference<Twin<Task>> x) {
        if (Param.DEBUG_REPORT_ANSWERS) {
            Twin<Task> qa = x.get();
            nar.input(Operator.log(nar.time(), qa.getOne() + "  " + qa.getTwo()));
        }
    }
}
