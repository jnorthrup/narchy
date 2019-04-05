package nars.op.stm;

import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.data.set.MetalLongSet;
import jcog.math.FloatRange;
import jcog.math.LongInterval;
import jcog.pri.Prioritizable;
import jcog.pri.Prioritized;
import jcog.util.ArrayUtils;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.BagClustering;
import nars.control.Causable;
import nars.control.CauseMerge;
import nars.control.channel.CauseChannel;
import nars.task.NALTask;
import nars.task.UnevaluatedTask;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.term.TermedDelegate;
import nars.term.util.conj.ConjLazy;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.dynamic.TaskList;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static nars.truth.func.TruthFunctions.c2wSafe;
import static nars.truth.func.TruthFunctions.confCompose;

public class ConjClustering extends Causable {

    public final BagClustering<Task> data;
    //final CentroidConjoiner conjoiner = new CentroidConjoiner();
    private final BagClustering.Dimensionalize<Task> model;
    private final CauseChannel in;
    private final byte punc;

    public final FloatRange termVolumeMaxPct = new FloatRange(0.75f, 0, 1f);

    /** collect at most Neach results from each queue */
    int tasksPerIterationPerCentroid = 1;

    private final Predicate<Task> filter;
    private long now;
    private int dur;
    private float confMin;
    private int volMax;

    private final boolean popConjoinedTasks = false;
    static final boolean priCopyOrMove = true;

    final AtomicBoolean busy = new AtomicBoolean(false);
    private int inputTermVolMax, stampLenMax;

    private volatile long lastLearn;

    private int learningIterations = 1;
    private int minDurationsPerLearning = 1;


    public ConjClustering(NAR nar, byte punc, int centroids, int capacity) {
        this(nar, punc, (t) -> true, centroids, capacity);
    }

    public ConjClustering(NAR nar, byte punc, Predicate<Task> filter, int centroids, int capacity) {
        super();

        this.in = nar.newChannel(this);

        this.model = new BagClustering.Dimensionalize<>(4) {



            @Override
            public void coord(Task t, double[] c) {
                Truth tt = t.truth();
                c[0] = t.mid();

                c[1] = tt.polarity();
                c[2] = tt.conf();

                c[3] = t.range();
            }

            @Override
            public double distanceSq(double[] a, double[] b) {

                double dPolarity = Math.abs(a[1] - b[1]);
                double dConf = Math.abs(a[2] - b[2]);

                double rangeMin = Math.min(a[3], b[3]);
                double dRange = Math.abs(a[3] - b[3])/rangeMin;
                double dMid = Math.abs(a[0] - b[0])/rangeMin;
                return (1 + dMid)
                        *
                       (1 + dRange)
                        *
                       (1 + (dConf + dPolarity))
                       ;

//                return (1 + (Math.abs(a[0] - b[0]) / Math.min(a[4], b[4])) + (Math.abs(a[4] - b[4]) / dur))
//                        *
//                        (
//                                Math.abs(a[1] - b[1])
//                                        + Math.abs(a[2] - b[2])
//                                        + Math.abs(a[3] - b[3]) * 0.1f
//                        );
            }
        };

        this.data = new BagClustering<>(model, centroids, capacity);

        this.punc = punc;
        this.filter = filter;

        this.now = lastLearn = nar.time();
        update(nar);

        nar.start(this);
    }

    @Override
    protected void starting(NAR nar) {

        super.starting(nar);


        on(nar.onTask(t -> {
            if (!t.isEternal()
                    && !t.hasVars() //<-- TODO requires multi-normalization (shifting offsets)
                    && (stampLenMax == Integer.MAX_VALUE || (t.stamp().length <= stampLenMax))
                    && t.volume() <= inputTermVolMax
                    && filter.test(t)) {

                data.put(t, pri(t));

            }
        }, punc));

    }


    protected float pri(Task t) {
        return    (t.priElseZero())
                * (t.conf())
                * (0.5f + 0.5f * t.polarity())
                * (0.5f + 0.5f * t.originality())

//                 * (1/(1f+t.volume()))
                //* TruthIntegration.evi(t);
                ;
    }


    @Override
    public /*synchronized*/ void next(NAR nar, BooleanSupplier kontinue /* max tasks generated per centroid, >=1 */) {

        update(nar);

        //round-robin visit each centroid one task at a time.  dont finish a centroid completely and then test kontinue, it is unfair
        FasterList<TaskList> centroids = new FasterList<>(this.data.net.centroidCount());
        data.forEachCentroid(TaskList::new, tt ->{
            int tts = tt.size();
            if (tts > 1) {
                if (tts > 2)
                    ArrayUtils.sort(tt.array(), 0, tts-1, Prioritized::priElseZero);

                centroids.add(tt);
            }
        });

        int cc = centroids.size();
        if (cc == 0)
            return;
        if (cc > 1)
            centroids.shuffleThis(nar.random());

        CentroidConjoiner conjoiner = new CentroidConjoiner();
        do {

            Iterator<TaskList> ii = centroids.iterator();
            while (ii.hasNext()) {
                FasterList<Task> l = ii.next();
                if (conjoiner.conjoinCentroid(l, tasksPerIterationPerCentroid, nar) == 0 || l.size()<=1)
                    ii.remove();

                 if (!kontinue.getAsBoolean())
                     return;
            }

        } while (!centroids.isEmpty());

    }

    private void update(NAR nar) {
        long now = nar.time();
        long lastNow = this.now;
        if (this.nar==null || lastNow < now) {
            //parameters must be set even if data is empty due to continued use in the filter
            //but at most once per cycle or duration
            this.now = now;
            this.nar = nar;
            this.dur = nar.dur();
            this.stampLenMax =
                    //Param.STAMP_CAPACITY / 2; //for minimum of 2 tasks in each conjunction
                    //Param.STAMP_CAPACITY - 1;
                    Integer.MAX_VALUE;
            this.confMin = nar.confMin.floatValue();
            this.inputTermVolMax = Math.round(Math.max(1f,
                    (this.volMax = nar.termVolumeMax.intValue()) * termVolumeMaxPct.floatValue()) +
                    -2 /* for the super-CONJ itself and another term of at least volume 1 */
            );

        }

        if (busy.compareAndSet(false, true)) {
            try {
                if (now - lastLearn >= minDurationsPerLearning*dur) {
                    data.learn(forgetRate(), learningIterations);
                    lastLearn = now;
                }
            } finally {
                busy.lazySet(false);
            }
        }
    }

    protected float forgetRate() {
        return nar.attn.decay.floatValue();
        //return 1f;
        //return 0.9f;
        //return 0.75f;
        //return 0.5f;
    }

    @Override
    public float value() {
        return in.value();
    }

    public static class STMClusterTask extends UnevaluatedTask {

        STMClusterTask(@Nullable ObjectBooleanPair<Term> cp, Truth t, long start, long end, long[] evidence, byte punc, long now) throws TaskException {
            super(cp.getOne(), punc, t.negIf(cp.getTwo()), now, start, end, evidence);
        }

        @Override
        public boolean isInput() {
            return false;
        }
    }

    private final class CentroidConjoiner {

//        private final Map<LongObjectPair<Term>, Task> vv = new UnifiedMap<>(16);
        final List<Task> trying = new FasterList(8);
        final FasterList<Task> tried = new FasterList(8);

        final MetalLongSet actualStamp = new MetalLongSet(Param.STAMP_CAPACITY * 2);

        private int conjoinCentroid(FasterList<Task> items, int limit, NAR nar) {
            int s = items.size();
            if (s == 0)
                return 0;


            int count = 0;
            float confMinThresh = confMin + nar.confResolution.floatValue()/2f;

            boolean active = true, reset = true;

            tried.clear();
            trying.clear();
            actualStamp.clear();

//            System.out.println(items.size());

            ListIterator<Task> i = ArrayUtils.cycle(items);
            int volEstimate = 1;
            float freq = 1, conf = 1;
            long start = Long.MAX_VALUE;
            main: for (; i.hasNext(); ) {


                if (--s < 0)
                    reset = true;

                if (reset) {
//                    System.out.println("\t" + items.size());

                    if (!active)
                        break;

                    s = items.size();

                    int ts = tried.size();
                    if (ts > 0) {
                        items.addAll(tried);
                        tried.clear();
                    }
                    if (s + ts <= 1)
                        break; //there would be no others remaining

                    reset = false;
                    active = false;

                    freq = conf = 1f;
                    start = Long.MAX_VALUE;
                    volEstimate = 1;

                    actualStamp.clear();
                    trying.clear();
                }


                Task t = i.next();

                Truth tx = t.truth();
                float tc = tx.conf();
                float nextConf = confCompose(conf, tc);
                if (nextConf < confMin)
                    continue; //try next

                Term term = t.term();
                boolean taskNeg = tx.isNegative();
                if (taskNeg)
                    term = term.neg();

                int xtv = term.volume();

                if (volEstimate + xtv <= volMax) {
                    long[] tStamp = t.stamp();

                    if (!Stamp.overlapsAny(actualStamp, tStamp)) {

                        long tStart = t.start();
                        //                        if (vv.isEmpty() || !vv.containsKey(pair(tStart, term.neg()))) {
//
//                            if (null == vv.putIfAbsent(pair(tStart, term), t)) {


                        volEstimate += xtv;

                        actualStamp.addAll(tStamp);

                        if (start > tStart) start = tStart;


                        conf = nextConf;

                        float tf = tx.freq();
                        freq *= taskNeg ? (1f - tf) : tf;

                        trying.add(t);
                        i.remove();

                        if (trying.size() > 1) {

                            if (items.isEmpty() || (conf <= confMinThresh) || (volEstimate  >= volMax) || (trying.size() >= Param.STAMP_CAPACITY)) {
                                Task[] x = trying.toArray(Task.EmptyArray);
                                trying.clear();

                                //TEMPORARY
                                for (Task c : x) if (c == null) throw new WTF();

                                Task y = conjoin(x, freq, conf, start);
                                boolean conjoined = y != null;
                                active = true;


                                if (conjoined) {


                                    s -= x.length;

                                    if (popConjoinedTasks) {
                                        for (Task aa : x)
                                            data.remove(aa);
                                    }

                                    in.input(y);

                                    if (++count >= limit)
                                        break main;

                                } else {

                                    //recycle to be reused
                                    tried.addAll(x);
                                }

                            }
                        }
                    }
                }

            }

            if (!tried.isEmpty()) {
                items.addAll(tried);
                tried.clear();
            }

            return count;
        }

        private Task conjoin(Task[] x, float freq, float conf, long start) {

            float e = c2wSafe(conf);
//            if (e != e)
//                throw new WTF();

            if (e > 0) {
                final Truth t = Truth.theDithered(freq, e, nar);
                if (t != null) {

                    Term cj = ConjLazy.sequence(x);
                    if (cj.volume() > 1) {

                        Term tt = Task.normalize(cj);

                        ObjectBooleanPair<Term> cp = Task.tryContent(tt, punc, true);
                        if (cp != null) {

                            long range = Util.min(LongInterval::range, x) - 1;
                            long tEnd = start + range;
                            NALTask y = new STMClusterTask(cp, t,
                                    start, tEnd,
                                    Stamp.sample(Param.STAMP_CAPACITY, actualStamp, nar.random()), punc, now);
                            y.cause(CauseMerge.AppendUnique.merge(Param.causeCapacity.intValue(), x));



                            int xVolMax = Util.max(TermedDelegate::volume, x);
                            float cmplFactor =
                                    ((float)xVolMax) / y.volume();

//                                float freqFactor =
//                                        t.freq();
//                                float confFactor =
//                                        (conf / (conf + confMax));

                            float p = Util.max(Task::priElseZero, x) * cmplFactor;

                            y.pri(
                                Prioritizable.fund(p, priCopyOrMove,
                                x)
                            );

                            return y;

                        }
                    }
                }


            }


            return null;
        }
    }
}