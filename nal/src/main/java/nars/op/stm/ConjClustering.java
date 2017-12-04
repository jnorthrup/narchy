package nars.op.stm;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import jcog.Util;
import jcog.list.FasterList;
import jcog.pri.VLink;
import nars.*;
import nars.bag.BagClustering;
import nars.control.Causable;
import nars.control.Cause;
import nars.control.CauseChannel;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.util.BudgetFunctions;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

public class ConjClustering extends Causable {
    private final CauseChannel<ITask> in;


    final static BagClustering.Dimensionalize<Task> ConjClusterModel = new BagClustering.Dimensionalize<>(3) {

        @Override
        public void coord(Task t, double[] c) {
            c[0] = t.start();
            Truth tt = t.truth();
            c[1] = tt.polarity(); //0..+1 //if negative, will be negated in subterms
            c[2] = tt.conf(); //0..+1
        }

        @Override
        public double distanceSq(double[] a, double[] b) {
            return Util.sqr(
                    Math.abs(a[0] - b[0])
            ) +    //time

                    (Math.abs(a[1] - b[1]))  //freq
                    + (Math.abs(a[2] - b[2])  //conf

            );

        }
    };
    private final BagClustering<Task> bag;
    private final byte punc;
    private final int maxConjSize;
    private long now;
    private float freqRes, confRes, confMin;
    private int volMax;
    private final static Logger logger = LoggerFactory.getLogger(ConjClustering.class);

    public ConjClustering(NAR nar, int maxConjSize, byte punc, boolean includeNeg, int centroids, int capacity) {
        super(nar);

        this.punc = punc;
        this.in = nar.newCauseChannel(this);
        this.bag = new BagClustering<>(ConjClusterModel, centroids, capacity);
        this.maxConjSize = maxConjSize;

        nar.onTask(t -> {
            if (!t.isEternal() && t.punc() == punc && (includeNeg || t.isPositive())) {
                bag.put(t,
                        t.priElseZero() * (1f / t.volume()) //prefer smaller events
                        //t.priElseZero()
                        //(1f + t.expolarity()) * (1f + t.conf())
                );
                //* (1f + t.priElseZero()));// / t.volume());
            }
        });
    }

    private int tasksCreated, taskLimitPerCentroid;


    @Override
    protected int next(NAR nar, int work) {


        now = nar.time();
        freqRes = nar.freqResolution.floatValue();
        confRes = nar.confResolution.floatValue();
        confMin = nar.confMin.floatValue();
        this.volMax = nar.termVolumeMax.intValue();

        taskLimitPerCentroid = work;
        tasksCreated = 0;

        bag.commitGroups(1, nar, this::conjoinCentroid);

        /* decrease cluster bag vlink priority by at least as much as the priority lost by the task during conjing
           helps ensures fair induction of new items and replacement of processed items (which can remain indefinitely)*/
        bag.bag.commit(t -> {
            float pp = t.id.pri();
            if (pp == pp)
                t.priMin(pp);
            else
                t.delete();
        });

        return tasksCreated;
    }

//    /**
//     * produces a parallel conjunction term consisting of all the task's terms
//     */
//    public Stream<List<Task>> chunk(Stream<Task> input, int maxComponentsPerTerm, int maxVolume) {
//        final int[] group = {0};
//        final int[] subterms = {0};
//        final int[] currentVolume = {0};
//        final float[] currentConf = {1};
//        return input.filter(x -> !x.isDeleted())
//                .collect(Collectors.groupingBy(x -> {
//
//                    int v = x.volume();
//                    float c = x.conf();
//
//                    if ((subterms[0] >= maxComponentsPerTerm) || (currentVolume[0] + v >= maxVolume) || (currentConf[0] * c < confMin)) {
//                        //next group
//                        group[0]++;
//                        subterms[0] = 1;
//                        currentVolume[0] = v;
//                        currentConf[0] = c;
//                    } else {
//                        subterms[0]++;
//                        currentVolume[0] += v;
//                        currentConf[0] *= c;
//                    }
//
//                    return group[0];
//                }))
//                .entrySet().stream()
//                .map(c -> {
//                    List<Task> v = c.getValue();
//                    return c.getKey() >= 0 && //only batches of >1
//                            v.size() > 1 ? v : null;  //ignore the -1 discard group
//                })
//                .filter(Objects::nonNull);
//
//    }

//    static final BiFunction<Task, Task, Task> termPointMerger = (prevZ, newZ) -> ((prevZ == null) || (newZ.conf() >= prevZ.conf())) ?
//            newZ : prevZ;

    private void conjoinCentroid(Stream<VLink<Task>> group, NAR nar) {

        //get only the maximum confidence task for each term at its given starting time

        //in.input(
        //chunk(group.filter(Objects::nonNull).takeWhile(kontinue)
        //.map(x -> x.id), maxConjSize, volMax).takeWhile(kontinue).map(tasks -> {

        List<ITask> gen = new FasterList();

        Iterator<VLink<Task>> gg =
                group.filter(x -> x != null && !x.isDeleted()).iterator();
                //Iterators.peekingIterator();


        Map<LongObjectPair<Term>, Task> vv = new HashMap<>();
        FasterList<Task> actualTasks = new FasterList();

        main: while (gen.size() < taskLimitPerCentroid) {

            vv.clear();
            actualTasks.clear();


            long end = Long.MIN_VALUE;
            long start = Long.MAX_VALUE;


            float freq = 1f;
            float conf = 1f;
            float priMax = Float.NEGATIVE_INFINITY, priMin = Float.POSITIVE_INFINITY;
            int vol = 0;

            do {
                if (!gg.hasNext())
                    break main;

                Task t =
                        gg.next().id;
                        //gg.peek().id;
                Term xt = t.term();

                long zs = t.start();
                long ze = t.end();
                if (start > zs) start = zs;
                if (end < ze) end = ze;
                assert (end >= start);

                Truth tx = t.truth();
                Term xtn = xt.neg();
                if (tx.isNegative()) {
                    xt = xtn;
                }

                int xtv = xt.volume();
                if (vol + xtv + 1 >= volMax || conf * tx.conf() < confMin) {
                    continue; //cant go any further with this task
                }

                boolean involved = false;
                LongObjectPair<Term> ps = pair(zs, xt);
                if (!vv.containsKey(pair(zs, xt.neg())) && null == vv.putIfAbsent(ps, t)) {
                    vol += xtv;
                    involved = true;
                }

                if (ze != zs) {
                    LongObjectPair<Term> pe = pair(ze, xt);
                    if (!vv.containsKey(pair(ze, xt.neg())) && null == vv.putIfAbsent(pe, t)) { //end point, if different from start
                        vol += xtv;
                        involved = true;
                    }
                }

                if (involved) {

                    actualTasks.add(t);

                    conf *= tx.conf();

                    float tf = tx.freq();
                    freq *= tx.isNegative() ? (1f - tf) : tf; //since it will appear as a negated subterm

                    float p = t.priElseZero();
                    if (p < priMin) priMin = p;
                    if (p > priMax) priMax = p;

                }
            } while (vol < volMax - 1 && conf > confMin);

            int vs = actualTasks.size();
            if (vs < 2)
                continue;

            //the tasks which are actually involved


            Task[] uu = actualTasks.toArrayRecycled(Task[]::new);

            //TODO discount based on evidential overlap? needs N-way overlapFraction function

            PreciseTruth t = $.t(freq, conf).dither(freqRes, confRes, confMin, 1f);
            if (t != null) {

                Term cj = Op.conj(new FasterList(vv.keySet())).normalize();
                ObjectBooleanPair<Term> cp = Task.tryContent(cj, punc, true);
                if (cp != null) {

                    long[] evidence = Stamp.zip(uu, Param.STAMP_CAPACITY);
                    NALTask m = new STMClusterTask(cp, t, start, end, evidence, punc, now); //TODO use a truth calculated specific to this fixed-size batch, not all the tasks combined

                    m.cause = Cause.zip(nar.causeCapacity.intValue(), uu);

                    float pri =
                            //priMax;
                            //priMin;
                            (priMin + priMax) / 2f;

                    m.priSet(BudgetFunctions.fund(pri, true, uu));
                    tasksCreated++;
                    gen.add(m);
                } else {
                    //Task.tryContent(cj, punc, true);
                    //logger.warn("{} failed", this);
                }

            }


        }

        if (!gen.isEmpty()) {
//            System.out.println(gen);
            in.input(gen);
        }

    }

    @Override
    public float value() {
        return in.value();
    }


//        Map<Term, Task> vv = new HashMap();
//        net.nodeStream()
//                .filter(x -> x.size() >= minConjSize)
//                .sorted(Comparator.comparingDouble(x -> x.localError() / (x.size())))
//                .filter(n -> {
//                    //TODO wrap all the coherence tests in one function call which the node can handle in a synchronized way because the results could change in between each of the sub-tests:
//
//
//                    double[] fc = n.coherence(2);
//                    if (fc != null && fc[1] >= freqCoherenceThresh) {
//                        double[] cc = n.coherence(3);
//                        if (cc != null && cc[1] >= confCoherenceThresh) {
//                            return true;
//                        }
//                        //return true;
//                    }
//
//                    return false;
//                })
//                .flatMap(n -> {
//

//
//                    //if temporal clustering is close enough, allow up to maxGroupSize in &&, otherwise lmiit to 2
//                    int gSize = ((n.range(0) <= dur && n.range(1) <= dur)) ? maxConjSize : 2;
//
//                    return n.chunk(gSize, maxVol).


    public static class STMClusterTask extends NALTask {

        public STMClusterTask(@Nullable ObjectBooleanPair<Term> cp, PreciseTruth t, long start, long end, long[] evidence, byte punc, long now) throws InvalidTaskException {
            super(cp.getOne(), punc, t, now, start, end, evidence);
        }

        @Override
        public boolean isInput() {
            return false;
        }
    }


}
