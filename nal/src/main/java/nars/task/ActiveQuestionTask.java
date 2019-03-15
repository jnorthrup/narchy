package nars.task;

import jcog.event.Off;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
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
 * The question actively listens for unifiable task events, until deleted
 * TODO abstract the matcher into:
 * --exact (.equals)
 * --unify
 * --custom Predicate<Term>
 * TODO separate the action into:
 * --bag (with filtering)
 * --custom BiConsumer<Q Task, A Task>
 * --statistical truth aggregator for easy to understand summary
 */
public class ActiveQuestionTask extends NALTask.NALTaskX implements Consumer<Task> {

    private final BiConsumer<? super ActiveQuestionTask /* Q */, Task /* A */> eachAnswer;

    final ArrayBag<Task, PriReference<Task>> answers;
    private Off onTask;

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
    public ITask next(NAR nar) {


        synchronized (this) {
            if (onTask!=null)
                return null; //already processed and active
            else {
                ITask next = super.next(nar);
                this.ttl = nar.deriveBranchTTL.intValue();
                this.onTask = nar.onTask(this, punc() == QUESTION ? BELIEF : /* quest */ GOAL);
                return next;
            }
        }
    }

    @Override
    public final void accept(Task t) {
        if (test.test(t.term()))
            onAnswer(t);
    }


    private static class MySubUnify extends Unify {

        boolean match;

        public MySubUnify(Random r, int ttl) {
            super(null, r, Param.UnificationStackMax, ttl);
        }

        @Override
        public void tryMatch() {
            
            this.match = true;
            stop(); 
        }

    }

    @Override
    public boolean delete() {
        off();
        return super.delete();
    }

    public synchronized void off() {
        if (this.onTask != null) {
            this.onTask.off();
            this.onTask = null;
        }
    }

    @Deprecated private ArrayBag<Task, PriReference<Task>> newBag(int history) {
        return new PLinkArrayBag<>(PriMerge.max, history) {
            @Override
            public void onAdd(PriReference<Task> t) {
                eachAnswer.accept(ActiveQuestionTask.this, t.get());
            }
        };
    }


    @Override
    public @Nullable Task onAnswered(Task answer, NAR n) {
        Task x = super.onAnswered(answer, n);
        onAnswer(answer);
        return x;
    }

    protected Task onAnswer(Task answer) {
        answers.putAsync(new PLink<>(answer, answer.priElseZero()));
        return answer;
    }

}
