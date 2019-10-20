package nars.op.stm;

import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.bag.BagClustering;
import nars.derive.Derivation;
import nars.derive.action.TaskAction;
import nars.derive.rule.RuleCause;
import nars.task.AbstractTask;
import nars.task.util.TaskList;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.dynamic.DynTaskify;
import nars.truth.dynamic.DynamicConjTruth;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.impl.block.factory.Comparators;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.truth.func.TruthFunctions.confCompose;

public class ConjClustering extends TaskAction {

    @Deprecated static final int ITERATIONS = 1; //temporary

    public final BagClustering<Task> data;

    public final FloatRange termVolumeMaxPct = new FloatRange(1f, (float) 0, 1f);
    public final FloatRange forgetRate = new FloatRange(1f, (float) 0, 1.0F);

    private int inputTermVolMax;
    private int stampLenMax;

    /** collect at most Neach results from each queue */
    int tasksPerIterationPerCentroid = 1;

    int eventBatchMin = 3;

    int learningIterations = 1;
    float minDurationsPerLearning = 1f;

    private final Predicate<Task> filter;

    private float confMin;
    private int volMax;


    private final AtomicBoolean busy = new AtomicBoolean(false);
    private volatile long lastLearn;
    @Deprecated private NAR nar;


    /** default that configures with belief/goal -> question/quest output mode */
    public ConjClustering(NAR nar, byte punc, int centroids, int capacity) {
        this(nar, punc, centroids, capacity, t -> true);
    }
    public ConjClustering(NAR nar, byte puncIn, byte puncOut, int centroids, int capacity) {
        this(nar, puncIn, puncOut, centroids, capacity, t -> true);
    }

    /** default that configures with belief/goal -> question/quest output mode */
    public ConjClustering(NAR nar, byte punc, int centroids, int capacity, Predicate<Task> filter) {
        this(nar, punc, (int) punc == (int) BELIEF ? QUESTION : QUEST, centroids, capacity, filter);
    }

    public ConjClustering(NAR nar, byte puncIn, byte puncOut, int centroids, int capacity, Predicate<Task> filter) {
        super();

        taskPunc(puncIn);
        //TODO other filters

        this.filter = filter;
        /** TODO make this a constructor parameter */
        //mid
        //range
        //dMid  (div by range to normalize against scale)
        //dRange
        //dConf
        //dPolarity
        //                return (1 + (Math.abs(a[0] - b[0]) / Math.min(a[4], b[4])) + (Math.abs(a[4] - b[4]) / dur))
        //                        *
        //                        (
        //                                Math.abs(a[1] - b[1])
        //                                        + Math.abs(a[2] - b[2])
        //                                        + Math.abs(a[3] - b[3]) * 0.1f
        //                        );
        BagClustering.Dimensionalize<Task> model = new BagClustering.Dimensionalize<>(4) {

            @Override
            public void coord(Task t, double[] c) {
                Truth tt = t.truth();
                long s = t.start(), e = t.end();
                c[0] = (double) ((s + e) / 2L); //mid

                c[1] = (double) tt.polarity();
                c[2] = (double) tt.conf();

                c[3] = (double) (e - s); //range
            }

            @Override
            public double distanceSq(double[] a, double[] b) {

                double range = 1.0 + Math.max(a[3], b[3]);
                return (1.0 + Math.abs(a[0] - b[0]) / range) //dMid  (div by range to normalize against scale)
                        *
                        (1.0 + Math.abs(a[3] - b[3]) / range) //dRange
                        *
                        (1.0 + (
                                Math.abs(a[2] - b[2]) //dConf
                                        +
                                        Math.abs(a[1] - b[1]) //dPolarity
                        ) / 2.0)
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

        this.nar = nar;
        _update(nar.time());



    }

//    @Override
//    protected void starting(NAR nar) {
//
//        super.starting(nar);
//
//        _update(nar.time());
//    }


    protected static float pri(Task t) {
        return    (1.0F + 0.5f * t.priElseZero())
                * (1.0F + 0.5f * t.conf())
                * (1.0F + 0.5f * t.polarity())
                * (1.0F + 0.5f * t.originality())

//                 * (1/(1f+t.volume()))
                //* TruthIntegration.evi(t);
                ;
    }


//    //final WeakIdentityMap<What,What> whats = WeakIdentityMap.newConcurrentHashMap();
//    final PartBag<What> whats = new PartBag<>(16) {
//        //TODO store off() somewhere so it can be called when an entry is evicted. maybe use MetaMap for What like Concept
//
//        @Override public void onAdd(What w) {
//            super.onAdd(w);
//            synchronized (this) {
//                Off off = w.onTask(ConjClustering.this, puncIn);
//            }
//        }
//    };


    @Override
    protected void accept(Task x, RuleCause why, Derivation d) {

        NAR nar = d.nar;

        if (!filter(x)) return;

        if (!data.put(x, pri(x)))
            return;

        tryLearn(d);

        CentroidConjoiner conjoiner = this.conjoiners.get();

        //round-robin visit each centroid one task at a time.  dont finish a centroid completely and then test kontinue, it is unfair

        int iterations = ITERATIONS;
        conjoiner.centroids = data.forEachCentroid(conjoiner.centroids, iterations, tasksPerIterationPerCentroid * eventBatchMin, TaskList::new    /* TODO by volume, ie volMax */);
        TaskList[] centroids = conjoiner.centroids;
        int N = centroids.length;

        //HACK this should be computed in forEachCentroid
        int nonEmpty = 0;
        for (int i = 0, centroidsLength = N; i < centroidsLength; i++) {
            TaskList tt = centroids[i];
            int tts = tt.size();
            switch (tts) {
                case 0: break;
                case 1: tt.clear(); break;
                default: nonEmpty++; break;
            }
        }

        if (nonEmpty == 0) return;

        //TODO sort sub-buffer if some entries are empty they can be avoided during iteration




        //round robin
        int next = 0; //start with first centroid in the array (which has been sorted already)
        int empty = 0;
        do {

            TaskList i = centroids[next];

            int ii = i.size();
            if (ii > 1) {


                if (conjoiner.conjoinCentroid(tasksPerIterationPerCentroid, i, why, d) == 0) {
                    if (i.size() > 2)
                        i.shuffleThis(d.random); //try a new order
                    else {
                        i.clear(); //doomed
                        empty++;
                    }
                }
            } else {
                empty++;
            }

            if (++next == N) next = 0;

        } while (empty < N && --iterations > 0);

        //in.acceptAll(conjoiner.out, w);
    }

    public void tryLearn(Derivation d) {
        if (busy.compareAndSet(false, true)) {
            try {
                //called by only one thread at a time:
                NAR nar = d.nar;
                //TODO adjust update rate according to value
                if ((float) (d.time - lastLearn) >= minDurationsPerLearning * d.dur) {
                    _update(d.time);

                    data.learn(forgetRate(), learningIterations);
                    lastLearn = d.time;
                }
            } finally {
                busy.set(false);
            }
        }
    }

    /** experimental */
    static final Comparator<Task> centroidContentsSort = Comparators
            .byFloatFunction(Task::priElseZero)
            .thenComparingInt(Task::volume)
            .reversed();
            //.thenComparingFloat(Task::originality);


    /** should be safe to be called from multiple threads */
    private void _update(long now) {
        //parameters must be set even if data is empty due to continued use in the filter
        //but at most once per cycle or duration
        this.stampLenMax =
                NAL.STAMP_CAPACITY / 2; //for minimum of 2 tasks in each conjunction
        //Param.STAMP_CAPACITY - 1;
        //Integer.MAX_VALUE;
        this.confMin = nar.confMin.floatValue();
        this.volMax = Math.max(3,Math.round(
                ((float) nar.termVolMax.intValue() * termVolumeMaxPct.floatValue())
                //-2 /* for the super-CONJ itself and another term of at least volume 1 */
        ));
        this.inputTermVolMax = Math.max(1, volMax - 2);
    }

    protected final float forgetRate() {
        return forgetRate.asFloat();
    }

    @Override
    public final float pri(Derivation d) {
        //prefilter
        Task t = d._task;
        if (t.isEternal())
            return (float) 0;

        return 1.0F;
        //return pri(d._task);

//
//
//        return 0.1f * (pri(t) + in.pri());
    }


    public boolean filter(Task t) {
        return !t.isEternal()
            && !t.hasVars() //<-- TODO requires multi-normalization (shifting offsets)
            && t.volume() <= inputTermVolMax
            && (stampLenMax == Integer.MAX_VALUE || (t.stamp().length <= stampLenMax))
            && filter.test(t);
    }

//    public static final class STMClusterTask extends TemporalTask implements UnevaluatedTask {
//
//        STMClusterTask(@Nullable ObjectBooleanPair<Term> cp, Truth t, long start, long end, long[] evidence, byte punc, long now) throws TaskException {
//            super(cp.getOne(), punc, t!=null ? t.negIf(cp.getTwo()) : null, now, start, end, evidence);
//        }
//
//    }

    private final ThreadLocal<CentroidConjoiner> conjoiners = ThreadLocal.withInitial(CentroidConjoiner::new);

    private final class CentroidConjoiner {

        final List<Task> trying = new FasterList();
        final IntToObjectFunction<Task> tryer = trying::get;
        final FasterList<Task> tried = new FasterList();

        /** centroid buffer */
        public transient TaskList[] centroids = new TaskList[0];


        private CentroidConjoiner() {

        }

        private int conjoinCentroid(int limit, FasterList<Task> in, RuleCause why, Derivation d) {

            int s = in.size();
            if (s < 2)
                return 0;


            float confMinThresh = confMin + d.nar.confResolution.floatValue()/2f;

            /** only an estimate to determine when threshold is being approached;
             * the actual conf will be computed more exactly but it wont exceed this */

            tried.clear();
            trying.clear();

//            System.out.println(items.size());

            ListIterator<Task> i = ArrayUtil.cycle(in);
            int volEstimate = 1;
            double conf = 1.0;
            boolean reset = true;
            boolean active = true;
            int count = 0;
            main: for (long start = Long.MAX_VALUE; i.hasNext(); ) {


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

                    conf = 1;
                    start = Long.MAX_VALUE;
                    volEstimate = 1;

                    trying.clear();
                }


                Task t = i.next();

                Truth tx = t.truth();
                float tc = tx.conf();
                float nextConf = confCompose(conf, (double) tc);
                if (nextConf < confMin)
                    continue; //try next

                Term term = t.term();
                boolean taskNeg = tx.isNegative();
                if (taskNeg)
                    term = term.neg();

                int xtv = term.volume();

                if (volEstimate + xtv <= volMax) {

                    if (!Stamp.overlap(t, 0, trying.size(), tryer)) {

                        long tStart = t.start();
                        //                        if (vv.isEmpty() || !vv.containsKey(pair(tStart, term.neg()))) {
//
//                            if (null == vv.putIfAbsent(pair(tStart, term), t)) {


                        /** HACK */
                        int volEstimateInflationFactor = 2;
                        volEstimate += ((xtv+1) * volEstimateInflationFactor);

                        if (start > tStart) start = tStart;


                        conf = (double) nextConf;

                        //float tf = tx.freq();

                        trying.add(t);
                        i.remove();

                        if (trying.size() > 1) {

                            if (in.isEmpty() || (conf <= (double) confMinThresh) || (volEstimate  >= volMax) || (trying.size() >= NAL.STAMP_CAPACITY)) {
                                Task[] x = trying.toArray(Task.EmptyArray);
                                trying.clear();

                                active = true;

                                Task y = conjoin(x, start);

                                if (y != null) {


                                    s -= x.length;

                                    boolean popConjoinedTasks = false;
                                    if (popConjoinedTasks) {
                                        for (Task aa : x)
                                            data.remove(aa);
                                    }

                                    d.remember( ((AbstractTask)y).why(why.why) );

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
                in.addAll(tried);
                tried.clear();
            }

            return count;
        }

        private Task conjoin(Task[] x, long start) {

            DynTaskify d = new DynTaskify(DynamicConjTruth.ConjIntersection, x[0].isBelief() /* else goal */,
                false, null, (float) 0, null, nar
            );
            MetalBitSet cp = d.componentPolarity;
            for (int i = 0, xLength = x.length; i < xLength; i++) {
                if (x[i].isNegative())
                    cp.clear(i);
            }

            d.addAll(x);

            return d.task();
        }
    }

}