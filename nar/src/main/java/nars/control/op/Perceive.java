package nars.control.op;

import jcog.WTF;
import jcog.data.list.FasterList;
import nars.$;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.Operator;
import nars.eval.Evaluation;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Bool;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.task.AbstractTask.multiTask;
import static nars.term.atom.Bool.True;

/**
 * transforms an input task into any smaller sub-tasks that constitute the perception process
 */
public enum Perceive {
    ;

    static final Logger logger = LoggerFactory.getLogger(Perceive.class);

    public static Task perceive(Task x, What w) {

        NAR n = w.nar;
        n.emotion.perceivedTaskStart.increment();


        byte punc = x.punc();
        boolean cmd = punc == COMMAND;

        Task executionPerceived = (cmd || (x instanceof Task && (punc == GOAL && !x.isEternal()))) ?
                execute(x, n, cmd) : null;

        Task xPerceived = (!cmd) ? Remember.the(x, n) : null;

        Task perceived;
        if (executionPerceived == null) {
            perceived = xPerceived;
        } else {
            if (xPerceived != null)
                perceived = task(new FasterList<Task>(2).with(executionPerceived, xPerceived), false);
            else
                perceived = executionPerceived;
        }

        if (!Evaluation.evalable(x.term()))
            return perceived;

        FasterList<Task> rt = (FasterList<Task>) new TaskEvaluation(x, w).result;
        if (rt != null) {
            rt.removeInstance(x); //something was eval, remove the input HACK
            //rt.remove(x);

            if (!rt.isEmpty()) {
                //move and share input priority fairly:
                float xp = x.priGetAndSet(0) / rt.size();
                for (Task y : rt)
                    y.pri(xp);

                //rt.add(perceived); //echo

                return task(rt, false);
            }
        }

        return perceived;
    }

    /**
     * deduplicate and bundle to one task
     */
    private static Task task(FastList<Task> yy, boolean dedup) {
        if (yy == null)
            return null;

        int yys = yy.size();
        switch (yys) {
            case 0:
                return null;
            case 1:
                return yy.get(0);
            case 2:
                if (dedup && yy.get(0).equals(yy.get(1)))
                    return yy.get(0);
                else
                    return multiTask(yy);
            default:
                //test for deduplication
                if (dedup) {
                    java.util.Set<Task> yyDedup = new UnifiedSet(yys);
                    yyDedup.addAll(yy);
                    int yyDedupSize = yyDedup.size();
                    if (yyDedupSize == 1)
                        return yy.get(0);
                    else
                        return multiTask(yyDedupSize == yys ?
                                /*the original list */ yy : /* the deduplicated set */ yyDedup);
                } else {
                    return multiTask(yy);
                }
        }

    }

    /**
     * returns true if the task is acceptable
     */
    @Nullable
    private static Task perceiveable(Task x, Term y, What w) {

        Term it = x.term();
        if (!it.equals(y)) {
            byte punc = x.punc();
            if (y.op() == BOOL) {
                return perceiveBooleanAnswer(x, y, w, it, punc);
            } else {
                return rememberTransformed(x, y, punc);
            }
        } else {
            return null;
        }

    }

    @Nullable
    private static Task perceiveBooleanAnswer(Task x, Term y, What w, Term it, byte punc) {
        Task t;
        if (punc == QUESTION || punc == QUEST) {
            //conver to an answering belief/goal now that the absolute truth has been determined
            //TODO decide if this makes sense for QUEST

            byte answerPunc;
            if (punc == QUESTION) answerPunc = BELIEF;
            else if (punc == QUEST) {
                answerPunc = GOAL;
            } else
                throw new UnsupportedOperationException();

//                    if (it.hasXternal())
//                        it = Retemporalize.retemporalizeXTERNALToDTERNAL.apply(it);

            t = Task.clone(x,
                    it,
                    $.t(y == True ? 1 : 0, w.nar.confDefault(answerPunc)),
                    answerPunc,
                    x.start(), x.end());

            if (t == null) {
                //throw new WTF();
                return null;
            }

        } else {
            //throw new WTF();
            return null;
        }
        return t;
    }

    private static Task rememberTransformed(Task input, Term y, byte punc) {
        @Nullable ObjectBooleanPair<Term> yy = Task.tryTaskTerm(y, punc,
                !input.isInput() // || !Param.DEBUG
        );
        if (yy == null)
            return null;

        Term yyz = yy.getOne();
        @Nullable Task u;

        u = Task.clone(input, yyz.negIf(yy.getTwo()));
        if (u != null) {
            return u; //recurse
        } else {
            throw new WTF();
            //return false;
        }
    }

    private static Task execute(Task t, NAR n, boolean cmd) {
        Term maybeOperator = Functor.func(t.term());

        if (maybeOperator != Bool.Null) {
            Concept oo = n.concept(maybeOperator);
            if (oo instanceof Operator) {
                FasterList<Task> queue = new FasterList(cmd ? 2 : 1);

                Operator o = (Operator) oo;
                try {
                    Task yy = o.model.apply(t, n);
                    if (yy != null && !t.equals(yy)) {
                        queue.add(yy);
                    }
                } catch (Throwable xtt) {
                    logger.warn("{} operator {} exception {}", t, o, xtt);
                    //queue.addAt(Operator.error(this, xtt, n.time()));
                    return null;
                }
                if (cmd) {
                    queue.add(new TaskEvent(t));
                }
                return task(queue, false);
            }
        }
        return null;
    }

    static final class TaskEvaluation extends Evaluation implements Predicate<Term> {

        final Term tt;
        private final What what;
        private final Task t;
        private int tried = 0;
        private Collection result = null;

        TaskEvaluation(Task t, What w) {
            super();

            this.t = t;
            this.tt = t.term();
            this.what = w;

            evalTry((Compound) (t.term()), w.nar.evaluator.get(), false);

            if (result!=null) {
                result = new FasterList(result);
                ((FasterList)result).replaceAll(this::termToTask);
                ((FasterList)result).removeNulls();
                if (result.isEmpty())
                    result = null;
            }
        }

        @Nullable protected Task termToTask(Object yTerm) {
            return Perceive.perceiveable(t, (Term)yTerm, what);
        }

        @Override
        public boolean test(Term y) {
            tried++;

            if (y != Bool.Null && !y.equals(tt)) {
                if (result(y)) {
                    if (result.size() >= NAL.TASK_EVAL_FORK_LIMIT)
                        return false; //done, enough forks
                }
            }

            return tried < NAL.TASK_EVAL_FORK_ATTEMPT_LIMIT;
        }

        protected boolean result(Term y) {
            if (!(y instanceof Bool /* allow Bool for answering */) && !Task.validTaskTerm(y.unneg()))
                return false;

            if (result == null)
                result = new UnifiedSet(1);

            if (result.add(y)) return true;
            else return false;
        }

        @Override
        protected Term bool(Term x, Bool b) {
//                    //filter non-true
            return b;
//                    if (b == True && x.equals(x))
//                        return True; //y;
//                    else if (b == False && x.equals(x))
//                        return False; //y.neg();
//                    else
//                        return Bool.Null; //TODO
        }
    }
}
