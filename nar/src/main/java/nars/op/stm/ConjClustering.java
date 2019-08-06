package nars.op.stm;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.set.MetalLongSet;
import jcog.event.Off;
import jcog.math.FloatRange;
import jcog.math.LongInterval;
import jcog.pri.Prioritized;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.What;
import nars.bag.BagClustering;
import nars.control.How;
import nars.control.PartBag;
import nars.control.channel.CauseChannel;
import nars.task.NALTask;
import nars.task.TemporalTask;
import nars.task.UnevaluatedTask;
import nars.task.util.TaskException;
import nars.task.util.TaskList;
import nars.term.Term;
import nars.term.util.conj.ConjSeq;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.truth.func.TruthFunctions.c2wSafe;
import static nars.truth.func.TruthFunctions.confCompose;

public class ConjClustering extends How implements Consumer<Task> {

    public final BagClustering<Task> data;
    private final BagClustering.Dimensionalize<Task> model;
    private final CauseChannel<Task> in;
    private final byte puncIn, puncOut;

    public final FloatRange termVolumeMaxPct = new FloatRange(1f, 0, 1f);
    public final FloatRange forgetRate = new FloatRange(1f, 0, 1);

    private int inputTermVolMax, stampLenMax;

    /** collect at most Neach results from each queue */
    int tasksPerIterationPerCentroid = 1;
    int learningIterations = 2;
    int minDurationsPerLearning = 1;

    private final Predicate<Task> filter;

    private float confMin;
    private int volMax;

    private final boolean popConjoinedTasks = false;
    static final boolean priCopyOrMove = true;

    private final AtomicBoolean busy = new AtomicBoolean(false);
    private volatile long now;
    private volatile long lastLearn;





    /** default that configures with belief/goal -> question/quest output mode */
    public ConjClustering(NAR nar, byte punc, int centroids, int capacity) {
        this(nar, punc, centroids, capacity, t -> true);
    }
    public ConjClustering(NAR nar, byte puncIn, byte puncOut, int centroids, int capacity) {
        this(nar, puncIn, puncOut, centroids, capacity, t -> true);
    }

    /** default that configures with belief/goal -> question/quest output mode */
    public ConjClustering(NAR nar, byte punc, int centroids, int capacity, Predicate<Task> filter) {
        this(nar, punc, punc == BELIEF ? QUESTION : QUEST, centroids, capacity, filter);
    }

    public ConjClustering(NAR nar, byte puncIn, byte puncOut, int centroids, int capacity, Predicate<Task> filter) {
        super();

        this.in = nar.newChannel(this);

        this.puncIn = puncIn;
        this.puncOut = puncOut;

        this.filter = filter;
        /** TODO make this a constructor parameter */
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



    }

    @Override
    protected void starting(NAR nar) {

        super.starting(nar);

        _update(nar.time());
    }


    protected float pri(Task t) {
        return    (t.pri())
                * (t.conf())
                * (0.5f + 0.5f * t.polarity())
                * (0.5f + 0.5f * t.originality())

//                 * (1/(1f+t.volume()))
                //* TruthIntegration.evi(t);
                ;
    }


    //final WeakIdentityMap<What,What> whats = WeakIdentityMap.newConcurrentHashMap();
    final PartBag<What> whats = new PartBag<>(16) {
        //TODO store off() somewhere so it can be called when an entry is evicted. maybe use MetaMap for What like Concept

        @Override public void onAdd(What w) {
            super.onAdd(w);
            synchronized (this) {
                Off off = w.onTask(ConjClustering.this, puncIn);
            }
        }
    };

    @Override public /*synchronized*/ void next(What w, BooleanSupplier kontinue /* max tasks generated per centroid, >=1 */) {

        //attempt register
        whats.putAsync(w);

        if (busy.compareAndSet(false, true)) {
            float dur = w.dur();
            try {
                long now = nar.time();
                long before = this.now;
                if (before < now) {
                    _update(now);
                }

                if (now - lastLearn >= minDurationsPerLearning*dur) {
                    data.learn(forgetRate(), learningIterations);
                    lastLearn = now;
                }
            } finally {
                busy.set(false);
            }
        }

        CentroidConjoiner conjoiner = this.conjoiners.get();

        //round-robin visit each centroid one task at a time.  dont finish a centroid completely and then test kontinue, it is unfair
        FasterList<TaskList> centroids = conjoiner.centroids;
        centroids.clear();
        data.forEachCentroid(TaskList::new, tt ->{
            int tts = tt.size();
            if (tts > 1) {
                if (tts > 2) {
                    ArrayUtil.sort(tt.array(), 0, tts - 1, Prioritized::priElseZero);
                    //tt.sortThis(centroidContentsSort); //java.lang.IllegalArgumentException: Comparison method violates its general contract!
                }

                centroids.add(tt);
            }
        });

        int cc = centroids.size();
        if (cc == 0)
            return;
        if (cc > 1)
            centroids.shuffleThis(w.nar.random());

        //round robin
        do {

            centroids.removeIf((Predicate<TaskList>)(i ->
                conjoiner.conjoinCentroid(tasksPerIterationPerCentroid, i, w) == 0 || i.size() <= 1));

        } while (!centroids.isEmpty() && kontinue.getAsBoolean());


        //in.acceptAll(conjoiner.out, w);

    }

    static final Comparator<Task> centroidContentsSort = Comparators
            .byFloatFunction(Task::priElseZero)
            .thenComparingInt(Task::volume)
            .reversed();
            //.thenComparingFloat(Task::originality);

        @Override
    public boolean singleton() {
        //TODO make NeuralGasNet synchronization free then this will be good to set false
        //return true;
        return false;
    }

    private void _update(long now) {
        //parameters must be set even if data is empty due to continued use in the filter
        //but at most once per cycle or duration
        this.now = now;
        this.stampLenMax =
                NAL.STAMP_CAPACITY / 2; //for minimum of 2 tasks in each conjunction
        //Param.STAMP_CAPACITY - 1;
        //Integer.MAX_VALUE;
        this.confMin = nar.confMin.floatValue();
        this.volMax = Math.max(3,Math.round(
                (nar.termVolMax.intValue() * termVolumeMaxPct.floatValue())
                //-2 /* for the super-CONJ itself and another term of at least volume 1 */
        ));
        this.inputTermVolMax = Math.max(1, volMax - 2);
    }

    protected float forgetRate() {
        return forgetRate.asFloat();
        //return 1f;
        //return 0.9f;
        //return 0.75f;
        //return 0.5f;
    }

    @Override
    public float value() {
        return in.value();
    }

    @Override public void accept(Task t) {
        if (
            !t.isEternal()
            && !t.hasVars() //<-- TODO requires multi-normalization (shifting offsets)
            && (stampLenMax == Integer.MAX_VALUE || (t.stamp().length <= stampLenMax))
            && t.volume() <= inputTermVolMax
            && filter.test(t)) {

            data.put(t, pri(t));

        }
    }

    public static final class STMClusterTask extends TemporalTask implements UnevaluatedTask {

        STMClusterTask(@Nullable ObjectBooleanPair<Term> cp, Truth t, long start, long end, long[] evidence, byte punc, long now) throws TaskException {
            super(cp.getOne(), punc, t!=null ? t.negIf(cp.getTwo()) : null, now, start, end, evidence);
        }

    }

    private final ThreadLocal<CentroidConjoiner> conjoiners = ThreadLocal.withInitial(CentroidConjoiner::new);

    private final class CentroidConjoiner {


//        private final Map<LongObjectPair<Term>, Task> vv = new UnifiedMap<>(16);
        final List<Task> trying = new FasterList();
        final FasterList<Task> tried = new FasterList();

        final MetalLongSet stamp = new MetalLongSet(NAL.STAMP_CAPACITY * 8);
        public FasterList<TaskList> centroids = new FasterList();

        /** generated tasks */
        //final FasterList<Task> out = new FasterList();
        private transient int tasksGeneratedPerCentroidIterationMax;

        private CentroidConjoiner() {

        }

        private int conjoinCentroid(int limit, FasterList<Task> in, What w) {

            int s = in.size();
            if (s == 0)
                return 0;


            this.tasksGeneratedPerCentroidIterationMax = limit;

            int count = 0;
            float confMinThresh = confMin + nar.confResolution.floatValue()/2f;

            boolean active = true, reset = true;

            tried.clear();
            trying.clear();
            stamp.clear();

//            System.out.println(items.size());

            ListIterator<Task> i = ArrayUtil.cycle(in);
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

                    s = in.size();

                    int ts = tried.size();
                    if (ts > 0) {
                        in.addAll(tried);
                        tried.clear();
                    }
                    if (s + ts <= 1)
                        break; //there would be no others remaining

                    reset = false;
                    active = false;

                    freq = conf = 1f;
                    start = Long.MAX_VALUE;
                    volEstimate = 1;

                    stamp.clear();
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

                    if (!Stamp.overlapsAny(stamp, tStamp)) {

                        long tStart = t.start();
                        //                        if (vv.isEmpty() || !vv.containsKey(pair(tStart, term.neg()))) {
//
//                            if (null == vv.putIfAbsent(pair(tStart, term), t)) {


                        volEstimate += (xtv+1);

                        stamp.addAll(tStamp);

                        if (start > tStart) start = tStart;


                        conf = nextConf;

                        float tf = tx.freq();
                        freq *= taskNeg ? (1f - tf) : tf;

                        trying.add(t);
                        i.remove();

                        if (trying.size() > 1) {

                            if (in.isEmpty() || (conf <= confMinThresh) || (volEstimate  >= volMax) || (trying.size() >= NAL.STAMP_CAPACITY)) {
                                Task[] x = trying.toArray(Task.EmptyArray);
                                trying.clear();

                                active = true;

                                Task y = conjoin(x, freq, conf, start);

                                if (y != null) {


                                    s -= x.length;

                                    if (popConjoinedTasks) {
                                        for (Task aa : x)
                                            data.remove(aa);
                                    }

                                    ConjClustering.this.in.accept(y, w);

                                    if (++count >= tasksGeneratedPerCentroidIterationMax)
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
                in.addAll(tried);
                tried.clear();
            }

            return count;
        }

        private Task conjoin(Task[] x, float freq, float conf, long start) {


            Truth t;
            if (puncOut==BELIEF || puncOut == GOAL) {
                double e = c2wSafe((double) conf);
                if (e < NAL.truth.EVI_MIN)
                    return null;

                t = Truth.theDithered(freq, e, nar);
                if (t == null)
                    return null;
            } else {
                t = null;
            }


            int ditherDT = nar.dtDither.intValue();
            Term cj = ConjSeq.sequence(x, ditherDT, Op.terms);
            if (cj.op() != CONJ)
                return null;

            ObjectBooleanPair<Term> cp = Task.tryTaskTerm(cj, puncOut, true);
            if (cp == null)
                return null;

            long range = Util.min(LongInterval::range, x) - 1;
            long tEnd = start + range;
            long[] se = Tense.dither(new long[] { start, tEnd}, nar);
            NALTask y = new STMClusterTask(cp, t,
                    se[0], se[1],
                    Stamp.sample(NAL.STAMP_CAPACITY, stamp, nar.random()), puncOut, now);

            Task.fund(y, x, priCopyOrMove);

            return y;


        }
    }

}