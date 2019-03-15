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

    private final class SubNARLoop implements Worker {
        public final NAR sub;

        int activeCapacity = 512;
        private boolean running;

        private SubNARLoop(NAR sooper) {
            this.sub = new NARS()
                    .index(sooper.concepts)
                    //.attention(new SubAttention())
                    .time(sooper.time)
                    .concepts(sooper.conceptBuilder)
                    .exe(new SubExec())
                    .get();

            sub.attn.linksCapacity.set(activeCapacity);
            sub.log();
            running = true;
        }

        @Override
        public void run() {
            do {
                in.clear(SuperExec.this::executeNow, 1);
                sub.run();
            } while (running);
        }

        @Override
        public void off() {
            running = false; sub.stop();
        }
    }


}
