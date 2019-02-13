package nars.control.op;

import jcog.WTF;
import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.Operator;
import nars.eval.Evaluation;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Bool;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.True;

/** transforms an input task into any smaller sub-tasks that constitute the perception process */
public enum Perceive { ;

    static final Logger logger = LoggerFactory.getLogger(Perceive.class);




    public static ITask perceive(Task task, NAR n) {
        Term x = task.term();

        FasterList<ITask> yy;
        if (Evaluation.canEval(x)) {

            yy = new Perceive.TaskEvaluation(task, n).result;
            if (yy==null)
                return null;

        } else {
            yy = new FasterList(1);
            if (!Perceive.remember(task, x, yy, n))
                return null;
        }

        return task(yy);

    }

    /** deduplicate and bundle to one task */
    @Nullable static ITask task(FasterList<ITask> yy) {
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
    private static boolean remember(Task input, Term y, Collection<ITask> queue, NAR n) {

        if (y == Bool.Null) {
            //logger.debug("nonsense {}", input);
            return false;
        }

        Task t;
        if (!input.term().equals(y)) {
            byte punc = input.punc();
            if (y.op()==BOOL) {
                if (punc == QUESTION || punc == QUEST) {
                    //conver to an answering belief/goal now that the absolute truth has been determined


                    byte answerPunc;
                    if (punc == QUESTION) answerPunc = BELIEF;
                    else answerPunc = GOAL;

                    t = Task.clone(input,
                            input.term(),
                            $.t(y==True ? 1 : 0, n.confDefault(answerPunc)),
                            answerPunc,
                            input.start(), input.end());

                    if (t == null)
                        throw new WTF();

                } else {
                    return false; //???
                }
            } else {

                return rememberTransformed(input, y, queue, punc);

            }

        } else {
            t = input;
        }

        return perceived(t, queue, n);
    }

    private static boolean rememberTransformed(Task input, Term y, Collection<ITask> queue, byte punc) {
        @Nullable ObjectBooleanPair<Term> yy = Task.tryContent(y, punc,
                !input.isInput() // || !Param.DEBUG
        );
        if (yy == null)
            return false;

        Term yyz = yy.getOne();
        @Nullable Task u;

        u = Task.clone(input, yyz.negIf(yy.getTwo()));
        if (u!=null) {
            return queue.add(u); //recurse
        } else {
            throw new WTF();
            //return false;
        }
    }


    static final class TaskEvaluation extends Evaluation implements Predicate<Term> {

        private final NAR nar;
        private final Task t;
        private int tried = 0;
        private FasterList<ITask> result = null;

        TaskEvaluation(Task t,NAR nar) {
            super();

            this.t = t;
            this.nar = nar;

            evalTry(t.term(), nar.evaluator);
        }

        @Override
        public boolean test(Term y) {
            tried++;

            if (y == Bool.Null)
                return true; //continue TODO maybe limit these

            if (result==null)
                result = new FasterList<>(1);

            if (Perceive.remember(t, y, result, nar)) {
                if (result.size() >= Param.TASK_EVAL_FORK_LIMIT)
                    return false; //done, enough forks
            }

            return tried < Param.TASK_EVAL_TRY_LIMIT;
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

    private static boolean perceived(Task t, Collection<ITask> queue, NAR n) {


        byte punc = t.punc();
        boolean cmd = punc == COMMAND;
        if (!cmd) {
            ITask p = Remember.the(t, n);
            if (p != null)
                queue.add(p);
        }

        if (cmd || (punc == GOAL && !t.isEternal())) {
            if (!execute(t, queue, n, cmd))
                return false;
        }


        return true;
    }

    private static boolean execute(Task t, Collection<ITask> queue, NAR n, boolean cmd) {
        Term maybeOperator = Functor.func(t.term());

        if (maybeOperator!= Bool.Null) {
            Concept oo = n.concept(maybeOperator);
            if (oo instanceof Operator) {
                Operator o = (Operator)oo;
                try {
                    Task yy = o.model.apply(t, n);
                    if (yy != null && !t.equals(yy)) {
                        queue.add(yy);
                    }
                } catch (Throwable xtt) {
                    logger.warn("{} operator {} exception {}", t, o, xtt);
                    //queue.addAt(Operator.error(this, xtt, n.time()));
                    return false;
                }
                if (cmd) {
                    queue.add(new TaskEvent(t));
                }

            }
        }
        return true;
    }
}
