package nars.truth.dynamic;

import jcog.Paper;
import jcog.data.bit.MetalBitSet;
import jcog.data.set.MetalLongSet;
import jcog.math.LongInterval;
import jcog.pri.Possibilities;
import jcog.pri.Possibilities.Possibility;
import nars.NAL;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.table.BeliefTable;
import nars.task.DynamicTruthTask;
import nars.task.NALTask;
import nars.task.util.Answer;
import nars.task.util.TaskList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.util.Timed;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static nars.Op.*;
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


    static class Component extends Possibility<DynTaskify,Task> {
        final Term term;
        final BeliefTable _concept;
        final long start, end;

        Component(Term term, BeliefTable _c, DynTaskify d, int currentComponent, long start, long end) {
            boolean negated = term.op() == Op.NEG;

            if (negated) {
                term = term.unneg();
                d.componentPolarity.clear(currentComponent);
            }

            this.term = term;
            this._concept = _c;
            this.start = start;
            this.end = end;
        }

        @Override
        public float value() {
            return 1/(1+term.volume());
        }

        @Override
        public Task apply(DynTaskify d) {
            return d.model.subTask(_concept, term, start, end, d.filter, d);
        }
    }

    Possibilities<DynTaskify,Task> components = null;

    public final AbstractDynamicTruth model;

    public  final NAR nar;
    public final Compound template;

    /** whether the result is intended for internal or external usage; determines precision settings */
    final boolean ditherTruth;
    //, ditherTime;
    final int dur;
    private MetalLongSet evi = null;

    final boolean beliefOrGoal;
    final Predicate<Task> filter;
    public final MetalBitSet componentPolarity;

    public DynTaskify(AbstractDynamicTruth model, boolean beliefOrGoal, boolean ditherTruth, boolean ditherTime,int dur, NAR nar) {
        this(model, beliefOrGoal, ditherTruth, ditherTime, null, dur, null, nar);
    }

    public DynTaskify(AbstractDynamicTruth model, boolean beliefOrGoal, boolean ditherTruth, boolean ditherTime, @Nullable Compound template, int dur, Predicate<Task> filter, NAR nar) {
        super(8 /* estimate */);
        this.model = model;
        this.beliefOrGoal = beliefOrGoal;
        this.ditherTruth = ditherTruth;

        this.template = template;
        assert(template==null || template.op() != NEG);

        this.dur = dur;
        this.nar = nar;
        this.filter = NAL.DYNAMIC_TRUTH_STAMP_OVERLAP_FILTER ?
                Answer.filter(filter, this::doesntOverlap) : filter;
        this.componentPolarity = MetalBitSet.bits(32).negate(); //all positive by default
    }

    public DynTaskify(AbstractDynamicTruth model, boolean beliefOrGoal, Answer a) {
        this(model, beliefOrGoal, a.ditherTruth, true, (Compound)a.term(), a.dur, a.filter, a.nar);
    }

    @Nullable public static Task merge(TaskList tasks, Term content, Truth t, Supplier<long[]> stamp, boolean beliefOrGoal, long start, long end, Timed w) {
        boolean neg = content instanceof Neg;
        if (neg)
            content = content.unneg();

        ObjectBooleanPair<Term> r = Task.tryTaskTerm(
                content,
                beliefOrGoal ? BELIEF : GOAL, !NAL.test.DEBUG_EXTRA);
        if (r==null)
            return null;

        NALTask y = new DynamicTruthTask(
                r.getOne(), beliefOrGoal,
                t.negIf(neg != r.getTwo()),
                w, start, end,
                stamp.get());



//        y.pri(
//              tasks.reapply(TaskList::pri, NAL.DerivationPri)
//                        // * dyn.originality() //HACK
//        );
        Task.fund(y, tasks.toArrayRecycled(), true);

        return y;
    }

    @Nullable
    public Task eval(long start, long end) {
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
                if (u == null)
                    return null;

                s = u[0];
                e = u[1];

            }

        }


        Term y = model.reconstruct(template, s, e, this);
        if (y==null || !y.unneg().op().taskable /*|| y.hasXternal()*/) { //quick tests
            if (NAL.DEBUG) {
                //TEMPORARY for debug
//                  model.evalComponents(answer, (z,start,end)->{
//                      System.out.println(z);
//                      nar.conceptualizeDynamic(z).beliefs().match(answer);
//                      return true;
//                  });
//                  model.reconstruct(template, this, s, e);
//                throw new TermException("DynTaskify template not reconstructed: " + this, template);
            }
            return null;
        }


        boolean absolute = (model!=Impl && model != ConjIntersection) || s == LongInterval.ETERNAL || earliest == LongInterval.ETERNAL;
        for (int i = 0, dSize = size(); i < dSize; i++) {
            Task x = get(i);
            long xStart = x.start(); if (xStart!=ETERNAL) {
                long shift = absolute || (xStart == ETERNAL) ? 0 : xStart - earliest;
                long ss = s + shift, ee = e + shift;
                if (xStart != ss || x.end() != ee) {
                    Task tt = Task.project(x, ss, ee,
                            NAL.truth.EVI_MIN, //minimal truth threshold for accumulating evidence
                            false,
                            1, //no need to dither truth or time here.  maybe in the final calculation though.
                            dur,
                            nar);
                    if (tt == null)
                        return null;
                    setFast(i, tt);
                }
            }
        }


        Truth t = model.truth(this);
        if (t == null)
            return null;

        /** interpret the presence of truth dithering as an indication this is producng something for 'external' usage,
         *  and in which case, also dither time
         */

        if (ditherTruth) {
            //dither and limit truth
            t = t.dither(nar);
            if (t == null)
                return null;
        }

//        if (ditherTime) {
//            if (s!= LongInterval.ETERNAL) {
//                int dtDither = nar.dtDither();
//                s = Tense.dither(s, dtDither, -1);
//                e = Tense.dither(e, dtDither, +1);
//            }
//        }

        return merge(this, y, t, stamp(nar.random()), beliefOrGoal, s, e, nar);
    }

    public boolean components() {
        if (components == null) {
            return false;
        }
        int cn = components.size();
        if (!components.commit(true, true))
            return false;
        int[] order = new int[cn];
        for (int i = 0; i < cn; i++)
            order[i] = i;

        Task[] yy = new Task[cn]; //HACK temporary buffer for sizing issue
        ensureCapacityForAdditional(cn);
        //ArrayUtil.sort(order, (int x) -> -components.list.get(x).term.volume());

        for (int i = 0; i < cn; i++) {
            int j = order[i];
            Task tt = components.list.get(j).apply(this);
            if (tt == null)
                return false;
            add(tt); //HACK necessary for stamp detection
            yy[j] = tt;
        }
        System.arraycopy(yy, 0, items, 0, yy.length); //HACK proper order
        return true;
    }


    private boolean evalComponent(Term subTerm, long start, long end) {
        Term su = subTerm.unneg();

        BeliefTable table = nar.tableDynamic(su, beliefOrGoal);
        if (table==null || table.isEmpty())
            return false;

        if (components == null)
            components = new Possibilities(this);//new FasterList(4);

        components.add(new Component(su, table,
                this, components.size(),
                start, end));

        return true;
    }





    @Override
    public boolean add(Task x) {

        super.add(x);

        if (evi == null) {
            switch (size) {
                case 1: //dont create set yet
                    assert(evi==null);
                    break;
                case 2: //more than 2:
                    assert(evi==null);
                    long[] a = get(0).stamp(), b = get(1).stamp();
                    evi = Stamp.toSet(a.length + b.length, a, b);
                    break;
                default:
                    evi.addAll(x.stamp());
                    break;
            }
        }


        return true;
    }

    private boolean doesntOverlap(Task t) {
        return size == 0 /*size < capacity()*/ || doesntOverlap(t.stamp());
    }

    private boolean doesntOverlap(long[] stamp) {
        MetalLongSet e = this.evi;
        if (e != null) {
            long[] s = stamp;
            for (long x : s) {
                if (e.contains(x))
                    return false;
            }
        } else if (size > 0) {
            //delay creation of evi set one more item
            assert(size == 1);
            return !Stamp.overlapsAny(get(0).stamp(), stamp);
        }

        return true;
    }


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



    private Supplier<long[]> stamp(Random rng) {
        if (evi == null) {
            switch(size) {
                case 1:
                    return get(0)::stamp;
                case 2:
                    return ()-> {
                        long[] a = get(0).stamp(), b = get(1).stamp();
                        return Stamp.sample(NAL.STAMP_CAPACITY, Stamp.toSet(a.length + b.length, a, b), rng);
                    };
                case 0:
                default:
                    throw new UnsupportedOperationException();
            }
        } else {
            return ()->Stamp.sample(NAL.STAMP_CAPACITY, this.evi, rng);
        }
    }


    @Override
    public boolean clearIfChanged() {
        if (super.clearIfChanged()) {
            evi = null;
            return true;
        }
        return false;
    }

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
