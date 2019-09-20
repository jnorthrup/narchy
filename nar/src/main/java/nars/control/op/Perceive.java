package nars.control.op;

import jcog.TODO;
import jcog.data.list.FasterList;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.Operator;
import nars.eval.Evaluation;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.TemporalTask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Bool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.COMMAND;

/**
 * transforms an input task into any smaller sub-tasks that constitute the perception process
 */
public enum Perceive {
    ;

    static final Logger logger = LoggerFactory.getLogger(Perceive.class);

    public static void perceive(Task x, What w) {
        //w.link(AtomicTaskLink.link(x.term().concept()).priSet(x.punc(), x.priElseZero()));

        if (x instanceof SeriesBeliefTable.SeriesTask)
            return; //ignore

        NAR n = w.nar;
        n.emotion.perceivedTaskStart.increment();

        byte punc = x.punc();
        boolean cmd = punc == COMMAND;

        Remember xPerceived = !cmd ? Remember.the(x, n) : null;

        if (xPerceived!=null) {
//        if (x instanceof UnevaluatedTask) {
            xPerceived.next(w);
        }
//            return;
//        }

//        Task executionPerceived = cmd || punc == GOAL && !x.isEternal() ?
//                execOperator(x, w, cmd) : null;
//
//        Remember perceived;
//        if (executionPerceived != null) {
//            if (xPerceived != null)
//                perceived = task(new FasterList<Task>(2).with(executionPerceived, xPerceived), false);
//            else
//                perceived = executionPerceived;
//        } else {
//            perceived = xPerceived;
//        }

        if (!(x instanceof TemporalTask.Unevaluated) && Evaluation.evalable(x.term())) {
            FasterList<Task> rt = (FasterList<Task>) new TaskEvaluation(x, w).result;
            if (rt != null) {
                //rt.removeInstance(x); //something was eval, remove the input HACK
                rt.remove(x); //HACK

                if (!rt.isEmpty()) {
                    //move and share input priority fairly:
                    float xPri = x.pri();
                    float xPriAfter = xPri * NAL.TaskEvalPriDecomposeRate;
                    x.pri(xPriAfter);
                    float xp = (xPri - xPriAfter) / rt.size();
                    for (Task y : rt) {
                        y.pri(xp);
                        perceive(y, w); //HACK
                    }

                    //echo TODO obviousness filter
                    //TODO cache the evaluation context in a proxy to the echo'd evaluable percept
//                    rt.add(perceived);
//
//                    return task(rt, false);
                }
            }
        }

    }

//    /**
//     * deduplicate and bundle to one task
//     */
//    private static Task task(FastList<Task> yy, boolean dedup) {
//        if (yy == null)
//            return null;
//
//        int yys = yy.size();
//        switch (yys) {
//            case 0:
//                return null;
//            case 1:
//                return yy.get(0);
//            case 2:
//                if (dedup && yy.get(0).equals(yy.get(1)))
//                    return yy.get(0);
//                else
//                    return multiTask(yy);
//            default:
//                //test for deduplication
//                if (dedup) {
//                    java.util.Set<Task> yyDedup = new UnifiedSet(yys);
//                    yyDedup.addAll(yy);
//                    int yyDedupSize = yyDedup.size();
//                    if (yyDedupSize == 1)
//                        return yy.get(0);
//                    else
//                        return multiTask(yyDedupSize == yys ?
//                                /*the original list */ yy : /* the deduplicated set */ yyDedup);
//                } else {
//                    return multiTask(yy);
//                }
//        }

//    private static Task task(FastList<Task> yy, boolean dedup) {
//        if (yy == null)
//            return null;
//
//        int yys = yy.size();
//        switch (yys) {
//            case 0:
//                return null;
//            case 1:
//                return yy.get(0);
//            case 2:
//                if (dedup && yy.get(0).equals(yy.get(1)))
//                    return yy.get(0);
//                else
//                    return multiTask(yy);
//            default:
//                //test for deduplication
//                if (dedup) {
//                    java.util.Set<Task> yyDedup = new UnifiedSet(yys);
//                    yyDedup.addAll(yy);
//                    int yyDedupSize = yyDedup.size();
//                    if (yyDedupSize == 1)
//                        return yy.get(0);
//                    else
//                        return multiTask(yyDedupSize == yys ?
//                                /*the original list */ yy : /* the deduplicated set */ yyDedup);
//                } else {
//                    return multiTask(yy);
//                }
//        }
//
//    }






    private static Task execOperator(final Task t, What w, boolean cmd) {
        Term maybeOperator = Functor.func(t.term());
        if (maybeOperator == Bool.Null)
            return null;

        NAR n = w.nar;
        Concept oo = n.concept(maybeOperator);
        if (!(oo instanceof Operator))
            return null;

        FasterList<Task> queue = new FasterList(cmd ? 2 : 1);

        Operator o = (Operator) oo;
        try {
            Task yy = o.model.apply(t, n);
            if (yy != null && !t.equals(yy))
                queue.add(yy);

        } catch (Throwable xtt) {
            logger.error("{} operator {} exception {}", t, o, xtt);
            //queue.addAt(Operator.error(this, xtt, n.time()));
            return null;
        }
        if (cmd) {
            w.emit(t); //queue.add(new TaskEvent(t));
        }

        //return task(queue, false);
        throw new TODO();
    }

}
