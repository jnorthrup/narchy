package nars.task;

import jcog.event.Off;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Op;
import nars.NAL;
import nars.Task;
import nars.attention.What;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.*;

/**
 * Question task which accepts a callback to be invoked on answers
 *
 * The question actively listens for unifiable task events, until deleted
 *
 * TODO register/unregister a NARPart internally for the duration of the task's lifecycle.  a weakref back to the task
 * can tell it when the NARPart should be off.
 *
 * TODO abstract the matcher into:
 * --exact (.equals)
 * --unify
 * --custom Predicate<Term>
 * TODO separate the action into:
 * --bag (with filtering)
 * --custom BiConsumer<Q Task, A Task>
 * --statistical truth aggregator for easy to understand summary
 */
public class ActiveQuestionTask extends NALTaskX implements Consumer<Task> {

    private final BiConsumer<? super ActiveQuestionTask /* Q */, Task /* A */> eachAnswer;

    private final Bag<Task, PriReference<Task>> answers;
    private volatile Off onTask;

    final Predicate<Term> test;

    private transient int ttl;

    /**
     * wrap an existing question task
     */
    public ActiveQuestionTask(Task q, int history, NAR nar, BiConsumer<? super ActiveQuestionTask, Task> eachAnswer) {
        this(q.term(), q.punc(), q.mid() /*, q.end()*/, history, nar, eachAnswer);
    }

    public ActiveQuestionTask(Term term, byte punc, long occ, int history, NAR nar, Consumer<Task> eachAnswer) {
        this(term, punc, occ, history, nar, (q, a) -> eachAnswer.accept(a));
    }

    public ActiveQuestionTask(Term term, byte punc, long occ, int history, NAR nar,  BiConsumer<? super ActiveQuestionTask, Task> eachAnswer) {
        super(term, punc, null, nar.time(), occ, occ, nar.evidence());

        assert(punc==QUESTION || punc == QUEST);

        budget(nar);

        this.answers = newBag(history);
        this.eachAnswer = eachAnswer;

        Op o = term.op();
        if (o.var) {
            this.test = null; //anything
        } else {
            if (term.hasVars())
                this.test = t -> {
                    if (t.op() == o && Subterms.possiblyUnifiable(term, t, Op.Variable)) {
                        MySubUnify u = new MySubUnify(nar.random(), ttl); //TODO pool ThreadLocal
                        u.unify(term(), t);
                        return u.match;
                    }
                    return false;
                };
            else
                this.test = term::equals;
        }
    }

    @Override
    public ITask next(Object ww) {

        What w = (What)ww;

        //synchronized (this) {
            if (onTask!=null)
                return null; //already processed and active
            else {
                ITask next = super.next(w);

                NAR nar = w.nar;
                this.ttl = nar.deriveBranchTTL.intValue();
                this.onTask = nar.onTask(this, punc() == QUESTION ? BELIEF : /* quest */ GOAL);
                return next;
            }
        //}
    }

    @Override
    public final void accept(Task t) {
        if (test.test(t.term()))
            onAnswer(t);
    }


    private static class MySubUnify extends Unify {

        boolean match;

        MySubUnify(Random r, int ttl) {
            super(null, r, NAL.unify.UNIFICATION_STACK_CAPACITY, ttl);
        }

        @Override
        public boolean tryMatch() {
            this.match = true;
            return false; //done
        }

    }

    @Override
    public boolean delete() {
        if (super.delete()) {
            if (this.onTask != null) {
                this.onTask.close();
                this.onTask = null;
            }
            return true;
        }
        return false;
    }

    @Deprecated private Bag<Task, PriReference<Task>> newBag(int history) {
        return new AnswerBag(history);
    }


    @Override
    public @Nullable Task onAnswered(Task answer) {
        Task x = super.onAnswered(answer);
        onAnswer(answer);
        return x;
    }

    private Task onAnswer(Task answer) {
        answers.putAsync(new PLink<>(answer, answer.priElseZero()));
        return answer;
    }

    private final class AnswerBag extends PLinkArrayBag<Task> {
        AnswerBag(int history) {
            super(PriMerge.max, history);
        }

        @Override
        public void onAdd(PriReference<Task> t) {
            eachAnswer.accept(ActiveQuestionTask.this, t.get());
        }
    }
}
