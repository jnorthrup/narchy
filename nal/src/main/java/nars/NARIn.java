package nars;

import nars.task.ActiveQuestionTask;
import nars.task.ITask;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * NAR Input methods
 */
public interface NARIn {

    void input(ITask t);

    default void input(ITask... t) {
        for (ITask x : t) input(x);
    }

    @Nullable
    default Task question(String questionTerm, long occ, BiConsumer<ActiveQuestionTask,Task> eachAnswer) throws Narsese.NarseseException {
        return question($.$(questionTerm), occ, eachAnswer);
    }

    @Nullable
    default ActiveQuestionTask question(Term term, long occ, BiConsumer<ActiveQuestionTask,Task> eachAnswer) {
        return question(term, occ, Op.QUESTION, eachAnswer);
    }

    @Nullable
    default ActiveQuestionTask question(Term term, long occ, byte punc /* question or quest */, BiConsumer<ActiveQuestionTask, Task> eachAnswer) {
        assert(punc == Op.QUESTION || punc == Op.QUEST);
        return inputTask( new ActiveQuestionTask(term, punc, occ, 16, (NAR)this, eachAnswer) );
    }

    @Nullable
    default ActiveQuestionTask ask(Term term, long occ, byte punc /* question or quest */, Consumer<Task> eachAnswer) {
        assert(punc == Op.QUESTION || punc == Op.QUEST);
        return inputTask( new ActiveQuestionTask(term, punc, occ, 16, (NAR)this, eachAnswer) );
    }




    /** parses one and only task */
    default <T extends Task> T inputTask(T t) {
        input(t);
        return t;
    }


//    default DurService believeWhile(Term target, Truth t, Predicate<Task> cond) {
//        return taskWhile(target, BELIEF, t, cond);
//    }
//
//    default DurService wantWhile(Term target, Truth t, Predicate<Task> cond) {
//        return taskWhile(target, GOAL, t, cond);
//    }
//
//    @Deprecated default DurService taskWhile(Term target, byte punc, Truth tru, Predicate<Task> cond) {
//        NAR n = (NAR)this;
//
//        long start = n.time();
//        float activeFreq = tru.freq();
//
//
//        float inactiveFreq = 0f;
//        float evi = tru.evi();
//        LongFunction<Truthlet> stepUntil = (toWhen) -> Truthlet.step(inactiveFreq, start, activeFreq, toWhen, activeFreq, evi);
//
//        TruthletTask t = new TruthletTask(target, punc, stepUntil.apply(start), n);
//        float pri = n.priDefault(punc);
//        t.priMax(pri);
//
//        n.input(t);
//
//        return DurService.onWhile(n, (nn)->{
//
//
//
//
//
//
//            long now = nn.time();
//            boolean kontinue;
//            Truthlet tt;
//            if (!cond.test(t)) {
//
//
//                tt = Truthlet.impulse(start, now, activeFreq, inactiveFreq, evi);
//                kontinue = false;
//            } else {
//
//                tt = stepUntil.apply(now);
//                kontinue = true;
//            }
//            t.priMax(pri);
//            t.truth(tt, true, nn);
//            return kontinue;
//        });
//    }
}
