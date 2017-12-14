package nars.concept.builder;

import jcog.bag.impl.CurveBag;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.PriReference;
import nars.Param;
import nars.Task;
import nars.link.Tasklinks;

import java.util.Map;
import java.util.Random;

public class TaskLinkCurveBag extends CurveBag<PriReference<Task>> {
    public TaskLinkCurveBag(Map sharedMap, Random rng) {
        super(Param.tasklinkMerge, sharedMap, rng, 0);
    }

    @Override
    public void onRemove(PriReference<Task> value) {
        float p = ((PLinkUntilDeleted) value).priBeforeDeletion;
        if (p == p) {
            // this link was deleted due to the referent being deleted,
            // not because the link was deleted.
            // so see if a forwarding exists

            Task x = value.get();
            Task px = x;
            Task y;

            //TODO maybe a hard limit should be here for safety in case anyone wants to create loops of forwarding tasks
            int hopsRemain = Param.MAX_TASK_FORWARD_HOPS;
            do {
                y = x.meta("@");
                if (y != null)
                    x = y;
            } while (y != null && --hopsRemain > 0);

            if (x != px && !x.isDeleted()) {
                Tasklinks.linkTask(x, p, this);
            }
        }

    }
}
