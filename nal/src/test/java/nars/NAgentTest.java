package nars;

import nars.control.DurService;
import nars.control.MetaGoal;
import nars.task.DerivedTask;
import nars.term.Term;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NAgentTest {

    static NAR nar() {
        Param.DEBUG = true;

        NAR n = NARS.tmp();
        n.termVolumeMax.set(12);
        n.freqResolution.set(0.05f);
        n.confResolution.set(0.02f);
        n.time.dur(1);
        //n.logWhen(System.out, false, true, true);

        //MetaGoal.Action.set(n.want, 2f);
        //MetaGoal.Desire.set(n.want, 1f);
        //n.freqResolution.set(0.1f);
        return n;
    }

    @Test
    public void testToggleSamePos() {

        MiniTest a = new ToggleSame(nar(), $.the("t"), $.$safe("t:y"), true);
//        a.nar.onTask(t -> {
//            if (t.isGoal() && t instanceof DerivedTask) {
//                System.out.println(t.proof());
//            }
//        });
        a.runSynch(600);

        assertTrue(a.avgReward() > 0.25f);
        assertTrue(a.dex.getMean() > 0.02f);
    }

    @Test
    public void testToggleSameNeg() {

        MiniTest a = new ToggleSame(nar(), $.the("t"), $.$safe("t:y"), false);
        a.runSynch(600);

        assertTrue(a.avgReward() > 0.25f);
        assertTrue(a.dex.getMean() > 0.02f);
    }


    abstract static class MiniTest extends NAgent {
        public float rewardSum = 0;
        final DescriptiveStatistics dex = new DescriptiveStatistics();

        public MiniTest(NAR n) {
            this(null, n);
        }

        public MiniTest(Term id, NAR n) {
            super(id, n);
        }

        @Override
        public void runSynch(int frames) {
            super.runSynch(frames);
            System.out.println(this + " avgReward=" + avgReward() + "\n" + dex.toString());
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
            return rewardSum / (((float)nar.time())/nar.dur());
        }
    }

    static class ToggleSame extends MiniTest {

        private final boolean posOrNeg;
        private float y;

        public ToggleSame(NAR n, Term env, Term action, boolean posOrNeg) {
            super(env, n);
            y = 0;
            this.posOrNeg = posOrNeg;

            BooleanProcedure pushed = (v) -> {
                //System.err.println(n.time() + ": " + v);
                this.y = v ? 1 : -1;
            };
            actionToggle(action, pushed);
            //actionPushButton(action, pushed);
        }

        @Override
        float reward() {
            float r = posOrNeg ? y : -y;
            y = 0; //reset
            return r;
        }

    }

//        n.onTask(t->{
//            if (t.isGoal()) {
//                if (t.term().equals(action))
//                    System.out.println(t.start() + ".." + t.end() + "\t" + t.proof());
//            }
//        });

}