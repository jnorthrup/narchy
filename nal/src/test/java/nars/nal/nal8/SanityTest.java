//package nars.nal.nal8;
//
//import jcog.list.CircularArrayList;
//import nars.*;
//import nars.control.MetaGoal;
//import nars.op.Operator;
//import nars.task.DerivedTask;
//import nars.task.NALTask;
//import nars.term.Term;
//import nars.term.container.TermContainer;
//import nars.term.var.Variable;
//import nars.time.Tense;
//import nars.truth.Truth;
//import org.jetbrains.annotations.Nullable;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.SortedSet;
//import java.util.TreeSet;
//
//import static jcog.Texts.n4;
//import static nars.Op.BELIEF;
//import static nars.Op.QUEST;
//
//public class SanityTest {
//
//    @Test
//    public void testTape1() throws Narsese.NarseseException {
//
//        NAR n = NARS.tmp();
//
//        n.want[MetaGoal.Perceive.ordinal()] = -1f; //reduce spam
//        n.want[MetaGoal.Desire.ordinal()] = +1f; //act
//
//        n.truthResolution.setValue(0.1f);
//        n.termVolumeMax.setValue(24);
//        n.time.dur(1);
//        //n.log();
//
//
//        int cap = 4;
//        TermBuffer t = new TermBuffer(cap);
//        for (int i = 0; i < cap; i++)
//            t.tape.add($.the(i));
//
//        t.on(n);
//
//        n.log();
//
//        System.out.println("SET");
//
//        for (int i = 0; i < 4; i++) {
//            n.input("$0.99 set(a, " + i + ")! :|:");
//            n.run(20);
//        }
//
//        n.run(20);
//
//        System.out.println("GET");
//
//        for (int i = 0; i < 4; i++) {
//            n.input("get(a, #x)! :|:");
//            n.run(5);
//        }
//
//        n.run(10);
//
//        System.out.println("TEST GET");
//
//        n.clear();
//
//        System.out.println("clear");
//        n.input("$0.9 get(a,3)! :|:");
//        n.run(1000);
//
//    }
//
//    @Test
//    public void testSanity0() {
//
//
//        Term happy = $.the("happy");
//        Term up = $.the("up");
//        Term down = $.the("down");
//
//        NAR n = NARS.tmp();
//
//        n.truthResolution.setValue(0.1f);
//        n.termVolumeMax.setValue(10);
//
//        n.log();
//
//        n.goal(happy);
//
//        n.onCycle(() -> {
//            long now = n.time();
//
//            Truth u = n.goalTruth(up, now);
//            float uu = u != null ? Math.max(0.5f, u.expectation()) : 0f;
//            Truth d = n.goalTruth(down, now);
//            float dd = d != null ? Math.max(0.5f, d.expectation()) : 0f;
//
//            n.believe(up, Tense.Present, uu);
//            n.believe(down, Tense.Present, dd);
//            n.believe(happy, Tense.Present, (uu - dd));
//        });
//
//        int STEP = 5;
//
//        n.run(STEP);
//
//        n.goal(up, Tense.Present, 1f);
//        n.run(STEP);
//        n.goal(up, Tense.Present, 0f);
//        n.run(STEP);
//
//        n.goal(down, Tense.Present, 1f);
//        n.run(STEP);
//        n.goal(down, Tense.Present, 0f);
//        n.run(STEP);
//
//        n.run(STEP * 10);
//
//    }
//
//    @Test
//    public void testSanity1() {
//
//
//        NAR n = NARS.tmp();
//
//        final int[] togglesPos = {0};
//        final int[] togglesNeg = {0};
//        boolean[] target = new boolean[1];
//
//        NAgent a = new NAgent(n) {
//
//
//            float score = 0;
//
//            {
//                actionToggle($.the("mustBeOn"), (b) -> {
//                    boolean good = b == target[0];
//                    System.out.println(good ? "=== GOOD ===" : "=== BADD ===");
//                    score += good ? +1 : -1;
//                });
//
//            }
//
//            @Override
//            protected float act() {
//                float r = score;
//                score = 0;
//
//                return r;
//            }
//        };
//
//        n.termVolumeMax.setValue(10);
//        //a.durations.setValue(1);
//
//
//        SortedSet<DerivedTask> derived = new TreeSet<>((y, x) -> {
//            int f1 = Float.compare(x.conf(), y.conf());
//            if (f1 == 0) {
//                int f2 = x.term().compareTo(y.term());
//                if (f2 == 0) {
//                    return Integer.compare(x.hashCode(), y.hashCode());
//                }
//                return f2;
//            }
//            return f1;
//        });
//        n.onTask(t -> {
//            if (t instanceof DerivedTask &&
//                    t.isGoal()) {
//                //t.isBeliefOrGoal()) {
//                //derived.add((DerivedTask)t);
//                System.err.println(t.proof());
//            }
//        });
//
//        //n.log();
//        int timeBatch = 50;
//
//        int batches = 2;
//        for (int i = 0; i < batches; i++) {
//            batchCycle(n, togglesPos, togglesNeg, target, a, derived, timeBatch);
//            a.curiosity.setValue(1 + (1f / i)); //decrease
//        }
//    }
//
//    public void batchCycle(NAR n, int[] togglesPos, int[] togglesNeg, boolean[] target, NAgent a, SortedSet<DerivedTask> derived, int timeBatch) {
//        //System.out.println("ON");
//        target[0] = true;
//        togglesPos[0] = togglesNeg[0] = 0;
//        n.believe($.$safe("target:on"), Tense.Present);
//        for (int i = 0; i < 1; i++) {
//            batch(n, togglesPos[0], togglesNeg[0], a, derived, timeBatch);
//        }
//
//        //System.out.println("OFF");
//        target[0] = false;
//        togglesPos[0] = togglesNeg[0] = 0;
//        n.believe($.$safe("(--,target:on)"), Tense.Present);
//        for (int i = 0; i < 1; i++) {
//            batch(n, togglesPos[0], togglesNeg[0], a, derived, timeBatch);
//        }
//    }
//
//    public void batch(NAR n, int togglesPo, int i, NAgent a, SortedSet<DerivedTask> derived, int timeBatch) {
//        n.run(timeBatch);
//
//
//        float averageReward = a.rewardSum / timeBatch;
//        //System.out.println("time=" + n.time() + "\tavg reward=" + n4(averageReward) );
//        System.out.println(n.time() + ", " + n4(averageReward));
////                "\ttoggles +" + togglesPo + "/-" + i);
//        a.rewardSum = 0;
//
//
//        if (averageReward < 0) {
//            derived.forEach(t -> {
//                if (t.isGoal())
//                    System.out.println(t.proof());
//            });
//        }
//        derived.clear();
//    }
//
//    static class TermBuffer {
//
//        static final Logger logger = LoggerFactory.getLogger(TermBuffer.class);
//
//        CircularArrayList<Term> tape;
//        int pos = 0;
//
//        public TermBuffer(int capacity) {
//            tape = new CircularArrayList<>(capacity);
//        }
//
//        public synchronized void rewind() {
//            pos = 0;
//        }
//
//        public synchronized Term next() {
//            Term x = tape.get(pos);
//            if (++pos >= tape.size())
//                pos = 0;
//            return x;
//        }
//
//        public synchronized void push(Term x) {
//            tape.set(pos, x);
//            if (++pos >= tape.size())
//                pos = 0;
//        }
//
//        public void on(NAR n) {
//
//            float exeThresh = 0.51f;
//            Operator get = n.onOp("get", new Operator.AtomicExec((xt, nn) -> {
//                @Nullable TermContainer aa = Operator.args(xt);
//                if (aa.subs() == 2) {
//                    Term tapeID = aa.sub(0);
//                    if (tapeID.op().conceptualizable) {
//                        long now = nn.time();
//
//                        if (aa.sub(1) instanceof Variable) {
//
//                            Term y = next();
//
//                            Term c = $.impl(
//                                        $.func("get", tapeID, $.varDep(1)),
//                                        (int) (now - xt.start()),
//                                        $.func("get", tapeID, y)
//                                    ).normalize();
//
//                            NALTask u = new NALTask(
//                                    c, BELIEF, $.t(1f, nn.confDefault(BELIEF)),
//                                    now, now, now, nn.time.nextInputStamp());
//
//                            u.pri(nn.priDefault(BELIEF));
//
//                            logger.info("{} {}", this, u);
//
//                            n.input(u);
//
//                        } else {
//                            //maybe you mean this
//                            NALTask u = new NALTask(
//                                    $.func("get", tapeID, $.varDep(1)).normalize(),
//                                    QUEST, null,
//                                    now, now, now, nn.time.nextInputStamp());
//                            u.pri(nn.priDefault(BELIEF));
//                            n.input(u);
//
//                        }
//                    }
//                }
//            }, exeThresh));
//            //n.goal(get.term(), Tense.Eternal, 0f, 0.05f); //neg bias
//
//            Operator set = n.onOp("set", new Operator.AtomicExec((xt, nn) -> {
//                @Nullable TermContainer aa = Operator.args(xt);
//                if (aa.subs() == 2) {
//                    Term tapeID = aa.sub(0);
//                    if (tapeID.op().conceptualizable) {
//                        long now = nn.time();
//
//                        Term v = aa.sub(1);
//                        if (!(v instanceof Variable)) {
//
//                            push(v);
//
//                            Term c = $.func("set", tapeID, v);
//
//                            NALTask u = new NALTask(
//                                    c, BELIEF, $.t(1f, nn.confDefault(BELIEF)),
//                                    now, now, now, nn.time.nextInputStamp());
//
//                            u.pri(nn.priDefault(BELIEF));
//
//                            logger.info("{} {}", this, u);
//
//                            n.input(u);
//
//                        }
//                    }
//                }
//            }, exeThresh));
//            //n.goal(get.term(), Tense.Eternal, 0f, 0.05f); //neg bias
//
//        }
//    }
//}
