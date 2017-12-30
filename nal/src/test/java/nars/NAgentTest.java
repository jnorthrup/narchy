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
        //MetaGoal.Action.want(n.want, 1f);
        n.time.dur(2);
        n.freqResolution.set(0.1f);

        int cycles = 500;

        final float[] rewardSum = {0};

        Term action = $.the("y");

        NAgent a = new NAgent(n) {

            float y = 0;

            {
                //sense($.the("x"), ()->x);

                actionToggle(action, (v)->{
//                    System.err.println(n.time() + ": " + v);
                    this.y = v ? 1 : 0;
                });
            }

            @Override
            protected float act() {
                float yy = y;
                rewardSum[0] += yy;
                return yy;
            }
        };
        //n.log();
//        Param.DEBUG = true;
//        n.onTask(t->{
//            if (t.isGoal()) {
//                if (t.term().equals(action))
//                    System.out.println(t.start() + " " + t.proof());
//            }
//        });
        DurService.on(n, a);
        n.run(cycles);

        float avgReward = rewardSum[0]/(cycles / ((float)n.time.dur()));
        System.out.println(avgReward);

        assertTrue(avgReward > 0.5f);
    }
}