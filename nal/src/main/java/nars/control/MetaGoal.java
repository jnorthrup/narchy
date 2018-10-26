package nars.control;

import com.google.common.collect.TreeBasedTable;
import jcog.Paper;
import jcog.data.list.FasterList;
import nars.NAR;
import nars.Task;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectDoubleHashMap;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

import static jcog.Texts.n4;

/**
 * high-level reasoner control parameters
 * neg: perceived as input, can be measured for partial and complete tasks
 * in their various stages of construction.
 * <p>
 * information is considered negative value by default (spam).
 * see: http:
 * <p>
 * satisfying other goals is necessary to compensate for the
 * perceptual cost.
 * <p>
 * there is also the "meta-goal" system which allows me to specify the high-level motivation of the system in terms of a few variables.  i can direct it to focus on action rather than belief, or throttle perception, or instead concentrate on answering questions.  more of these meta goals can be added but the total number needs to be kept small because each one is tracked for every cause in the system which could be hundreds or thousands.  so i settled on 5 basic ones so far you can see these in the EXE menu.  each can be controlled by a positive or negative factor, 0 being neutral and having no effect.  negative being inhibitory.  it uses these metagoals to to estimate the 'value' of each 'cause' the system is able to choose to apply throughout its operating cycles.  when it knows the relative value of a certain cause it can allocate an appropriate amount of cpu time for it in the scheduler.   these are indicated in the animated tree chart enabled by the 'CAN' button
 */
@Paper
public enum MetaGoal {

    PerceiveCmplx,  //by complexity
    PerceivePri, //by priority

    /**
     * pos: accepted as belief
     */
    Believe,

    /**
     * pos: accepted as goal
     */
    Desire,

//    /**
//     * pos: anwers a question
//     */
//    Answer,

    /**
     * pos: actuated a goal concept
     */
    Action;



    /**
     * learn that the given effects have a given value
     * note: requires that the FasterList's internal array is correct Cause[] type for direct un-casting access
     */
    public void learn(short[] cause, float strength, FasterList<Cause> causes) {

        if (Math.abs(strength) < Float.MIN_NORMAL)
            return;

        int n = cause.length;
        if (n == 0)
            return;

        float s =
                strength / n;
                //strength;

        int ordinal = ordinal();
        Cause[] cc = causes.array();
        for (short c : cause) {
            MetaGoal.learn(cc[c].credit, ordinal, s);
        }
    }

    /**
     * contributes the value to a particular goal in a cause's goal vector
     */
    protected static void learn(Traffic[] goalValue, int ordinal, float v) {
        goalValue[ordinal].add(v);
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
            value += ((Cause) vv[c]).value();


        return value / effects;
    }


//    public static AgentBuilder newController(NAgent a) {
//        NAR n = a.nar;
//
//        Emotion ne = n.emotion;
//        Arrays.fill(ne.want, 0);
//
//        AgentBuilder b = new AgentBuilder(
//
//                HaiQae::new,
//
//                () -> a.enabled.get() ? (0.1f + a.dexterity()) * Util.tanhFast(a.reward) /* - lag */ : 0f)
//
//                .in(a::dexterity)
//                .in(a.happy)
//
//
//
//
//                .in(new FloatNormalized(
//
//                        new FloatFirstOrderDifference(n::time, () -> n.emotion.deriveTask.getValue().longValue())
//                ).relax(0.1f))
//                .in(new FloatNormalized(
//
//                                new FloatFirstOrderDifference(n::time, () -> n.emotion.premiseFire.getValue().longValue())
//                        ).relax(0.1f)
//                ).in(new FloatNormalized(
//                                n.emotion.busyVol::getSum
//                        ).relax(0.1f)
//                );
//
//
//        for (MetaGoal g : values()) {
//            final int gg = g.ordinal();
//            float min = -2;
//            float max = +2;
//            b.in(new FloatPolarNormalized(() -> ne.want[gg], max));
//
//            float step = 0.5f;
//
//            b.out(2, (w) -> {
//                float str = 0.05f + step * Math.abs(ne.want[gg] / 4f);
//                switch (w) {
//                    case 0:
//                        ne.want[gg] = Math.min(max, ne.want[gg] + str);
//                        break;
//                    case 1:
//                        ne.want[gg] = Math.max(min, ne.want[gg] - str);
//                        break;
//                }
//            });
//        }
//
//
//
//
//
//
//
//
//
//
//
//
//        return b;
//    }

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
                for (Traffic t : c.credit) {
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

        public synchronized void print(PrintStream out) {
            keyValuesView().toSortedListBy(x -> -x.getTwo()).forEach(x ->
                    out.println(
                            n4(x.getTwo()) + '\t' + MetaGoal.values()[x.getOne().getTwo()] + '\t' + x.getOne().getOne()
                    )
            );
        }
    }

    public static String proof(Task t, NAR nar) {
        short[] tc = t.cause();
        StringBuilder sb = new StringBuilder();
        for (short s : tc) {
            Cause c = nar.causes.get(s);
            sb.append(c.toString()).append('\n');
        }
        return sb.toString().trim();
    }
}
