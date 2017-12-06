package nars.exe;

import jcog.Util;
import jcog.exe.Loop;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.control.Causable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static jcog.Texts.n4;

public class MultiExecTest {

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

    @Test
    public void test1() {

        int threads = 4;
        MultiExec exe = new MultiExec(0, threads, 3 /* TODO this shouldnt need to be > 1 */);
        NAR n = new NARS().exe(exe).get();


        DummyCan a = new DummyCan(n,"a").value(1f).delay(10);
        DummyCan b = new DummyCan(n,"b").value(2f).delay(10);
        DummyCan c = new DummyCan(n,"c").value(1f).delay(20);

        n.onCycle(nn -> {
           System.out.println(nn.time() + " " +
                   Arrays.toString(exe.focus.schedule.read().active) +
                   "->" + n4(exe.focus.schedule.read().weight));
        });

        Loop l = n.startFPS(50f);
        Util.pause(1000);
        l.stop();

        System.out.println(a.executed);
        System.out.println(b.executed);
        System.out.println(c.executed);

    }
}