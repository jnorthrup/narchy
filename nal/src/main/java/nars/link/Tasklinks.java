package nars.link;

import jcog.bag.Bag;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.op.PriForget;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.term.Termed;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;


public class Tasklinks {


//    public static void linkTask(Task t, float pri, Concept cc) {
//        linkTask(t, pri, cc, null);
//    }

    public static void linkTask(Task x, float p, Bag b) {
        linkTask(x, p, b, null);
    }

    static void linkTask(Task x, float p, Bag<?, TaskLink> b, @Nullable MutableFloat overflow) {
        TaskLink xx =
                //new TaskLink.DirectTaskLink(x, p);
                new TaskLink.GeneralTaskLink(x, p);

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


    public static void linkTask(Task t, final float _pri, /*Task*/Concept cc, @Nullable NAR nar) {

        final float priCause = Math.max(_pri, Pri.EPSILON);



        MutableFloat overflow = new MutableFloat();

        linkTask(t, priCause, cc.tasklinks(), overflow);

        float priEffect = priCause - overflow.floatValue();
        assert(priEffect >= 0);

        //adjust the experienced emotion according to the actual effect
        nar.emotion.onActivate(t, priEffect);


        float conceptActivation = priEffect
                //* nar.amp(t.cause())
        ;

        TermLinks.linkTemplates(cc, priEffect, nar.momentum.floatValue(), nar);

        nar.activate(cc, conceptActivation);

        //adjust the cause values according to the input's actual demand
        ((TaskConcept) cc).value(t, priCause, nar);


        //activation is the ratio between the effective priority and the input priority, a value between 0 and 1.0
        //it is a measure of the 'novelty' of a task as reduced by the priority of an equivalent existing tasklink
        float effectiveness = priEffect / priCause;
        if (effectiveness >= Param.TASK_ACTIVATION_THRESHOLD)
            nar.eventTask.emit(t);

    }

    public static void linkTaskTemplates(Concept c, Task t, float priApplied, NAR nar) {

        List<Termed> ts = c.templates();
        int tss = ts.size();
        if (tss > 0) {
            List<Concept> cc = $.newArrayList(tss);
            for (int i = 0, tsSize = ts.size(); i < tsSize; i++) {
                Termed x = ts.get(i);
                if (x.op().conceptualizable) {
                    Concept ccc = nar.conceptualize(x);
                    if (ccc!=null)
                        cc.add(ccc);
                }
            }

            int ccs = cc.size();
            if (ccs > 0) {
                float p = priApplied;
                {
                    List<Concept> l;
                    if (ccs == 1) {
                        l = cc;
                    } else { //if (activation > (1f - 1f / ccs)) {
                        //all of them but in a random order
                        Collections.shuffle(cc, nar.random());
                        l = cc;
                    } /*else {
                        //sample from the set
                        l = randomTemplateConcepts(
                                cc, nar.random(), (int) Math.ceil(activation * ccs));
                        ccs = l.size();
                    }*/

                    MutableFloat overflow = new MutableFloat();
                    float pEach = p / ccs;
                    if (pEach > Pri.EPSILON) {

                        final float headRoom = 1f - pEach;
                        for (int i = 0; i < ccs; i++) {
                            float o = overflow.get();

                            //spread overflow of saturated targets to siblings
                            float change;
                            if (o >= Pri.EPSILON) {
                                change = Math.min(o, headRoom);
                                overflow.subtract(change);
                            } else {
                                change = 0;
                            }

                            linkTask(t, pEach + change, l.get(i).tasklinks(), overflow);
                        }
                    }
                }
            }

        }

        //TODO also use BatchActivator


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
