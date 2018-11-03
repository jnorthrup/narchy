package nars.agent;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import nars.*;
import nars.control.DurService;
import nars.term.Term;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Consumer;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NAgentTest {

    static NAR nar() {

        NAR n = NARS.tmp();
        n.termVolumeMax.set(4);
        n.freqResolution.set(0.25f);
        n.confResolution.set(0.01f);
        n.time.dur(1);

        return n;
    }

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

    @ParameterizedTest
    @ValueSource(strings = {/*"tt", "tf", */"t", "f"})
    public void testSame(String x) {

        boolean posOrNeg = x.charAt(0) == 't';


        System.out.println((posOrNeg ? "positive" : " negative"));
        NAR n = nar();
        MiniTest a = new ToggleSame(n, $.the("t"), $.the("x"), posOrNeg);

//        RewardPlot p = new RewardPlot(a);


//        Param.DEBUG = true;
//        n.log();
        n.run(100);

//        List<Task> tasks = n.tasks().sorted(
//                Comparators.byFloatFunction((FloatFunction<Task>) task -> -task.priElseZero())
//                        .thenComparing(Termed::term).thenComparing(System::identityHashCode)).collect(toList());
//        tasks.forEach(t -> {
//            System.out.println(t);
//        });

//        p.plot();

        long bs = n.time()/2;
        long be = n.time()+1;

        Term xIsReward = $$("(x =|> reward)");
        {
            Task xIsRewardTask = n.is(xIsReward, bs, be);
            if(xIsRewardTask!=null)
                System.out.println(xIsRewardTask.proof());
            else
                System.out.println(xIsReward + " null");
        }

        Term notXnotReward = $$("(--x =|> reward)");
        {

            Task notXnotRewardTask = n.is(notXnotReward, bs, be);
            if (notXnotRewardTask!=null)
                System.out.println(notXnotRewardTask.proof());
            else
                System.out.println(notXnotReward + " null");
        }

        assertTrue(a.avgReward() > 0.5f, ()->a.avgReward() + " avgReward");
        assertTrue(a.dex.getMean() > 0f);

    }


    @ValueSource(ints = {10, 20, 5, 2})
    @ParameterizedTest public void testOscillate1(int period) {

        MiniTest a = new ToggleOscillate(nar(), $.the("t"), $.the("y"), period);

        a.nar().run(1000);

        System.out.println("period: " + period + " avgReward=" + a.avgReward() + " avgDex=" + a.dex.getMean());
        assertTrue(a.avgReward() > 0.5f);
        assertTrue(a.dex.getMean() > 0f);
    }

    @Test
    public void testInvert() {

        NAR n = nar();

//        n.log();
//        Param.DEBUG = true;
//        n.onTask(x -> {
//           if (x instanceof DerivedTask)
//               System.err.println(x.proof());
//        }, GOAL);

//        n.onCycle(()->{
//            n.attn.active.print();
//            System.out.println();
//        });

        assertInverts(n, (a) -> {

        });
    }


    static void assertInverts(NAR n, Consumer<NAgent> init) {


        MiniTest a = new ToggleNegate(n, $.the("t"),
                $$("y"),

                true);

        init.accept(a);

        n.run(500);

        assertTrue(-(-1 - a.avgReward()) > 0.2f, ()->""+a.avgReward());
        assertTrue(a.dex.getMean() > 0.01f, ()->a.dex.toString());
    }


    @Deprecated abstract static class MiniTest extends NAgent {

        public float rewardSum = 0;
        final SummaryStatistics dex = new SummaryStatistics();

        public MiniTest(Term id, NAR n) {
            super(id, FrameTrigger.durs(1), n);
            //statPrint = n.emotion.printer(System.out);

            reward($.the("reward"), () -> {
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

    static class ToggleSame extends MiniTest {

        private float reward;

        public ToggleSame(NAR n, Term env, Term action, boolean posOrNeg) {
            super(env, n);

            actionPushButton(action, (BooleanProcedure) (v) ->
                this.reward = posOrNeg ? (v ? 1 : 0) : (v ? 0 : 1));
        }

        @Override
        public float reward() {
            float r = reward;
            reward = 0;
            return r;
        }

    }
    static class ToggleOscillate extends MiniTest {

        private float reward;

        public ToggleOscillate(NAR n, Term env, Term action, int period) {
            super(env, n);
            reward = 0;

            BooleanProcedure pushed = (v) -> {

                boolean posOrNeg = Math.sin(Math.round(n.time() * 2 * Math.PI / period)) < 0;
                this.reward = (v == posOrNeg) ? 1 : 0;
            };


            actionPushButton(action, pushed);
        }

        @Override
        public float reward() {
            float r = reward;
            reward = 0;
            return r;
        }

    }

    /**
     * reward for rapid inversion/oscillation of input action
     */
    static class ToggleNegate extends MiniTest {

        private int y;
        private int prev = 0;

        public ToggleNegate(NAR n, Term env, Term action, boolean toggleOrPush) {
            super(env, n);
            y = 0;

            BooleanProcedure pushed = (v) -> {

                this.y = v ? 1 : -1;
            };
            if (toggleOrPush)
                actionToggle(action, pushed);
            else
                actionPushButton(action, pushed);
        }

        @Override
        public float reward() {
            float r;

            if (y == prev) {
                r = -1;
            } else {
                r = 1;
            }

            prev = y;

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