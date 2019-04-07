package nars.control.op;

import jcog.WTF;
import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.Operator;
import nars.eval.Evaluation;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.transform.Retemporalize;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.True;

/** transforms an input task into any smaller sub-tasks that constitute the perception process */
public enum Perceive { ;

    static final Logger logger = LoggerFactory.getLogger(Perceive.class);

    public static ITask perceive(Task task, What w) {

        NAR n = w.nar;
        n.feel.perceivedTaskStart.increment();

        Term x = task.term();
        if (Evaluation.canEval(x)) {
            return task(new TaskEvaluation(task, w).result);
        } else {
            return Perceive.perceive(task, x, w);
        }
    }

    /** deduplicate and bundle to one task */
    private @Nullable static ITask task(FastList<ITask> yy) {
        if (yy == null)
            return null;

        int yys = yy.size();
        switch (yys) {
            case 0:
                return null;
            case 1:
                return yy.get(0);
            case 2:
                if (yy.get(0).equals(yy.get(1)))
                    return yy.get(0);
                break;
        }

        //test for deduplication
        java.util.Set<ITask> yyy = new HashSet(yys);
        yyy.addAll(yy);
        int yyys = yyy.size();
        if (yyys==1)
            return yy.get(0);
        else
            return AbstractTask.of(yyys ==yys ? /*the original list */ yy : /* the deduplicated set */ yyy);
    }

    /** returns true if the task is acceptable */
    private static ITask perceive(Task x, Term y, What w) {

        assert(!(y instanceof Bool));

        Task t;
        Term it = x.term();
        if (!it.equals(y)) {
            byte punc = x.punc();
            if (y.op()==BOOL) {
                if (punc == QUESTION/* || punc == QUEST*/) {
                    //conver to an answering belief/goal now that the absolute truth has been determined
                    //TODO decide if this makes sense for QUEST

                    byte answerPunc;
                    if (punc == QUESTION) answerPunc = BELIEF;
                    else { answerPunc = GOAL; assert(punc==QUEST); } //HACK

                    if (it.hasXternal())
                        it = Retemporalize.retemporalizeXTERNALToDTERNAL.apply(it);

                    t = Task.clone(x,
                            it,
                            $.t(y==True ? 1 : 0, w.nar.confDefault(answerPunc)),
                            answerPunc,
                            x.start(), x.end());

                    if (t == null)
                        throw new WTF();

                } else {
                    return null; //???
                }
            } else {

                return rememberTransformed(x, y, punc);

            }

        } else {
            t = x;
        }

        return perceived(t, w.nar);
    }

    private static ITask rememberTransformed(Task input, Term y, byte punc) {
        @Nullable ObjectBooleanPair<Term> yy = Task.tryContent(y, punc,
                !input.isInput() // || !Param.DEBUG
        );
        if (yy == null)
            return null;

        Term yyz = yy.getOne();
        @Nullable Task u;

        u = Task.clone(input, yyz.negIf(yy.getTwo()));
        if (u!=null) {
            return u; //recurse
        } else {
            throw new WTF();
            //return false;
        }
    }


    static final class TaskEvaluation extends Evaluation implements Predicate<Term> {

        private final What what;
        private final Task t;
        private int tried = 0;
        private FasterList<ITask> result = null;

        TaskEvaluation(Task t,What w) {
            super();

            this.t = t;
            this.what = w;

            evalTry(t.term(), w.nar.evaluator);
        }

        @Override
        public boolean test(Term y) {
            tried++;

            if (y == Bool.Null)
                return true; //continue TODO maybe limit these


            ITask next = Perceive.perceive(t, y, what);
            if (next != null) {
                if (result==null)
                    result = new FasterList<>(1);

                result.add(next);

                if (result.size() >= Param.TASK_EVAL_FORK_LIMIT)
                    return false; //done, enough forks
            }

            return tried < Param.TASK_EVAL_FORK_ATTEMPT_LIMIT;
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

    private static ITask perceived(ITask t, NAR n) {

        byte punc = t.punc();
        boolean cmd = punc == COMMAND;

        ITask e = null, r = null;
        if (cmd || (t instanceof Task && (punc == GOAL && !((Task)t).isEternal()))) {
            e = execute((Task)t, n, cmd);
        }

        if (!cmd) {
            r = Remember.the((Task)t, n);
        }

        if (e != null && r != null)
            return task(new FasterList<ITask>(2).with(e, r));
        else if (e!=null)
            return e;
        else return r;
    }

    private static ITask execute(Task t, NAR n, boolean cmd) {
        Term maybeOperator = Functor.func(t.term());

        if (maybeOperator!= Bool.Null) {
            Concept oo = n.concept(maybeOperator);
            if (oo instanceof Operator) {
                FasterList<ITask> queue = new FasterList(cmd ? 2 : 1);

                Operator o = (Operator)oo;
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
                return task(queue);
            }
        }
        return null;
    }
}
