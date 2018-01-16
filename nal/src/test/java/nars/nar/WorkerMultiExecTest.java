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
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkerMultiExecTest {

    @Test
    public void test1() {

        int threads = 4;
        Exec exe = new PoolMultiExec(new Focus.DefaultRevaluator(), threads, 16,3 /* TODO this shouldnt need to be > 1 */);
        NAR n = new NARS().exe(exe).get();


        DummyCan a = new DummyCan(n,"a").value(1f).delay(10);
        DummyCan b = new DummyCan(n,"b").value(2f).delay(10);
        DummyCan c = new DummyCan(n,"c").value(1f).delay(20);

//        n.onCycle(nn -> {
//            Focus.Schedule s = exe.focus.schedule.read();
//            System.out.println(nn.time() + " " +
//                   Arrays.toString(s.active) + "->" + n4(s.weight));
//        });

        Loop l = n.startFPS(30f);
        Util.sleep(1500);
        l.stop();

        System.out.println(a.executed);
        System.out.println(b.executed);
        System.out.println(c.executed);

        assertEquals(2, ((float)b.executed) / a.executed, 0.5f);
        assertEquals(2, ((float)a.executed) / c.executed, 0.5f);

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
        Arrays.fill(n.want, -1);
        n.want[MetaGoal.Desire.ordinal()] = 1;

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

    static class DummyCan extends Causable {

        private float value;
        private int delayMS;

        int executed = 0;

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
            Util.sleep(iterations * delayMS);
            executed += iterations;
            return iterations;
        }

        @Override
        public float value() {
            return value;
        }
    }


}