//package nars.concept.sensor;
//
//import jcog.Util;
//import jcog.math.FloatRange;
//import nars.$;
//import nars.NAR;
//import nars.Task;
//import nars.concept.PermanentConcept;
//import nars.concept.TaskConcept;
//import nars.table.BeliefTables;
//import nars.table.dynamic.DynamicTaskTable;
//import nars.task.signal.SignalTask;
//import nars.task.util.Answer;
//import nars.target.Term;
//import nars.target.Termed;
//import nars.time.Tense;
//import nars.truth.Truth;
//import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.List;
//import java.util.function.Predicate;
//
///**
// * dynamically computed time-dependent function
// */
//public class Scalar extends TaskConcept implements Sensor, PermanentConcept {
//
//    private FloatRange pri, res;
//
//    public Scalar(Term c, LongToFloatFunction belief, NAR n) {
//        this(c, belief, null, n);
//    }
//
//    public Scalar(Term target, @Nullable LongToFloatFunction belief, @Nullable LongToFloatFunction goal, NAR n) {
//        super(target,
//                belief != null ?
//                        new BeliefTables(
//                                new ScalarBeliefTable(target, true, belief, n) ) : n.conceptBuilder.newTable(target, true),
//
//                goal != null ?
//                        new BeliefTables(
//                                new ScalarBeliefTable(target, false, goal, n) ) : n.conceptBuilder.newTable(target, false)
//                ,
//                n.conceptBuilder);
//
//        pri = FloatRange.unit(goal!=null ? n.goalPriDefault : n.beliefPriDefault );
//        res = FloatRange.unit(n.freqResolution);
//
//        if (belief!=null) {
//            ScalarBeliefTable b = ((BeliefTables)beliefs()).tableFirst(ScalarBeliefTable.class);
//            b.setPri(pri);
//            b.setRes(res);
//        }
//        if (goal!=null) {
//            ScalarBeliefTable g = ((BeliefTables)goals()).tableFirst(ScalarBeliefTable.class);
//            g.setPri(pri);
//            g.setRes(res);
//        }
//        n.on(this);
//    }
//
//    @Override
//    public Iterable<Termed> components() {
//        return List.of(this);
//    }
//
//    @Override
//    public void update(long last, long now, long next, NAR nar) {
//        //?
//    }
//
//    @Override
//    public FloatRange resolution() {
//        return res;
//    }
//
//    @Override
//    public FloatRange pri() {
//        return pri;
//    }
//
//    /** samples at time-points */
//    static class ScalarBeliefTable extends DynamicTaskTable {
//
//        FloatRange pri, res;
//
//        private final LongToFloatFunction value;
//        final long stampSeed;
//        final long stampStart;
//
//        protected ScalarBeliefTable(Term target, boolean beliefOrGoal, LongToFloatFunction value, NAR n) {
//            super(target, beliefOrGoal);
//            stampStart = n.time();
//
//            this.stampSeed = target.hashCode();
//
//            this.value = value;
//        }
//
//        protected void setPri(FloatRange pri) {
//            this.pri = pri;
//        }
//
//        protected void setRes(FloatRange res) {
//            this.res = res;
//        }
//
//        @Override
//        protected Task taskDynamic(Answer a) {
//            Term template = a.template;
//            if (template == null)
//                a.template = template = target;
//
//            long start = a.time.start;
//            long end = a.time.end;
//            NAR nar = a.nar;
//            long mid = Tense.dither((start+end)/2L, nar);
//            Truth t = truthDynamic(mid, mid, template, a.filter, nar);
//            if (t == null)
//                return null;
//            else {
//
//                long stampDelta = mid - stampStart; //TODO optional dithering
//                long stamp = stampDelta ^ stampSeed; //hash
//                SignalTask tt = new SignalTask(target, punc(), t, nar.time(), mid, mid, stamp);
//                tt.pri(pri.get());
//                return tt;
//            }
//        }
//
//        @Override
//        protected Truth truthDynamic(long start, long end, Term template /* ignored */, Predicate<Task> filter, NAR nar) {
//            long t = (start + end) / 2L; //time-point
//            float f = value.valueOf(t);
//            return f == f ? truth(Util.round(f, res.get()), nar) : null;
//        }
//
//        /** truther: value -> truth  */
//        protected Truth truth(float v, NAR n) {
//            return $.t(v, n.confDefault(punc()));
//        }
//
//    }
//}
