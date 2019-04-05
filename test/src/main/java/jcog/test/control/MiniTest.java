package jcog.test.control;

import nars.NAR;
import nars.agent.GameTime;
import nars.agent.Game;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import static nars.$.the;

abstract public class MiniTest extends Game {

    public float rewardSum = 0;
    public final SummaryStatistics dex = new SummaryStatistics();

    public MiniTest(NAR n) {
        super(MiniTest.class.getSimpleName(), GameTime.durs(1), n);
        //statPrint = n.emotion.printer(System.out);

        reward(the("reward"), () -> {
//                System.out.println(this + " avgReward=" + avgReward() + " dexMean=" + dex.getMean() + " dexMax=" + dex.getMax());
//                statPrint.run();
//                nar.stats(System.out);

            float yy = myReward();

            rewardSum += yy;
            dex.addValue(dexteritySum());

            return yy;
        });


        n.start(this);
    }


    abstract protected float myReward();

    public float avgReward() {
        return rewardSum / (((float) nar.time()) / nar.dur());
    }
}
