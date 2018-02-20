package nars.control;

import com.google.common.collect.TreeBasedTable;
import jcog.Paper;
import jcog.Util;
import jcog.learn.ql.HaiQAgent;
import jcog.list.FasterList;
import jcog.math.FloatFirstOrderDifference;
import jcog.math.FloatNormalized;
import jcog.math.FloatPolarNormalized;
import nars.Emotion;
import nars.NAR;
import nars.NAgent;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectDoubleHashMap;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Arrays;

import static jcog.Texts.n4;

/**
 * high-level reasoner control parameters
 * neg: perceived as input, can be measured for partial and complete tasks
 * in their various stages of construction.
 * <p>
 * information is considered negative value by default (spam).
 * see: http://mattmahoney.net/costofai.pdf
 * <p>
 * satisfying other goals is necessary to compensate for the
 * perceptual cost.
 */
@Paper
public enum MetaGoal {
    Perceive,

    /**
     * pos: accepted as belief
     */
    Believe,

    /**
     * pos: accepted as goal
     */
    Desire,

    /**
     * pos: anwers a question
     */
    Answer,

    /**
     * pos: actuated a goal concept
     */
    Action,

    /**
     * pos: prediction confirmed a sensor input
     * neg: contradicted a sensor input
     */
    Accurate;


    /**
     * learn that the given effects have a given value
     */
    public void learn(short[] cause, float strength, FasterList<Cause> causes) {

        int n = cause.length;
        if (n == 0)
            return;

        if (Math.abs(strength) < Float.MIN_NORMAL)
            return; //would have no effect

        float s = strength/n;

        int ordinal = ordinal();
        for (short c : cause) {
            MetaGoal.learn(causes.get(c).goal, ordinal, s);
        }
    }

    /**
     * contributes the value to a particular goal in a cause's goal vector
     */
    protected static void learn(Traffic[] goalValue, int ordinal, float v) {
        goalValue[ordinal].addAndGet(v);
    }





    public static final Logger logger = LoggerFactory.getLogger(MetaGoal.class);

    /**
     * estimate the priority factor determined by the current value of priority-affecting causes
     */
    public static float privaluate(FasterList<Cause> values, short[] effect) {

        int effects = effect.length;
        if (effects == 0) return 0;

        float value = 0;
        Object[] vv = values.array();
        for (short c : effect)
            value += ((Cause)vv[c]).value();


        return value / effects;
    }


    public static AgentBuilder newController(NAgent a) {
        NAR n = a.nar;

        Emotion ne = n.emotion;
        Arrays.fill(ne.want, 0);

        AgentBuilder b = new AgentBuilder(
                //DQN::new,
                HaiQAgent::new,
                //() -> Util.tanhFast(a.dexterity())) //reward function
                () -> a.enabled.get() ? (0.1f + a.dexterity()) * Util.tanhFast(a.reward) /* - lag */ : 0f) //reward function

                .in(a::dexterity)
                .in(a.happy)
//                .in(new FloatNormalized(
//                        ((Emotivation) n.emotion).cycleDTRealMean::getValue)
//                        .decay(0.9f)
//                )
                .in(new FloatNormalized(
                        //TODO use a Long-specific impl of this:
                        new FloatFirstOrderDifference(n::time, () -> n.emotion.deriveTask.getValue().longValue())
                ).relax(0.1f))
                .in(new FloatNormalized(
                                //TODO use a Long-specific impl of this:
                                new FloatFirstOrderDifference(n::time, () -> n.emotion.premiseFire.getValue().longValue())
                        ).relax(0.1f)
                ).in(new FloatNormalized(
                                n.emotion.busyVol::getSum
                        ).relax(0.1f)
                );


        for (MetaGoal g : values()) {
            final int gg = g.ordinal();
            float min = -2;
            float max = +2;
            b.in(new FloatPolarNormalized(() -> ne.want[gg], max));

            float step = 0.5f;

            b.out(2, (w) -> {
                float str = 0.05f + step * Math.abs(ne.want[gg] / 4f);
                switch (w) {
                    case 0:
                        ne.want[gg] = Math.min(max, ne.want[gg] + str);
                        break;
                    case 1:
                        ne.want[gg] = Math.max(min, ne.want[gg] - str);
                        break;
                }
            });
        }

//        .out(
//                        new StepController((x) -> n.time.dur(Math.round(x)), 1, n.dur(), n.dur() * 2)
//                ).out(
//                        StepController.harmonic(n.confMin::setValue, 0.01f, 0.08f)
//                ).out(
//                        StepController.harmonic(n.truthResolution::setValue, 0.01f, 0.08f)
//                ).out(
//                        StepController.harmonic(a.curiosity::setValue, 0.01f, 0.16f)
//                ).get(n);


        return b;
    }

    public static class Report extends ObjectDoubleHashMap<ObjectBytePair<Cause>> {

        public TreeBasedTable<Cause, MetaGoal, Double> table() {
            TreeBasedTable<Cause, MetaGoal, Double> tt = TreeBasedTable.create();
            MetaGoal[] mv = MetaGoal.values();
            synchronized (this) {
                forEachKeyValue((k, v) -> {
                    Cause c = k.getOne();
                    MetaGoal m = mv[k.getTwo()];
                    tt.put(c, m, v);
                });
            }
            return tt;
        }

        public Report add(Report r) {
            synchronized (this) {
                r.forEachKeyValue(this::addToValue);
            }
            return this;
        }

        public Report add(Iterable<Cause> cc) {

            cc.forEach(c -> {

                int i = 0;
                for (Traffic t : c.goal) {
                    MetaGoal m = MetaGoal.values()[i];
                    double tt = t.total;
                    if (tt != 0) {
                        synchronized (this) {
                            addToValue(PrimitiveTuples.pair(c, (byte) i), tt);
                        }
                    }
                    i++;
                }

            });
            return this;
        }

        public void print(PrintStream out) {
            synchronized (this) {
                keyValuesView().toSortedListBy(x -> -x.getTwo()).forEach(x ->
                        out.println(
                                n4(x.getTwo()) + "\t" + MetaGoal.values()[x.getOne().getTwo()] + "\t" + x.getOne().getOne()
                        )
                );
            }
        }
    }

}
