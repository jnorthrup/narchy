package nars.control.op;

import jcog.data.list.FasterList;
import nars.NAL;
import nars.Task;
import nars.attention.What;
import nars.concept.NodeConcept;
import nars.concept.Operator;
import nars.control.MetaGoal;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.TemporalTask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.IdempotentBool;
import nars.term.buffer.Termerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.COMMAND;
import static nars.Op.GOAL;

/**
 * transforms an input task into any smaller sub-tasks that constitute the perception process
 */
public enum Perceive {
    ;

    static final Logger logger = LoggerFactory.getLogger(Perceive.class);

    public static void perceive(Task x, What w) {
        //w.link(AtomicTaskLink.link(x.term().concept()).priSet(x.punc(), x.priElseZero()));

        var n = w.nar;

        var xx = x.term();

        MetaGoal.Perceive.learn(x, ((float)xx.volume())/Short.MAX_VALUE, n);

        if (x instanceof SeriesBeliefTable.SeriesTask)
            return; //ignore


        n.emotion.perceivedTaskStart.increment();

        var punc = x.punc();
        var cmd = punc == COMMAND;

        if (cmd || punc == GOAL && !x.isEternal()) {
            if (execOperator(x, w)) {

            }
        }
        //SeriesTask already will have been added directly by the table to itself
        var perceived = (!cmd && !(x instanceof SeriesBeliefTable.SeriesTask)) ? Remember.the(x, w) : null;



        if (!(x instanceof TemporalTask.Unevaluated) && Termerator.evalable(xx)) {
            var rt = (FasterList<Task>) new TaskEvaluation(x, w).result;
            if (rt != null) {
                //rt.removeInstance(x); //something was eval, remove the input HACK
                rt.remove(x); //HACK

                if (!rt.isEmpty()) {
                    //move and share input priority fairly:
                    var xPri = x.pri();
                    var xPriAfter = xPri * NAL.TaskEvalPriDecomposeRate;
                    x.pri(xPriAfter);
                    var xp = (xPri - xPriAfter) / rt.size();
                    for (var y : rt) {
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






    private static boolean execOperator(Task x, What w) {
        Term maybeOperator = Functor.func(x.term());
        if (maybeOperator == IdempotentBool.Null)
            return false;

        var n = w.nar;
        var ooo = n.concept(maybeOperator);
        if (!(ooo instanceof NodeConcept.PermanentNodeConcept))
            return false;
        var oo = ooo.term();
        if (!(oo instanceof Operator))
            return false;

        Task next;

        var o = (Operator) oo;
        try {
            var y = o.model.apply(x, n);
            if (y == null)
                return false;
            if (y.equals(x)) {
                //if debug... warn
                return true; //self; but dont execute
            }

            next = y;

        } catch (Throwable xtt) {
            logger.error("{} operator {} exception {}", x, o, xtt);
            //queue.addAt(Operator.error(this, xtt, n.time()));
            return false;
        }
//        if (cmd) {
//            w.emit(x); //queue.add(new TaskEvent(t));
//        }
        //assert(next!=null);
        Perceive.perceive(next, w);
        return true;

        //queue.forEach(q -> Perceive.perceive(q, w));
        //return task(queue, false);
        //throw new TODO();
    }

}
