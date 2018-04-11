package nars.link;

import jcog.bag.Bag;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.op.PriForget;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;


public class Tasklinks {

    @Deprecated public static void linkTask(Task x, float p, Bag b) {
        TaskLink xx =
                new TaskLink.DirectTaskLink(x, p);
                //new TaskLink.GeneralTaskLink(x, p);

        linkTask(xx, b, null);
    }

    static void linkTask(TaskLink xx, Bag<?, TaskLink> b, @Nullable MutableFloat overflow) {

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


    /** create source tasklink */
    public static TaskLink.GeneralTaskLink linkTask(Task t, final float _pri, /*Task*/Concept src, @Nullable NAR nar) {

        /** non-zero for safety */
        final float priCause = Math.max(_pri, Pri.EPSILON);


        //MutableFloat overflow = new MutableFloat();
        TaskLink.GeneralTaskLink link = new TaskLink.GeneralTaskLink(t, nar, priCause);

        linkTask(link, src.tasklinks(), null);

        float priEffect =
                priCause;
                //t.isInput() ? priCause : priCause - overflow.floatValue();

        assert(priEffect >= 0);

        //activate the task's concept
        nar.activate(src,  nar.activationRate.floatValue() * priEffect);

        //activate the task concept termlink templates
        src.templates().activate(src, priEffect, nar);

        {
            //adjust the cause values according to the input's actual demand
            ((TaskConcept) src).value(t, priCause, nar);

            //adjust the experienced emotion according to the actual effect
            nar.emotion.onActivate(t, priEffect);
        }


        //activation is the ratio between the effective priority and the input priority, a value between 0 and 1.0
        //it is a measure of the 'novelty' of a task as reduced by the priority of an equivalent existing tasklink
//        float effectiveness = priEffect / priCause;
//        if (effectiveness >= Param.TASK_ACTIVATION_THRESHOLD)
            nar.eventTask.emit(t);

            return link;
    }

    /** propagate tasklink to templates */
    public static void linkTaskTemplates(Concept c, TaskLink tasklink, NAR nar) {

        Concept[] cc = c.templates().conceptsShuffled(nar, true);
        int ccs = cc.length;
        if (ccs <= 0)
            return;

        float taskLinkMomentum = 0.25f;
        float priTotal = tasklink.priElseZero();
        float priTransferred = (1f - taskLinkMomentum) * priTotal;
        tasklink.priSub(priTransferred);
        float pEach = Math.max(Pri.EPSILON, priTransferred / ccs);
        {

            TaskLink.Tasklike tlSeed =
                    ((TaskLink.GeneralTaskLink)tasklink).get();
                    //TaskLink.GeneralTaskLink.seed(t, false, nar);

            final float headRoom = 1f - pEach;
            MutableFloat overflow = new MutableFloat();
            for (int i = 0; i < ccs; i++) {
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

                linkTask(xx, cc[i].tasklinks(), overflow);
            }
        }

    }

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

    public static class ForgetNonPresentTasklinks extends PriForget<PriReference<Task>> {
        private final long now;
        private final int dur;

        public ForgetNonPresentTasklinks(float r, long now, int dur) {
            super(r);
            this.now = now;
            this.dur = dur;
        }

        @Override
        public void accept(PriReference<Task> b) {
            Task t = b.get();
            float rate;
            if (t.isBeliefOrGoal()) {
                //decrease rate in proximity to now or the future
                if (t.isEternal() || !t.isBefore(now - dur))
                    rate = 0.5f; //slower forget
                else {
                    rate = 1f; //full forget
                }
            } else {
                rate = 1f; //full forget
            }
            b.priSub(priRemoved * rate);
        }
    }
}
