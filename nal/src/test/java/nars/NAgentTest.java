package nars;

import nars.control.DurService;
import nars.control.MetaGoal;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NAgentTest {

    @Test
    public void test1() {
        NAR n = NARS.tmp();
        MetaGoal.Action.set(n.want, 2f);
        MetaGoal.Desire.set(n.want, 1f);
        n.time.dur(2);
        n.freqResolution.set(0.1f);

        Param.DEBUG = true;
        //n.log();
        n.logWhen(System.out, false, true, true);

        int cycles = 500;

        final float[] rewardSum = {0};

        Term action = $.the("y");

        NAgent a = new NAgent(n) {

            float y = 0;

            {
                //sense($.the("x"), ()->x);

                actionToggle(action, (v)->{
//                    System.err.println(n.time() + ": " + v);
                    this.y = v ? 1 : -1;
                });
            }

            @Override
            protected float act() {
                if (n.time() > cycles/2) {
                    if (curiosity.get() > 0) {
                        System.err.println("curiosity off");
                        curiosity.set(0);
                    }
                }

                float yy = y;
                rewardSum[0] += yy;
                return yy;
            }
        };
        n.onTask(t->{
            if (t.isGoal()) {
                if (t.term().equals(action))
                    System.out.println(t.start() + " " + t.proof());
            }
        });
        DurService.on(n, a);
        n.run(cycles);

        float avgReward = rewardSum[0]/(cycles / ((float)n.time.dur()));
        System.out.println(avgReward);

        assertTrue(avgReward > 0f);
    }
}