package jcog.test.control;

import nars.NAR;
import nars.game.Game;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import static nars.$.the;

public abstract class MiniTest extends Game {

    public float rewardSum = (float) 0;
    public final SummaryStatistics dex = new SummaryStatistics();

    public MiniTest(NAR n) {
        super(MiniTest.class.getSimpleName());
        //statPrint = n.emotion.printer(System.out);

        reward(the("reward"), 1f, () -> {
//                System.out.println(this + " avgReward=" + avgReward() + " dexMean=" + dex.getMean() + " dexMax=" + dex.getMax());
//                statPrint.run();
//                nar.stats(System.out);

            float yy = myReward();

            rewardSum += yy;
            dex.addValue(dexterity());

            return yy;
        });

    }


    protected abstract float myReward();

    public float avgReward() {
        return rewardSum / (((float) nar.time()) / nar.dur());
    }
}
