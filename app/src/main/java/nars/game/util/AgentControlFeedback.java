package nars.game.util;

import nars.NAR;
import nars.game.Game;
import nars.util.TaskSummarizer;

public class AgentControlFeedback {

    public AgentControlFeedback(Game a) {

        var n = a.nar();
        /*mean*/
        //System.out.println("reward: " + Texts.n4(reward) + " : ");
        //s.print();
        //TODO ARFF
        //                s.forEach((punc, num, o, volMean, priMean) ->
        //                        reward + "," + o.str + " " + ((char) punc) + " x " + num + ", volMean=" + n2(volMean) + " priMean=" + n2(priMean))
        //                );
        n.onDur(new Runnable() {

            final TaskSummarizer ts = new TaskSummarizer(n);
            static final int snapshotEvery = 8;
            int dur = 0;
            float rewardSum = 0;

            @Override
            public void run() {
                rewardSum += a.happiness();
                if (dur++ % snapshotEvery == 0) {
                    update(ts.snapshot(), rewardSum/snapshotEvery);
                    rewardSum = 0;
                }
            }

            private void update(TaskSummarizer.TaskSummary s, float reward /*mean*/) {
                //System.out.println("reward: " + Texts.n4(reward) + " : ");
                //s.print();
                //TODO ARFF
//                s.forEach((punc, num, o, volMean, priMean) ->
//                        reward + "," + o.str + " " + ((char) punc) + " x " + num + ", volMean=" + n2(volMean) + " priMean=" + n2(priMean))
//                );
            }
        });

    }
}
