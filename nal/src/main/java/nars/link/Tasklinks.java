package nars.link;

import jcog.bag.Bag;
import jcog.pri.ScalarValue;
import jcog.util.NumberX;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class Tasklinks {

    public static void linkTask(TaskLink xx, Bag<?, TaskLink> b, @Nullable NumberX overflow) {

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


        TaskLink.GeneralTaskLink link = new TaskLink.GeneralTaskLink(t, nar, pri);

        linkTask(link, src.tasklinks(), null);

        ((TaskConcept) src).value(t, pri, nar);

        return link;
    }


    /**
     * create a batch of tasklinks, sharing common seed data
     */
    public static void linkTask(TaskLink.GeneralTaskLink tasklink, float priTransferred, List<Concept> targets, NumberX overflow) {
        int nTargets = targets.size();
        assert(nTargets > 0);

        float pEach = Math.max(ScalarValue.EPSILON,
                priTransferred / nTargets
        );

        TaskLink.Tasklike tlSeed = tasklink.id;

        final float headRoom = 1f - pEach;

        for (Concept c : targets) {


            float result;
            float available = overflow.floatValue();
            if (available > headRoom) {
                //take some
                overflow.add(-headRoom);
                result = headRoom;
            } else {
                //take all
                overflow.set(0f);
                result = available;
            }
            float change = result;

            linkTask(
                new TaskLink.GeneralTaskLink(tlSeed, pEach + change),
                    c.tasklinks(), overflow);

        }
    }


}
