package nars.nar;

import jcog.learn.ql.HaiQae;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.NAgent;
import nars.control.MetaGoal;
import nars.op.RLBooster;
import nars.term.Term;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NAgentTest {

    static NAR nar() {

        NAR n = NARS.tmp();
        n.termVolumeMax.set(17);
        n.freqResolution.set(0.1f);
        n.confResolution.set(0.01f);
        n.time.dur(1);

        //n.emotion.deriveFailTemporal.why.on(new Meter.ReasonCollector());
        //n.emotion.deriveFailEval.why.on(new Meter.ReasonCollector());

        n.emotion.want(MetaGoal.Perceive, -0.1f);
        n.emotion.want(MetaGoal.Desire, +0.1f);

//        n.logWhen(System.out, false, true, true);

        //n.freqResolution.set(0.1f);

//
//        if (Param.DEBUG) {
//            n.onTask(t -> {
//                if (t instanceof DerivedTask && t.isGoal()) {
//                    System.out.println(t.proof());
//                }
//            });
//        }
        return n;
    }

    @ParameterizedTest
    @ValueSource(strings={/*"tt", "tf", */"t", "f"})
    public void testSame(String x) {

        boolean posOrNeg = x.charAt(0) == 't';
//        boolean toggleOrPush = x.charAt(1) == 't';

        System.out.println((posOrNeg ? "positive" : " negative"));// + " and " + (toggleOrPush ? "toggle" : " push"));
        MiniTest a = new ToggleSame(nar(), $.the("t"),
                //$.$safe("t:y"),
                $.$$("(t,y)"),
                posOrNeg);

//        a.curiosity.set(0.5f);
//
//        //a.nar.log();
//        a.nar.onTask(t -> {
//            if (!t.isInput() & t.isGoal()) {
//                System.out.println(t.proof());
//            }
//        });

        a.runSynch(1000);

        assertTrue(a.avgReward() > 0.01f);
        assertTrue(a.dex.getMean() > 0f);
    }

    @Test
    public void testOscillate() {

        NAR n = nar();
        assertOscillatesAction(n, (a)->{});
    }

    @Test
    public void testOscillate_RLBoost_only() {

        NAR n = NARS.shell();

        assertOscillatesAction(n, (a)->{
            new RLBooster(a, HaiQae::new, 2);
        });

    }

    static void assertOscillatesAction(NAR n, Consumer<NAgent> init) {

//        System.out.println((toggleOrPush ? "toggle" : " push"));

        MiniTest a = new ToggleOscillate(n, $.the("t"),
                $.$$("t:y"),
                //$.$safe("(t,y)"),
                true);
                //toggleOrPush);

        init.accept(a);

        a.runSynch(500);

        assertTrue(-(-1-a.avgReward()) > 0.2f); //oscillation density
        assertTrue(a.dex.getMean() > 0.1f);
    }



    abstract static class MiniTest extends NAgent {
        private final Runnable statPrint;
        public float rewardSum = 0;
        final DescriptiveStatistics dex = new DescriptiveStatistics();

        public MiniTest(NAR n) {
            this(null, n);
        }

        public MiniTest(Term id, NAR n) {
            super(id, n);
            statPrint = n.emotion.printer(System.out);
        }



        @Override
        public void runSynch(int frames) {
            super.runSynch(frames);
            System.out.println(this + " avgReward=" + avgReward() + " dexMean=" + dex.getMean() + " dexMax=" + dex.getMax());
            statPrint.run();
            nar.stats(System.out);
        }

        @Override
        protected float act() {
            float yy = reward();

            rewardSum += yy;
            dex.addValue(dexterity());

            return yy;
        }


        abstract float reward();

        public float avgReward() {
            return rewardSum / (((float) nar.time()) / nar.dur());
        }
    }

    static class ToggleSame extends MiniTest {

        private float reward;

        public ToggleSame(NAR n, Term env, Term action, boolean posOrNeg) {
            super(env, n);
            reward = 0;

            BooleanProcedure pushed = (v) -> {
                //System.err.println(n.time() + ": " + v);
                if (posOrNeg) {
                    this.reward = v ? 1 : 0;
                } else {
                    this.reward = v ? -1 : 1;
                }
            };
//            if (toggleOrPush)
//                actionToggle(action, pushed);
//            else
                actionPushButton(action, pushed);
        }

        @Override
        float reward() {
            float r = reward;
            reward = 0; //reset
            return r;
        }

    }

    /** reward for rapid inversion/oscillation of input action */
    static class ToggleOscillate extends MiniTest {

        private int y;
        private int prev = 0;

        public ToggleOscillate(NAR n, Term env, Term action, boolean toggleOrPush) {
            super(env, n);
            y = 0;

            BooleanProcedure pushed = (v) -> {
                //System.err.println(n.time() + ": " + v);
                this.y = v ? 1 : -1;
            };
            if (toggleOrPush)
                actionToggle(action, pushed);
            else
                actionPushButton(action, pushed);
        }

        @Override
        float reward() {
            float r = y == prev ? -1 : 1;
            prev = y;
            y = 0; //reset
            //System.out.println(nar.time() + " " + r);
            return r;
        }

    }

//        n.onTask(t->{
//            if (t.isGoal()) {
//                if (t.term().equals(action))
//                    System.out.println(t.start() + ".." + t.end() + "\t" + t.proof());
//            }
//        });

//    @Test public void testSameCheat() {
//
//
//
//        NAR n = new NARS().get();
//
//        Term action = $.$$("(t,y)");
//
//        Deriver d = new MatrixDeriver(n.exe::fire, (Collection<Task> x)->{
//
//            //HACK TODO this is more efficiently done by filtering the rules rather than the results!
//            Collection<Task> filtered = Collections2.filter(x, Task::isGoal);
//            if (!filtered.isEmpty()) {
//                //System.out.println(filtered);
//                Collection<Task> filterAction = Collections2.filter(filtered, (t) -> t.term().equals(action));
//
//                filterAction.forEach(t->{
//                    System.out.println(t.proof());
//                    System.out.println();
//                });
//            }
//            n.input(filtered);
//
//        }, Derivers.nal(1, 8, n), n);
//
//        new STMLinkage(n, 1, false);
//        d.conceptsPerIteration.set(2);
//
////        n.log();
//
//
//
//        MiniTest a = new ToggleSame(n, $.the("t"),
//                //$.$safe("t:y"),
//                action,
//                true);
//
////        n.onCycle(a::run);
////        n.run(100);
//
//        //n.goal("(t,y)", Tense.Present, 1f);
//        n.synch();
//
//        Term ax = IMPL.the(action, 0, a.happy.filter[0].term);
//        n.believe(ax, Tense.Present);
//
//        a.runSynch(1500);
//
//        assertTrue(a.avgReward() > 0.1f);
//        assertTrue(a.dex.getMean() > 0f);
//    }
}