//package nars.nar;
//
//import jcog.Util;
//import jcog.exe.Loop;
//import nars.$;
//import nars.NAR;
//import nars.NARS;
//import nars.Narsese;
//import MetaGoal;
//import Traffic;
//import nars.exe.*;
//import nars.task.DerivedTask;
//import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
//import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
//import org.eclipse.collections.impl.map.mutable.primitive.ByteIntHashMap;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//
//import java.util.Arrays;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.function.ToDoubleFunction;
//
//import static jcog.Texts.*;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class WorkerMultiExecTest {
//
//
//    @Disabled
//    @Test
//    void test1() {
//
//        int threads = 3;
//        Focus.DefaultValuator reval = new Focus.DefaultValuator();
//        WorkerMultiExec exe = new WorkerMultiExec(reval, threads, 16,3 /* TODO this shouldnt need to be > 1 */);
//
//
//        NAR n = new NARS().exe(exe).get();
//
//
//        AtomicLong dutyTimeNS = new AtomicLong();
//        DummyCan a = new DummyCan("a", dutyTimeNS, n, (int j)->Util.sleep(1*j)).value(4f);
//        DummyCan b = new DummyCan("b", dutyTimeNS, n, (int j)->Util.sleep(1*j)).value(1f);
//        DummyCan c = new DummyCan("c", dutyTimeNS, n, (int j)->Util.sleep(2*j)).value(1f);
//
//
//
//
//
//
//
//
//        long start = System.nanoTime();
//        Loop l = n.startFPS(5f /* this shouldnt matter to the degree the DummyCan are short-lived executions */);
//        Util.sleep(4000);
//        l.stop();
//        long totalTimeNS = (System.nanoTime() - start) * threads;
//
//        System.out.println("dutyTime=" + timeStr(dutyTimeNS.get()) + " , totalTime=" + timeStr(totalTimeNS) +
//                ", efficiency=" + ((double)dutyTimeNS.get())/totalTimeNS );
//
//        System.out.println(a.executed.get());
//        System.out.println(b.executed.get());
//        System.out.println(c.executed.get());
//        System.out.println(exe.focus);
//
//        assertEquals(3, exe.focus.choice.size());
//
//        assertEquals(expectedDuty(a) / expectedDuty(b), ((float)a.executed.get()) / b.executed.get(), 0.5f);
//        assertEquals(expectedDuty(a) / expectedDuty(c)/2, ((float)a.executed.get()) / c.executed.get(), 0.5f);
//
//    }
//
//    private static float expectedDuty(DummyCan a) {
//
//        return a.value;
//    }
//
//    @Test
//    void testValueDerivationBranches() throws Narsese.NarseseException {
//
//
//        Exec exe = new UniExec(32);
//        NAR n = new NARS()
//                .withNAL(1, 1)
//                .withNAL(6, 6)
//                .exe(exe)
//                .get();
//
//
//        Arrays.fill(n.emotion.want, -1);
//        n.emotion.want(MetaGoal.Desire, +1);
//
//        Exec.Valuator r = new Focus.DefaultValuator();
//        int cycles = 100;
//
//
//
//        ByteIntHashMap byPunc = new ByteIntHashMap();
//        n.onTask(t -> {
//           if (t instanceof DerivedTask) {
//               byPunc.addToValue(t.punc(), 1);
//           }
//        });
//        n.log();
//        n.input("(x==>y).");
//        n.input("f:a.");
//        for (int i = 0; i < cycles; i++) {
//            n.input("f:b. :|:");
//            n.input("x! :|:");
//            n.run(1);
//            r.update(n);
//        }
//
//        System.out.println(byPunc);
//        n.causes.forEach(c -> {
//            double sum = Util.sum((ToDoubleFunction<Traffic>)(t->t.total), c.goal);
//            if (sum > Double.MIN_NORMAL) {
//                System.out.println(Arrays.toString(c.goal) + "\t" + c);
//            }
//        });
//    }
//
//    class DummyCan extends Causable {
//
//        private final AtomicLong dutyTimeNS;
//        private float value;
//        final IntToIntFunction duty;
//        final AtomicInteger executed = new AtomicInteger();
//
//        DummyCan(String id, AtomicLong dutyTimeNS, NAR nar, IntProcedure duty) {
//            this(id, dutyTimeNS, nar, (i)->{ duty.accept(i); return i; });
//        }
//
//        DummyCan(String id, AtomicLong dutyTimeNS, NAR nar, IntToIntFunction duty) {
//            super(nar, $.the(id));
//            this.dutyTimeNS = dutyTimeNS;
//            this.duty = duty;
//        }
//
//
//        DummyCan value(float v) {
//            this.value = v;
//            return this;
//        }
//
//        @Override
//        public boolean singleton() {
//            return false;
//        }
//
//        @Override
//        protected int next(NAR n, int iterations) {
//            executed.addAndGet(iterations);
//
//
//
//            long start = System.nanoTime();
//            try {
//                return duty.valueOf(iterations);
//
//            } finally {
//                dutyTimeNS.addAndGet( (System.nanoTime() - start ) );
//            }
//        }
//
//        @Override
//        public float value() {
//            return value;
//        }
//    }
//
//
//}