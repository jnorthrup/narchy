package nars.op.stm;

import jcog.data.list.FasterList;
import jcog.pri.NLink;
import jcog.pri.Priority;
import jcog.pri.VLink;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.BagClustering;
import nars.control.Cause;
import nars.control.channel.BufferedCauseChannel;
import nars.exe.Causable;
import nars.task.NALTask;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.term.util.Conj;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.truth.TruthFunctions.c2w;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

public class ConjClustering extends Causable {


    private final BagClustering<Task> bag;
    private final BagClustering.Dimensionalize<Task> model;
    private final BufferedCauseChannel in;
    private final byte punc;
    private final float termVolumeMaxFactor = 0.9f;
    private final Predicate<Task> filter;
    private long now;
    private int dur;
    private float confMin;
    private int volMax;
    private int ditherTime;
    private int taskLimitPerCentroid;

    //temporary to the current singleton
    transient private int tasksGenerated;

    public ConjClustering(NAR nar, byte punc, int centroids, int capacity) {
        this(nar, punc, (t)->true, centroids, capacity);
    }

    public ConjClustering(NAR nar, byte punc, Predicate<Task> filter, int centroids, int capacity) {
        super();

        this.in = nar.newChannel(this).buffered();

        this.dur = nar.dur();

        this.model = new BagClustering.Dimensionalize<>(4) {

            @Override
            public void coord(Task t, double[] c) {
                c[0] = t.start();
                c[3] = t.priElseZero();
                Truth tt = t.truth();
                c[1] = tt.polarity();
                c[2] = tt.conf();
            }

            @Override
            public double distanceSq(double[] a, double[] b) {
                return (1 + Math.abs(a[0] - b[0]) / dur)
                        *
                        (
                                Math.abs(a[1] - b[1])
                                        + Math.abs(a[2] - b[2])
                                        + Math.abs(a[3] - b[3]) * 0.1f
                        );
            }
        };

        this.bag = new BagClustering<>(model, centroids, capacity);

        this.punc = punc;
        this.filter = filter;

        nar.on(this);
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        ons.add(nar.onTask(t -> {
            if (!t.isEternal()
                    && t.punc() == punc
                    && !t.hasVars() //<-- TODO requires multi-normalization (shifting offsets)
                    && filter.test(t)) {
                bag.put(t,

                        t.priElseZero()

                );

            }
        }));

    }

    @Override
    public void run(NAR n, int workRequested, Consumer<NLink<Runnable>> buffer) {
        if (bag == null || bag.bag.isEmpty())
            return;

        super.run(n, workRequested, buffer);
    }

    @Override
    protected int next(NAR nar, int iterations /* max tasks generated per centroid, >=1 */) {

        if (bag==null || bag.bag.isEmpty()) return 0;

        this.now = nar.time();
        this.dur = nar.dur();
        this.ditherTime = nar.dtDither();
        this.confMin = nar.confMin.floatValue();
        this.volMax = Math.round(nar.termVolumeMax.intValue() * termVolumeMaxFactor);
        this.taskLimitPerCentroid = Math.max(1, Math.round(((float) iterations) / bag.net.centroids.length));

        tasksGenerated = 0;

        bag.commitGroups(nar, nar.forgetRate.floatValue(), this::conjoinCentroid);

        if (tasksGenerated > 0) {
            in.commit();
        }
        return iterations;
    }


    private void conjoinCentroid(Stream<VLink<Task>> group, NAR nar) {

        Iterator<VLink<Task>> gg =
                group.filter(x -> x != null && !x.isDeleted()).iterator();

        Map<LongObjectPair<Term>, Task> vv = new HashMap<>(16);
        FasterList<Task> actualTasks = new FasterList(8);


        int centroidGen = 0;


        LongHashSet actualStamp = new LongHashSet();

        main:
        while (gg.hasNext() && centroidGen < taskLimitPerCentroid) {

            vv.clear();
            actualTasks.clear();
            actualStamp.clear();


            long end = Long.MIN_VALUE;
            long start = Long.MAX_VALUE;

            float freq = 1f;
            float conf = 1f;
            float priMax = Float.NEGATIVE_INFINITY, priMin = Float.POSITIVE_INFINITY;
            float confMax = Float.NEGATIVE_INFINITY;//, confMin = Float.POSITIVE_INFINITY;
            int vol = 0;
            int volMax = 0;

            do {
                if (!gg.hasNext())
                    break;

                Task t =
                        gg.next().id;

                Term xt = t.term();

                long zs = Tense.dither(t.start(), ditherTime);


                Truth tx = t.truth();
                Term xtn = xt.neg();
                if (tx.isNegative()) {
                    xt = xtn;
                }

                int xtv = xt.volume();
                volMax = Math.max(volMax, xt.volume());
                if (vol + xtv + 1 >= this.volMax || conf * tx.conf() < confMin) {
                    continue;
                }

                boolean include = false;
                LongObjectPair<Term> ps = pair(zs, xt);
                Term xtNeg = xt.neg();


                if (!Stamp.overlapsAny(actualStamp, t.stamp())) {
                    if (!vv.containsKey(pair(zs, xtNeg)) && null == vv.putIfAbsent(ps, t)) {
                        vol += xtv;
                        include = true;
                    }
                }


                if (include) {

                    actualTasks.add(t);

                    actualStamp.addAll(t.stamp());

                    if (start > zs) start = zs;
                    if (end < zs) end = zs;

                    float tc = tx.conf();
                    if (tc > confMax) confMax = tc;

                    conf *= tc;

                    float tf = tx.freq();
                    freq *= tx.isNegative() ? (1f - tf) : tf;

                    float p = t.priElseZero();
                    if (p < priMin) priMin = p;
                    if (p > priMax) priMax = p;

                    if (actualTasks.size() >= Param.STAMP_CAPACITY)
                        break;
                }
            } while (vol < this.volMax - 1 && conf > confMin);

            int vs = actualTasks.size();
            if (vs < 2)
                continue;


            Task[] uu = actualTasks.toArrayRecycled(Task[]::new);


            float e = c2w(conf);
            if (e > 0) {
                final Truth t = Truth.theDithered(freq, e, nar);
                if (t != null) {

                    Term cj = Conj.conj(vv.keySet());
                    if (cj != null) {





                            ObjectBooleanPair<Term> cp = Task.tryContent( Term.forceNormalizeForBelief(cj), punc, true);
                            if (cp != null) {


                                NALTask m = new STMClusterTask(cp, t, start, start, actualStamp.toArray(), punc, now);


                                m.cause = Cause.sample(Param.causeCapacity.intValue(), uu);


                                int v = cp.getOne().volume();
                                float cmplFactor =
                                        ((float) v) / (v + volMax);

                                float freqFactor =
                                        t.freq();
                                float confFactor =
                                        (conf / (conf + confMax));

                                m.pri(Priority.fund(priMin * freqFactor * cmplFactor * confFactor, false, uu));

                                if (in.inputIfCapacity(m)) {
                                    centroidGen++;
                                    tasksGenerated++;
                                } else {
                                    return;
                                }
                            }
                        }


                }
            }


        }

    }

    @Override
    public float value() {
        return in.value();
    }


    public static class STMClusterTask extends NALTask {

        STMClusterTask(@Nullable ObjectBooleanPair<Term> cp, Truth t, long start, long end, long[] evidence, byte punc, long now) throws InvalidTaskException {
            super(cp.getOne(), punc, t.negIf(cp.getTwo()), now, start, end, evidence);
        }

        @Override
        public boolean isInput() {
            return false;
        }
    }


}
