package nars.truth.dynamic;

import jcog.Paper;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.math.LongInterval;
import jcog.sort.QuickSort;
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
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static nars.Op.NEG;

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


    static class Component implements Function<DynTaskify,Task> {
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
            return AbstractDynamicTruth.subTask(table, term, start, end, d.filter, d);
        }
    }

    protected FasterList<Component> components = null;

    public final AbstractDynamicTruth model;

    public  final NAR nar;
    public final Compound template;

    /** ditherTruth applied only to final result; not to projected subTasks */
    final boolean ditherTruth;


    final float dur;

    final boolean beliefOrGoal;
    private final Predicate<Task> filter;
    public final MetalBitSet componentPolarity;


    public DynTaskify(AbstractDynamicTruth model, boolean beliefOrGoal, boolean ditherTruth, @Nullable Compound template, float dur, Predicate<Task> filter, NAR nar) {
        super(0);
        this.model = model;
        this.beliefOrGoal = beliefOrGoal;
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
        this(model, beliefOrGoal, a.ditherTruth, (Compound)a.term(), a.dur, a.filter, a.nar);
    }

    public @Nullable Task eval(long start, long end) {
        return model.evalComponents(template, start, end, this::evalComponent) && components() ?
            task() : null;
    }

    @Override
    public @Nullable Task task() {

        long s, e;
        long earliest;
        long latest = maxValue(Stamp::end);
        if (latest == LongInterval.ETERNAL) {
            //all are eternal
            earliest = s = e = LongInterval.ETERNAL;
        } else {

            earliest = earliestStart();

            if (model.temporal()) {
                //calculate the minimum range (ie. intersection of the ranges)
                s = earliest;
                e = s == LongInterval.ETERNAL ?
                    LongInterval.ETERNAL :
                    s + minValue(t -> t.rangeIfNotEternalElse(1) - 1);
            } else {

                long[] u = Tense.union(0, this);
                s = u[0];
                e = u[1];
//                s = e = ETERNAL;

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

            int[] order = IntStream.range(0, cn).toArray();
            Component[] cc = c.array();

            IntToFloatFunction smallestFirst = (int j) -> +cc[j].termVolume;
            //IntToFloatFunction biggestFirst = (int j) -> -(cc[j]).termVolume;
            QuickSort.sort(order,
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
        items[i] = x;
        this.size++;
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
