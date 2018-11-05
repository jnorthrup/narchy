package nars.link;

import jcog.data.NumberX;
import jcog.pri.ScalarValue;
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
        assert(!targets.isEmpty());

//        float pEach = Math.max(ScalarValue.EPSILON,
//                priTransferred / nTargets
//        );
        float pEach =
                //TODO abstract priority transfer function here
                pri; //no division
                //pri/targets.size(); //division

        Tasklike tlSeed = tasklink.id;

        final float headRoom = 1f - pEach;

        for (Concept c : targets) {

            float p = pEach;

            float take = Math.min(overflow.floatValue(), headRoom);
            if (take > ScalarValue.EPSILON) {
                overflow.add(-take);
                p += take;
            }

            linkTask(
                new TaskLink.GeneralTaskLink(tlSeed, p),
                c.tasklinks(), overflow);

        }
    }


}
