package nars;

/**
 * NAR Input methods
 */
@FunctionalInterface
public interface NARIn {

    void input(Task t);

    default void input(Task... t) {
        for (var x : t) input(x);
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
