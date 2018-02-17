package nars.nal.nal7;

import nars.NAR;
import nars.Param;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import static nars.time.Tense.ETERNAL;


abstract class TemporalStabilityTest {

    boolean unstable;

    //private final boolean stopOnFirstError = true;

    public void test(int cycles, @NotNull NAR n) {

        Param.DEBUG = true;

        //n.log();
        n.onTask(this::validate);
//        n.onCycle(f -> {
//
//            TimeMap m = new TimeMap(n);
//
//            //Set<Between<Long>> times = m.keySetSorted();
//            /*if (times.size() < 3)
//                continue; //wait until the initial temporal model is fully constructed*/
//
//            //m.print();
//            m.forEach(tt -> {
//
//                validate(tt);
//            });
//
//
//        });

        input(n);

        run(cycles, n);

        assert(!unstable);
    }

    long minInput = ETERNAL, maxInput = ETERNAL;

    public void validate(Task t) {
        long ts = t.start();
        long te = Math.max(t.start()+t.term().dtRange(), t.end());
        if (t.isInput()) {
            System.out.println("in: " + t);
            if (!t.isEternal()) {
                if (minInput == ETERNAL || minInput > ts)
                    minInput = ts;
                if (maxInput == ETERNAL || maxInput < te)
                    maxInput = te;
            }
        } else {

            if (ts < minInput || te > maxInput) {
                System.err.println("  OOB: " + "\n" + t.proof() + "\n");
                unstable = true;
            } else if (!validOccurrence(ts) || !validOccurrence(te) || refersToOOBEvents(t)) {
                //if (irregular.add(t)) { //already detected?
                System.err.println("  instability: " + "\n" + t.proof() + "\n");
                unstable = true;
                //                if (stopOnFirstError)
                //                    n.stop();
                //}
            }
        }
    }

    private boolean refersToOOBEvents(Task t) {
        long s = t.start();
        if (s == ETERNAL)
            return false;
        return t.term().eventsWhile((r, xt)->{

            return !validOccurrence(s + r);

            //cant be determined unless analyzing the relative time only
//            if (xt.op()==IMPL && xt.dt()!=DTERNAL) {
//                if (!validOccurrence(s + xt.sub(0).dtRange() + x.getTwo() + xt.dt()))
//                    return true;
//            }
        }, 0);
    }

    private void run(int cycles, NAR n) {

        if (cycles > 0) {

            n.run(cycles);

            //evaluate(n);
        }
    }

//    public void evaluate(@NotNull NAR n) {
//
//        if (!irregular.isEmpty()) {
//
////            TimeMap m = new TimeMap(n);
//
//            irregular.forEach(i -> {
//
//                System.err.println(i.proof());
//            });
//
//            //m.print();
//
//            assertTrue(false);
//        }
//
//    }


    abstract public boolean validOccurrence(long o);

    /**
     * inputs the tasks for a test
     */
    abstract public void input(NAR n);
}
