package nars.nal.nal7;

import nars.NAR;
import nars.Task;

import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertTrue;


abstract class TemporalStabilityTest {

    private boolean unstable;


    public void test(int cycles, NAR n) {


        n.termVolumeMax.set(16);
        n.freqResolution.set(0.1f);
        n.confResolution.set(0.02f);


        n.onTask(this::validate);


        input(n);

        run(cycles, n);

        assertTrue(!unstable);
    }

    private long minInput = ETERNAL;
    private long maxInput = ETERNAL;

    private void validate(Task t) {

        long ts = t.start();
        long te = Math.max(ts + t.term().eventRange(), t.end());

        if (t.isInput()) {
            System.out.println("in: " + t);
            if (!t.isEternal()) {
                if (minInput == ETERNAL || minInput > ts)
                    minInput = ts;
                if (maxInput == ETERNAL || maxInput < te)
                    maxInput = te;
            }
        } else {
            if (t.isQuestionOrQuest())
                return; //ignore. it is natural for it to be curious!!!!

            if (ts < minInput || te > maxInput) {
                System.err.println("  OOB: " + "\n" + t.proof() + "\n");
                unstable = true;
            } else if (!validOccurrence(ts) || !validOccurrence(te) || refersToOOBEvents(t)) {

                System.err.println("  instability: " + "\n" + t.proof() + "\n");
                unstable = true;


            }
        }
    }

    private boolean refersToOOBEvents(Task t) {
        long s = t.start();
        if (s == ETERNAL)
            return false;
        return t.term().eventsWhile((r, xt) -> {

            return !validOccurrence(s + r);


        }, 0);
    }

    private void run(int cycles, NAR n) {

        if (cycles > 0) {

            n.run(cycles);


        }
    }


    protected abstract boolean validOccurrence(long o);

    /**
     * inputs the tasks for a test
     */
    protected abstract void input(NAR n);
}
