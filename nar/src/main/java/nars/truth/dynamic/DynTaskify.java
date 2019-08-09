package nars.truth.dynamic;

import jcog.Paper;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.math.LongInterval;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.table.BeliefTable;
import nars.task.util.Answer;
import nars.task.util.TaskList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Stamp;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.NEG;
import static nars.truth.dynamic.DynamicConjTruth.ConjIntersection;
import static nars.truth.dynamic.DynamicStatementTruth.Impl;

/**
 * Dynamic Taskify
 *
 * uses dynamic truth models and recursive dynamic belief evaluation to compute
 * accurately truthed, timed, and evidentially stamped composite/aggregate truths
 * with varying specified or unspecified internal temporal features described by
 * a template target.
 *
 * additionally tracks evidential overlap while being constructed, and provide the summation of evidence after*/
@Paper
public class DynTaskify extends TaskList {




    private static class Component implements Function<DynTaskify,Task> {
        final Term term;
        final BeliefTable table;
        final long start, end;
        final int termVolume;

        Component(Term term, BeliefTable _c, long start, long end) {
            this.term = term;
            this.termVolume = term.volume();
            this.table = _c;
            this.start = start;
            this.end = end;
        }

        @Override public Task apply(DynTaskify d) {
            return d.model.subTask(table, term, start, end, d.filter, d);
        }
    }

    private FasterList<Component> components = null;

    public final AbstractDynamicTruth model;

    public  final NAR nar;
    public final Compound template;

    /** ditherTruth applied only to final result; not to projected subTasks */
    final boolean ditherTruth;

    final boolean ditherTime;

    final float dur;

    final boolean beliefOrGoal;
    final Predicate<Task> filter;
    final MetalBitSet componentPolarity;


    public DynTaskify(AbstractDynamicTruth model, boolean beliefOrGoal, boolean ditherTruth, boolean ditherTime, @Nullable Compound template, float dur, Predicate<Task> filter, NAR nar) {
        super(0);
        this.model = model;
        this.beliefOrGoal = beliefOrGoal;
        this.ditherTime = ditherTime;
        this.ditherTruth = ditherTruth;

        this.template = template;
        assert(template==null || template.op() != NEG);

        this.dur = dur;
        this.nar = nar;
        this.filter = NAL.DYNAMIC_TRUTH_STAMP_OVERLAP_FILTER ?
                Answer.filter(filter, this::noOverlap) : filter;
        this.componentPolarity = MetalBitSet.bits(32).negate(); //all positive by default
    }

    public DynTaskify(AbstractDynamicTruth model, boolean beliefOrGoal, Answer a) {
        this(model, beliefOrGoal, a.ditherTruth, true, (Compound)a.term(), a.dur, a.filter, a.nar);
    }

    @Nullable
    public Task eval(long start, long end) {
        if (template == null)
            throw new NullPointerException();
        return model.evalComponents(template, start, end, this::evalComponent) ? taskify() : null;
    }

    @Nullable public Task taskify() {

        if (!components())
            return null;

        long s, e;
        long earliest;
        long latest = maxValue(Stamp::end);
        if (latest == LongInterval.ETERNAL) {
            //all are eternal
            s = e = LongInterval.ETERNAL;
            earliest = LongInterval.ETERNAL;
        } else {

            earliest = earliestStart();


            if (model == ConjIntersection || model == Impl) {
                //calculate the minimum range (ie. intersection of the ranges)
                s = earliest;

                //long ss = a.time.start, ee = a.time.end;
                //long range = ee-ss;
//                if (ss != LongInterval.ETERNAL && ss != XTERNAL) {
//                    s = Util.clampSafe(s, ss, ee); //project sequence to when asked
//                }

                if (s == LongInterval.ETERNAL) {
                    e = LongInterval.ETERNAL;
                } else {
                    long range = (minValue(t -> t.isEternal() ? 0 : t.range()-1));
                    e = s + range;
                }

            } else {

                long[] u = Tense.union(0, this);
//                if (u == null)
//                    return null;

                s = u[0];
                e = u[1];

            }

        }

        return model.task(template, earliest, s, e, this);

    }

    private boolean components() {

        FasterList<Component> c = this.components;

        if (c == null )
            return false;

        int cn = c.size();
        if (cn == 0)
            return false;

        if (cn > 1) {

            int[] order = new int[cn];
            for (int i = 0; i < cn; i++)
                order[i] = i;
            Component[] cc = c.array();

            IntToFloatFunction smallestFirst = (int j) -> -cc[j].termVolume;
            //IntToFloatFunction biggestFirst = (int j) -> (cc[j]).termVolume;
            ArrayUtil.sort(order,
                    smallestFirst
                    //biggestFirst
            );

            for (int i = 0; i < cn; i++) {
                int j = order[i];
                Task tt = cc[j].apply(this);
                if (tt == null)
                    return false;

                if (i == 0)
                    ensureCapacityForAdditional(cn);

                setTask(j, tt);
            }
        } else {
            ensureCapacityForAdditional(1);
            Task only = c.get(0).apply(this);
            if (only == null)
                return false;
            setTask(0, only);
        }
        return true;
    }


    private boolean evalComponent(Term subTerm, long start, long end) {
        boolean negated = subTerm instanceof Neg;
        Term c;
        if (negated) {
            c = subTerm.unneg();
            componentPolarity.clear(components==null ? 0 : components.size());
        } else
            c = subTerm;

        BeliefTable table = nar.tableDynamic(c, beliefOrGoal);
        if (table==null || table.isEmpty())
            return false;

        if (components == null)
            components = new FasterList<>(0, new Component[model.componentsEstimate()]);

        components.add(new Component(c, table, start, end));

        return true;
    }

    @Override
    public boolean add(Task x) {
        throw new UnsupportedOperationException();
    }

    private void setTask(int i, Task x) {
        setFast(i, x);
        ++this.size;
    }

    private boolean noOverlap(Task t) {
        return size == 0 || !anySatisfyWith(Stamp::overlapNullable, t);
    }

//    private boolean noOverlap(long[] stamp) {
//        MetalLongSet e = this.evi;
//        if (e != null) {
//            long[] s = stamp;
//            for (long x : s) {
//                if (e.contains(x))
//                    return false;
//            }
//        }
//
//        return true;
//    }


//    public final Task task(Term target, Truth t, boolean beliefOrGoal, NAR n) {
//        return task(target, t, beliefOrGoal, false, n);
//
//    }

//    public final Truth truth(Term target, BiFunction<DynTruth, NAR, Truth> o, boolean beliefOrGoal, NAR n) {
//        return (Truth) eval(target, o, false, beliefOrGoal, n);
//    }

//    /**
//     * TODO make Task truth dithering optional
//     */
//    @Deprecated  public Truthed eval(Term superterm, @Deprecated BiFunction<DynTruth, NAR, Truth> truthModel, boolean taskOrJustTruth, boolean beliefOrGoal, NAR nar) {
//
//        Truth t = truthModel.apply(this, nar);
//        if (t == null)
//            return null;
//
//        //return eval(()->superterm, t, taskOrJustTruth, beliefOrGoal, freqRes, confRes, eviMin, nar);
//        if (taskOrJustTruth) {
//            return task(superterm, t, this::stamp, beliefOrGoal, nar);
//        } else {
//            return t;
//        }
//
//    }



    /** earliest start time, excluding ETERNALs.  returns ETERNAL if all ETERNAL */
    public long earliestStart() {
        long w = minValue(t -> {
            long ts = t.start();
            return ts != LongInterval.ETERNAL ? ts : LongInterval.TIMELESS;
        });
        return w == TIMELESS ? ETERNAL : w;
    }

    /** latest start time, excluding ETERNALs.  returns ETERNAL if all ETERNAL */
    public long latestStart() {
        return maxValue(Stamp::start);
    }

}
