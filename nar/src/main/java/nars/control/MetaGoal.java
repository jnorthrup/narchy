package nars.control;

import com.google.common.collect.TreeBasedTable;
import jcog.Paper;
import jcog.Util;
import jcog.data.list.FasterList;
import nars.NAR;
import nars.Task;
import nars.term.Term;
import nars.time.part.DurLoop;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectDoubleHashMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.function.BiConsumer;

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

    //Futile,

    Perceive, //by volume, at beginning of input

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

//    /**
//     * pos: actuated a goal concept
//     */
//    Action,

    ;


    public static DurLoop addGovernor(NAR n, BiConsumer<NAR,FasterList<Cause>> valuePrioritizer) {
        return n.onDur(nn -> value(nn, valuePrioritizer));
    }

    public void learn(Task t, float strength, NAR n) {
        Term why = t.why();
        if (why != null)
            learn(why, strength, n.control.why);
    }

    void learn(Term why, float pri, FasterList<Cause> whies) {
        Why.eval(why, pri, whies.array(), learner);
    }

    private final Why.Evaluator<Cause> learner = (CC, w, p)-> learn(CC[w], p);
    private final int g = ordinal(); //just in case this isnt JIT'd

    private void learn(Cause cause, float pri) {
        cause.credit.addAt(pri, g);
    }

//    /**
//     * learn that the given effects have a given value
//     * note: requires that the FasterList's internal array is correct Cause[] type for direct un-casting access
//     */
//    @Deprecated public void learn(float strength, FasterList<Cause> whies, short... why) {
//
//        int n = why.length;
//        if (n == 0)
//            return;
//
//        float s =
//            strength / n;
//        //strength;
//        if (Math.abs(s) < Float.MIN_NORMAL)
//            return;
//
//
//
//
//        int ordinal = ordinal();
//        Cause[] cc = whies.array();
//        for (short c : why) {
//            Cause ccc = cc[c];
////            if (why!=null) //HACK
//                learn(ccc.credit, ordinal, s);
//        }
//    }


    /** default linear adder
     * @param valuePrioritizer responsible for reading the 'value' computed here and assigning an effective
     *                         priority to it
     **/
    static public void value(NAR n, @Nullable BiConsumer<NAR, FasterList<Cause>> valuePrioritizer) {

        FasterList<Cause> cause = n.control.why;
        int cc = cause.size();
        if (cc == 0)
            return;

        if (Util.equals(Util.sumAbs(n.emotion.want),0))
            return; //flat metagoal early exit

        Cause[] ccc = cause.array();


        double[] want = Util.toDouble(n.emotion.want);

        float[] credit = new float[want.length];

        for (int i = 0; i < cc; i++) {

            Cause ci = ccc[i];

            double v = 0;
            //boolean valued = false;
            ci.commit(credit);
            for (int w = 0; w < want.length; w++) {

                //if (Math.abs(c) > Float.MIN_NORMAL) {
                    v += want[w] * credit[w];
                    //valued = true;
                //}
            }

            ci.value(
                //valued ? (float)v : Float.NaN
                (float)v
            );
        }

        if (valuePrioritizer!=null)
            valuePrioritizer.accept(n, cause);

//        @Nullable Consumer<Why[]> g = this.governor;
//        if (g!=null)
//            g.accept(ccc);
    }

//    /** implements value/pri feedback */
//    @Nullable
//    private Consumer<Why[]> governor = null;
//    /** sets the governor to be used in next value/pri feedback iteration */
//    public MetaGoal governor(Consumer<Why[]> governor) {
//        this.governor = governor;
//        return MetaGoal;
//    }

//    /**
//     * contributes the value to a particular goal in a cause's goal vector
//     */
//    protected static void learn(Credit[] goalValue, int ordinal, float v) {
//        goalValue[ordinal].add(v);
//    }


    public static final Logger logger = LoggerFactory.getLogger(MetaGoal.class);


    /**
     * estimate the priority factor determined by the current value of priority-affecting causes
     */
    @Deprecated public static float privaluate(FasterList<Cause> values, short[] effect) {

        int effects = effect.length;
        if (effects == 0) return 0;

        float value = 0;
        Object[] vv = values.array();
        for (short c : effect)
            value += ((Cause) vv[c]).pri();


        return value / effects;
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

//                int i = 0;
//                MetaGoal[] values = MetaGoal.values();
                //TODO
//                for (Credit t : c.credit) {
//
//                    MetaGoal m = values[i];
//                    double tt = //t.total();
//                        t.floatValue();
//                    if (tt != 0) {
//                        synchronized (this) {
//                            addToValue(PrimitiveTuples.pair(c, (byte) i), tt);
//                        }
//                    }
//                    i++;
//                }

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

}
