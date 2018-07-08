package nars.link;

import jcog.bag.Bag;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class Tasklinks {

    public static void linkTask(TaskLink xx, Bag<?, TaskLink> b, @Nullable MutableFloat overflow) {

        if (overflow != null) {
            TaskLink yy = b.put(xx, overflow);
//            if (yy != xx && yy != null) {
//
//
//            }
        } else {
            b.putAsync(xx);
        }
    }


    /**
     * create source tasklink
     */
    public static TaskLink.GeneralTaskLink linkTask(Task t, final float pri, /*Task*/Concept src, NAR nar) {


        float taskLinkPri = pri;

        TaskLink.GeneralTaskLink link = new TaskLink.GeneralTaskLink(t, nar, taskLinkPri);

        linkTask(link, src.tasklinks(), null);


        ((TaskConcept) src).value(t, taskLinkPri, nar);



        //nar.eventTask.emit(t);

        return link;
    }


    /**
     * create a batch of tasklinks, sharing common seed data
     */
    public static void linkTask(TaskLink.GeneralTaskLink tasklink, float priTransferred, List<Concept> targets, MutableFloat overflow) {
        int nTargets = targets.size();
        assert(nTargets > 0);

        float pEach = Math.max(Prioritized.EPSILON,
                priTransferred / nTargets
        );

        TaskLink.Tasklike tlSeed = tasklink.id;

        final float headRoom = 1f - pEach;

        for (Concept c : targets) {

            float change = overflow.subAtMost(headRoom);

            linkTask(
                new TaskLink.GeneralTaskLink(tlSeed, pEach + change),
                    c.tasklinks(), overflow);

        }
    }


}
