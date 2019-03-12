package nars.exe.impl;

import nars.NAR;
import nars.NARS;
import nars.attention.Attention;
import nars.exe.Valuator;

import java.util.function.Supplier;

public class SuperExec extends ThreadedExec {

    public SuperExec(Valuator valuator, int concurrency) {
        super(valuator, concurrency);
    }

    @Override
    protected Supplier<Worker> loop() {
        return ()->new SubNARLoop(nar);
    }

    /** routes work to either current thread, or the global pool */
    private static class SubExec extends UniExec {
        //TODO
    }
    private static class SubAttention extends Attention {

    }

    private class SubNARLoop implements Worker {
        public final NAR sub;

        int activeCapacity = 512;

        private SubNARLoop(NAR sooper) {
            this.sub = new NARS()
                    .index(sooper.concepts)
                    //.attention(new SubAttention())
                    .time(sooper.time)
                    .concepts(sooper.conceptBuilder)
                    .exe(new SubExec())
                    .get();

            sub.attn.activeCapacity.set(activeCapacity);
            sub.log();
        }

        @Override
        public void run() {
            while (true) {
                in.clear(SuperExec.this::executeNow, 1);
                sub.run();
            }
        }

        @Override
        public void off() {
            sub.stop();
        }
    }


}
