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
            if (yy!=xx && yy!=null) {
//
//                //if the tasks are different, merge the priorities to the linked one
//                Task y = yy.get();
//                if (y!=null && y!=x) {
//                    Param.taskMerge.merge(y, x);
//                }
            }
        } else {
            b.putAsync(xx);
        }
    }


//    public static TaskLink.GeneralTaskLink linkTask(Task t,  /*Task*/Concept src, @Nullable NAR nar) {
//        return linkTask(t, t.priElseZero(), src, nar);
//    }

    public static TaskLink.GeneralTaskLink linkTask(Task t, /*Task*/Concept src, NAR nar) {
        return linkTask(t, t.priElseZero(), src, nar);
    }

    /** create source tasklink */
    public static TaskLink.GeneralTaskLink linkTask(Task t, final float _pri, /*Task*/Concept src, NAR nar) {

        /** non-zero for safety */
        final float pri = Math.max(_pri, Prioritized.EPSILON);


        float taskLinkPri = pri;

        TaskLink.GeneralTaskLink link = new TaskLink.GeneralTaskLink(t, nar, taskLinkPri);

        linkTask(link, src.tasklinks(), null);



        {
            //adjust the cause values according to the input's actual demand
            ((TaskConcept) src).value(t, taskLinkPri, nar);

            //adjust the experienced emotion according to the actual effect
            nar.emotion.onActivate(t, pri);
        }


        //activation is the ratio between the effective priority and the input priority, a value between 0 and 1.0
        //it is a measure of the 'novelty' of a task as reduced by the priority of an equivalent existing tasklink
//        float effectiveness = priEffect / priCause;
//        if (effectiveness >= Param.TASK_ACTIVATION_THRESHOLD)


            nar.eventTask.emit(t);

            return link;
    }

//    public static void linkTask(TaskLink tasklink, float priTransferred, FasterList<Concept> targets) {
//        linkTask(tasklink, priTransferred, targets.toArrayRecycled(Concept[]::new));
//    }

    /** propagate tasklink to templates */
    public static void linkTask(TaskLink tasklink, float priTransferred, Concept[] targets, Random rng) {
        int nTargets = targets.length;
        if (nTargets <= 0)
            return;

        float pEach = Math.max(Prioritized.EPSILON,
                priTransferred / nTargets  //divided
                //priTransferred //keep original priority
        );
        {

            TaskLink.Tasklike tlSeed =
                    ((TaskLink.GeneralTaskLink)tasklink).get();

            final float headRoom = 1f - pEach;
            MutableFloat overflow = new MutableFloat();

            //shuffle the offset to spread the link activation fairly, when budget backpressure (change) is involved
            //TODO shuffle iteration direction
            int j = rng.nextInt(nTargets);
            for (int i = 0; i < nTargets; i++) {

//                float o = overflow.get();
//
//                //spread overflow of saturated targets to siblings
//                float change;
//                if (o >= Pri.EPSILON) {
//                    overflow.subtract(change = Math.min(o, headRoom));
//                } else {
//                    change = 0;
//                }
                float change = overflow.subAtMost(headRoom);

                TaskLink xx =
                        //new TaskLink.DirectTaskLink(t, pEach + change);
                        new TaskLink.GeneralTaskLink(tlSeed, pEach + change);

                linkTask(xx, targets[j++].tasklinks(), overflow);

                if (j == nTargets) j = 0;
            }
        }
    }

//    @Deprecated public static void linkTask(Task x, float p, Bag b) {
//        TaskLink xx =
//                new TaskLink.DirectTaskLink(x, p);
//                //new TaskLink.GeneralTaskLink(x, p);
//
//        linkTask(xx, b, null);
//    }


//    public static void linkTask(Task t, Concept cc, NAR nar) {
//        float p = t.pri();
//        if (p == p)
//            linkTask(t, p, cc, nar);
//    }

//    public static void linkTask(Task task, Collection<Concept> targets) {
//        int numSubs = targets.size();
//        if (numSubs == 0)
//            return;
//
//        float tfa = task.priElseZero();
//        float tfaEach = tfa / numSubs;
//
//
//        for (Concept target : targets) {
//
//            linkTask(task, tfaEach, target);
////                target.termlinks().putAsync(
////                        new PLink(task.term(), tfaEach)
////                );
//
//
//        }
//    }

//    public static class ForgetNonPresentTasklinks extends PriForget<PriReference<Task>> {
//        private final long now;
//        private final int dur;
//
//        public ForgetNonPresentTasklinks(float r, long now, int dur) {
//            super(r);
//            this.now = now;
//            this.dur = dur;
//        }
//
//        @Override
//        public void accept(PriReference<Task> b) {
//            Task t = b.get();
//            float rate;
//            if (t.isBeliefOrGoal()) {
//                //decrease rate in proximity to now or the future
//                if (t.isEternal() || !t.isBefore(now - dur))
//                    rate = 0.5f; //slower forget
//                else {
//                    rate = 1f; //full forget
//                }
//            } else {
//                rate = 1f; //full forget
//            }
//            b.priSub(priRemoved * rate);
//        }
//    }
}
