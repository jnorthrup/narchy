package nars.nar;

import jcog.Util;
import jcog.exe.Loop;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.control.MetaGoal;
import nars.control.Traffic;
import nars.exe.*;
import nars.task.DerivedTask;
import org.eclipse.collections.impl.map.mutable.primitive.ByteIntHashMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkerMultiExecTest {

    int threads = 4;
    PoolMultiExec exe = new PoolMultiExec(new Focus.DefaultRevaluator(), threads, 16,3 /* TODO this shouldnt need to be > 1 */);

    @Test
    public void test1() {



        NAR n = new NARS().exe(exe).get();

        Loop l = n.startFPS(5f);

        DummyCan a = new DummyCan(n,"a").value(1f).delay(1);
        DummyCan b = new DummyCan(n,"b").value(2f).delay(1);
        DummyCan c = new DummyCan(n,"c").value(1f).delay(2);

//        n.onCycle(nn -> {
//            Focus.Schedule s = exe.focus.schedule.read();
//            System.out.println(nn.time() + " " +
//                   Arrays.toString(s.active) + "->" + n4(s.weight));
//        });

        Util.sleep(1000);
        l.stop();

        System.out.println(a.executed.get());
        System.out.println(b.executed.get());
        System.out.println(c.executed.get());
        System.out.println(exe.focus);

        assertEquals(2, ((float)b.executed.get()) / a.executed.get(), 0.5f);
        assertEquals(2, ((float)a.executed.get()) / c.executed.get(), 0.5f);

    }

    @Test public void testValueDerivationBranches() throws Narsese.NarseseException {
        int threads = 1;
//        Exec exe = new MultiExec(32, threads, 2);
        Exec exe = new UniExec(32);
        NAR n = new NARS()
                .deriverAdd(1, 1)
                .deriverAdd(6, 6)
                .exe(exe)
                .get();

        //all -1 except goal production
        Arrays.fill(n.emotion.want, -1);
        n.emotion.want(MetaGoal.Desire, +1);

        Exec.Revaluator r = new Focus.DefaultRevaluator();
        int cycles = 100;

        //2 competing independent processes. NAL1 rules will apply to one, and NAL6 rules apply to ther other.
        //measure the amount of derivation work occurring for each
        ByteIntHashMap byPunc = new ByteIntHashMap();
        n.onTask(t -> {
           if (t instanceof DerivedTask) {
               byPunc.addToValue(t.punc(), 1);
           }
        });
        n.log();
        n.input("(x==>y).");
        n.input("f:a.");
        for (int i = 0; i < cycles; i++) {
            n.input("f:b. :|:");
            n.input("x! :|:");
            n.run(1);
            r.update(n);
        }

        System.out.println(byPunc);
        n.causes.forEach(c -> {
            double sum = Util.sum((ToDoubleFunction<Traffic>)(t->t.total), c.goal);
            if (sum > Double.MIN_NORMAL) {
                System.out.println(Arrays.toString(c.goal) + "\t" + c);
            }
        });
    }

    class DummyCan extends Causable {

        private float value;
        private int delayMS;

        final AtomicInteger executed = new AtomicInteger();

        protected DummyCan(NAR nar, String id) {
            super(nar, $.the(id));
        }

        public DummyCan delay(int ms) {
            this.delayMS = ms;
            return this;
        }

        public DummyCan value(float v) {
            this.value = v;
            return this;
        }

        @Override
        public boolean singleton() {
            return false;
        }

        @Override
        protected int next(NAR n, int iterations) {
            executed.addAndGet(iterations);
//            System.out.println(this + " x " + iterations
//                    //+ " " + exe.focus
//                    );
            Util.sleep(iterations * delayMS);
            return iterations;
        }

        @Override
        public float value() {
            return value;
        }
    }


}