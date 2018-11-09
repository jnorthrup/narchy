package jcog.test;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import jcog.test.control.BooleanChoiceTest;
import jcog.test.control.MiniTest;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.agent.FrameTrigger;
import nars.agent.NAgent;
import nars.control.DurService;
import nars.term.Term;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static jcog.Texts.n4;
import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NAgentTest {

    static NAR nar() {

        NAR n = NARS.tmp();
        n.termVolumeMax.set(4);
        n.freqResolution.set(0.25f);
        n.confResolution.set(0.02f);
        n.time.dur(1);

        return n;
    }


    @ParameterizedTest
    @ValueSource(strings = {"t", "f"})
    public void testSame(String posOrNegChar) {

//        Param.DEBUG = true;
//        n.log();
        int cycles = 2000;

        boolean posOrNeg = posOrNegChar.charAt(0) == 't';

        NAR n = nar();
        n.goalPriDefault.set(0.5f);
        n.beliefPriDefault.set(0.4f);

//        n.onTask((t)->{
//            if (t instanceof DerivedTask)
//                System.out.println(t.proof());
//        }, GOAL);

        BooleanBooleanPredicate onlyTrue = (prev, next) -> next;
        BooleanBooleanPredicate onlyFalse = (prev, next) -> !next;

        MiniTest a = new BooleanChoiceTest(n, posOrNeg ? onlyTrue : onlyFalse);

        n.run(cycles);

        long bs = cycles/2, be = cycles+1;

        float avgReward = a.avgReward();
        double avgDex = a.dex.getMean();

        System.out.println((posOrNeg ? "positive" : " negative"));
        System.out.println("\tavgReward=" + n4(avgReward));
        System.out.println("\tavgDex=" + n4(avgDex));

        Term xIsReward = $$("(x =|> reward)");
        {
            Task xIsRewardTask = n.is(xIsReward, bs, be);
            if(xIsRewardTask!=null)
                System.out.println(xIsRewardTask.proof());
            else
                System.out.println(xIsReward + " null");
            String s = xIsRewardTask.toStringWithoutBudget();
            assertTrue(s.contains("(x=|>reward)"));
            assertTrue(s.contains(posOrNeg ? "%1.0;" : "%0.0;"));
            assertTrue(xIsRewardTask.conf() > 0.1f);
            assertTrue(xIsRewardTask.range() > 200);
        }

        Term notXnotReward = $$("(--x =|> reward)");
        {

            Task notXnotRewardTask = n.is(notXnotReward, bs, be);
            if (notXnotRewardTask!=null)
                System.out.println(notXnotRewardTask.proof());
            else
                System.out.println(notXnotReward + " null");
            String s = notXnotRewardTask.toStringWithoutBudget();
            assertTrue(s.contains("((--,x)=|>reward)"));
            assertTrue(s.contains(posOrNeg ? "%0.0;" : "%1.0;"));
            assertTrue(notXnotRewardTask.conf() > 0.1f);
            assertTrue(notXnotRewardTask.range() > 250);
        }


        assertTrue(avgReward > 0.6f, ()-> avgReward + " avgReward");
        assertTrue(avgDex > 0f);

    }


    @ValueSource(ints = { 4, 8, 16 })
    @ParameterizedTest public void testOscillate1(int period) {

        int cycles = 2000;

        NAR n = nar();
        n.freqResolution.set(0.1f);
        n.termVolumeMax.set(8);
//        n.goalPriDefault.set(0.9f);
//        n.beliefPriDefault.set(0.1f);
//        n.time.dur(period/2);

        MiniTest a = new BooleanChoiceTest(n, (prev, next)->{
            return next == (n.time() % period < period/2); //sawtooth: true half of duty cycle, false the other half
        });

        //n.log();
        n.run(cycles);

//        long bs = cycles/2, be = cycles+1;

        n.run(cycles);

        float avgReward = a.avgReward();
        double avgDex = a.dex.getMean();

        System.out.println("period: " + period + " avgReward=" + avgReward + " avgDex=" + avgDex);
        assertTrue(avgReward > 0.6f);
        assertTrue(avgDex > 0f);
    }

    /**
     * reward for rapid inversion/oscillation of input action
     */
    @Test public void testInvert() {

        int cycles = 500;

        NAR n = nar();

        MiniTest a = new BooleanChoiceTest(n, (next, prev) -> {
            //System.out.println(prev + " " + next);
            return next != prev;
        });

        n.run(cycles);

        float avgReward = a.avgReward();
        double avgDex = a.dex.getMean();

        System.out.println(" avgReward=" + avgReward + " avgDex=" + avgDex);
        assertTrue(avgReward > 0.6f);
        assertTrue(avgDex > 0f);
    }


    @Test void testAgentTimingDurs() {
        int dur = 10;
        int dursPerFrame = 2;
        int dursPerService = 3;
        LongArrayList aFrames = new LongArrayList();
        LongArrayList sFrames = new LongArrayList();

        NAR nar = NARS.tmp();
        nar.time.dur(dur);
        NAgent a = new NAgent("x", FrameTrigger.durs(dursPerFrame), nar);

        a.onFrame(()->{
            aFrames.add(nar.time());
        });
        DurService.on(nar,()->{
            sFrames.add(nar.time());
        }).durs(dursPerService);
        nar.run(50);
        assertEquals("[10, 30, 50]", aFrames.toString());
        assertEquals("[0, 10, 40]", sFrames.toString());

    }

    @Test void testNAR_CliffWalk() {

    }
}


//        List<Task> tasks = n.tasks().sorted(
//                Comparators.byFloatFunction((FloatFunction<Task>) task -> -task.priElseZero())
//                        .thenComparing(Termed::term).thenComparing(System::identityHashCode)).collect(toList());
//        tasks.forEach(t -> {
//            System.out.println(t);
//        });

//        p.plot();


//    static class RewardPlot {
//
//        public final Table t;
//
//        public RewardPlot(NAgent a) {
//            t = Table.create(a + " reward").addColumns(
//                    DoubleColumn.create("time"),
//                    DoubleColumn.create("reward")
//            );
//
//            DoubleColumn timeColumn = (DoubleColumn) t.column(0).setName("time");
//            DoubleColumn rewardColumn = (DoubleColumn) t.column(1).setName("reward");
//
//            a.onFrame(x -> {
//                timeColumn.append(a.now);
//                rewardColumn.append(a.reward());
//            });
//        }
//        public void plot() {
//
//            Plot.show(
//                    LinePlot.create( t.name(),
//                            t, "time", "reward").);
//        }
//    }