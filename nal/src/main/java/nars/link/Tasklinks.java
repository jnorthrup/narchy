package nars.link;

import jcog.bag.Bag;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.Random;


public class Tasklinks {

    public static void linkTask(TaskLink xx, Bag<?, TaskLink> b, @Nullable MutableFloat overflow) {

        if (overflow != null) {
            TaskLink yy = b.put(xx, overflow);
            if (yy != xx && yy != null) {


            }
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
     * propagate tasklink to templates
     */
    public static void linkTask(TaskLink tasklink, float priTransferred, Concept[] targets, Random rng) {
        int nTargets = targets.length;
        if (nTargets <= 0)
            return;

        float pEach = Math.max(Prioritized.EPSILON,
                priTransferred / nTargets

        );
        {

            TaskLink.Tasklike tlSeed =
                    ((TaskLink.GeneralTaskLink) tasklink).id;

            final float headRoom = 1f - pEach;
            MutableFloat overflow = new MutableFloat();


            int j = rng.nextInt(nTargets);
            for (Concept target: targets) {


                float change = overflow.subAtMost(headRoom);

                TaskLink xx =

                        new TaskLink.GeneralTaskLink(tlSeed, pEach + change);

                linkTask(xx, targets[j++].tasklinks(), overflow);

                if (j == nTargets) j = 0;
            }
        }
    }


}
