package nars.agent;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.control.DurService;
import nars.task.DerivedTask;
import nars.term.Term;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static jcog.Texts.n4;
import static nars.$.$$;
import static nars.$.the;
import static nars.Op.GOAL;
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

        n.onTask((t)->{
            if (t instanceof DerivedTask)
                System.out.println(t.proof());
        }, GOAL);

        BooleanBooleanPredicate onlyTrue = (next, prev) -> next;
        BooleanBooleanPredicate onlyFalse = (next, prev) -> !next;

        MiniTest a = new BooleanAgent(n, posOrNeg ? onlyTrue : onlyFalse);

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

        MiniTest a = new BooleanAgent(n, (next, prev)->{
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

        MiniTest a = new BooleanAgent(n, (next, prev) -> {
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


    @Deprecated abstract static class MiniTest extends NAgent {

        public float rewardSum = 0;
        final SummaryStatistics dex = new SummaryStatistics();

        public MiniTest(NAR n) {
            super((Term)null, FrameTrigger.durs(1), n);
            //statPrint = n.emotion.printer(System.out);

            reward(the("reward"), () -> {
//                System.out.println(this + " avgReward=" + avgReward() + " dexMean=" + dex.getMean() + " dexMax=" + dex.getMax());
//                statPrint.run();
//                nar.stats(System.out);

                float yy = reward();

                rewardSum += yy;
                dex.addValue(dexterity());

                return yy;
            });


            n.on(this);
        }

        public abstract float reward();

        public float avgReward() {
            return rewardSum / (((float) nar.time()) / nar.dur());
        }
    }

    static class BooleanAgent extends MiniTest {

        private float reward;
        boolean prev = false;

        public BooleanAgent(NAR n, BooleanBooleanPredicate goal) {
            this(n, the("x"), goal);
        }

        public BooleanAgent(NAR n, Term action, BooleanBooleanPredicate goal) {
            super(n);

            actionPushButton(action, (next) -> {
               boolean c = goal.accept(next, prev);
               prev = next;
               reward = c ? 1f : 0f;
               return next;
            });

        }

        @Override
        public float reward() {
            float r = reward;
            reward = Float.NaN;
            return r;
        }

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