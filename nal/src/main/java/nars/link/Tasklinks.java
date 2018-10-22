package nars.link;

import jcog.data.NumberX;
import jcog.pri.bag.Bag;
import nars.concept.Concept;
import nars.task.Tasklike;
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
     * create a batch of tasklinks, sharing common seed data
     */
    public static void linkTask(TaskLink.GeneralTaskLink tasklink, float pri, List<Concept> targets, NumberX overflow) {
        int nTargets = targets.size();
        assert(nTargets > 0);

//        float pEach = Math.max(ScalarValue.EPSILON,
//                priTransferred / nTargets
//        );
        float pEach =
                //pri; //no division
                pri/nTargets; //no division

        Tasklike tlSeed = tasklink.id;

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
