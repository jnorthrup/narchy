package nars.op.stm;

import jcog.list.FasterList;
import jcog.pri.Priority;
import jcog.pri.VLink;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.bag.BagClustering;
import nars.control.Cause;
import nars.control.CauseChannel;
import nars.exe.Causable;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.DiscreteTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.truth.TruthFunctions.c2wSafe;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

public class ConjClustering extends Causable {

    private final CauseChannel<ITask> in;

    final BagClustering.Dimensionalize<Task> ConjClusterModel;
    protected final BagClustering<Task> bag;
    private final byte punc;
    private long now;
    private float confMin;
    private int volMax;
    private int ditherTime;
    private float termVolumeMaxFactor = 0.5f;

    public ConjClustering(NAR nar, byte punc, Predicate<Task> filter, int centroids, int capacity) {
        super(nar);

        this.ConjClusterModel = new BagClustering.Dimensionalize<>(4) {

            @Override
            public void coord(Task t, double[] c) {
                c[0] = t.start();
                c[3] = t.priElseZero(); //0..+1
                Truth tt = t.truth();
                c[1] = tt.polarity(); //0..+1 //if negative, will be negated in subterms
                c[2] = tt.conf(); //0..+1
            }

            @Override
            public double distanceSq(double[] a, double[] b) {
                return (1 + ((double)Math.abs(a[0] - b[0]))/nar.dur())    //d(time)/dur
                        *
                        (
                          Math.abs(a[1] - b[1])  //d(freq_polarity)
                        + Math.abs(a[2] - b[2])  //d(conf)
                        + Math.abs(a[3] - b[3])  //d(pri)
                        );
            }
        };

        this.bag = new BagClustering<>(ConjClusterModel, centroids, capacity);

        this.punc = punc;
        this.in = nar.newCauseChannel(this);

        nar.onTask(t -> {
            if (!t.isEternal() && t.punc() == punc && filter.test(t)) {
                bag.put(t,
                        //t.priElseZero() * (1f / t.volume()) //prefer smaller events
                        t.priElseZero()
                        //(1f + t.expolarity()) * (1f + t.conf())
                );
                //* (1f + t.priElseZero()));// / t.volume());
            }
        });
    }

    private int taskLimitPerCentroid;

    private FasterList<Task> gen = null;

    @Override
    protected int next(NAR nar, int work /* max tasks generated per centroid, >=1 */) {

        if (bag.bag.isEmpty())
            return -1; //done for cycle

        this.now = nar.time();
        this.ditherTime = nar.dtDitherCycles();
        this.confMin = nar.confMin.floatValue();
        this.volMax = Math.round(nar.termVolumeMax.intValue() * termVolumeMaxFactor);
        this.taskLimitPerCentroid = Math.max(1, Math.round(((float) work) / bag.net.centroids.length));

        gen = new FasterList();

        bag.commitGroups(1, nar, this::conjoinCentroid);

        if (!gen.isEmpty()) {
//            System.out.println(gen);
            in.input(gen);
        }

        /* decrease cluster bag vlink priority by at least as much as the priority lost by the task during conjing
           helps ensures fair induction of new items and replacement of processed items (which can remain indefinitely)*/
        bag.bag.commit(t -> {
            float pp = t.id.pri();
            if (pp == pp)
                t.priMin(pp);
            else
                t.delete();
        });

        return (int) Math.ceil(((float)gen.size())/bag.net.centroids.length);
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


        Iterator<VLink<Task>> gg =
                group.filter(x -> x != null && !x.isDeleted()).iterator();
        //Iterators.peekingIterator();


        Map<LongObjectPair<Term>, Task> vv = new HashMap<>();
        FasterList<Task> actualTasks = new FasterList();


        int centroidGen = 0;

        main:
        while (gg.hasNext() && centroidGen < taskLimitPerCentroid) {

            vv.clear();
            actualTasks.clear();


            long end = Long.MIN_VALUE;
            long start = Long.MAX_VALUE;


            int dur = nar.dur();
            float freq = 1f;
            float conf = 1f;
            float priMax = Float.NEGATIVE_INFINITY, priMin = Float.POSITIVE_INFINITY;
            int vol = 0;
            int maxVolume = 0;

            do {
                if (!gg.hasNext())
                    break;

                Task t =
                        gg.next().id;
                //gg.peek().id;
                Term xt = t.term();

                long zs = Tense.dither(t.start(), ditherTime);
                long ze = Tense.dither(t.end(), ditherTime);
//                assert (end >= start);

                Truth tx = t.truth();
                Term xtn = xt.neg();
                if (tx.isNegative()) {
                    xt = xtn;
                }

                int xtv = xt.volume();
                maxVolume = Math.max(maxVolume, xt.volume());
                if (vol + xtv + 1 >= volMax || conf * tx.conf() < confMin) {
                    continue; //cant go any further with this task
                }

                boolean involved = false;
                LongObjectPair<Term> ps = pair(zs, xt);
                Term xtNeg = xt.neg();

                //TODO fairly decide midpoint if the conj cant hold both endpoints:

                if (!vv.containsKey(pair(zs, xtNeg)) && null == vv.putIfAbsent(ps, t)) {
                    vol += xtv;
                    if (start > zs) start = zs;
                    if (end < zs) end = zs;
                    involved = true;
                }

                if (ze - zs >= dur) {
                    //endpoint
                    if (vol + xtv + 1 < volMax) {
                        LongObjectPair<Term> pe = pair(ze, xt);
                        if (!vv.containsKey(pair(ze, xtNeg)) && null == vv.putIfAbsent(pe, t)) { //end point, if different from start
                            vol += xtv;
                            if (end < ze) end = ze;
                            involved = true;
                        }
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

            ObjectFloatPair<long[]> evidence = Stamp.zip(actualTasks, Param.STAMP_CAPACITY);
            float overlap = evidence.getTwo();
            float e = Param.overlapEvidence(c2wSafe(conf), overlap);
            if (e > 0) {
                final DiscreteTruth t = Truth.theDiscrete(freq, e, nar);
                if (t != null) {

                    Term cj = Op.conjEvents(new FasterList(vv.keySet()));
                    if (cj != null) {

                        cj = cj.normalize();


                        if (Math.abs(cj.dtRange() - (end - start)) < ditherTime) { //test if merge collapse occurred and occurrence time needs recalculated


                            ObjectBooleanPair<Term> cp = Task.tryContent(cj, punc, true);
                            if (cp != null) {


                                NALTask m = new STMClusterTask(cp, t, start, end, evidence.getOne(), punc, now); //TODO use a truth calculated specific to this fixed-size batch, not all the tasks combined
                                if (evidence.getTwo() > 0) m.setCyclic(true);

                                m.cause = Cause.zip(nar.causeCapacity.intValue(), uu);

                                float priAvg =
                                        //priMax;
                                        //priMin;
                                        (priMin + priMax) / 2f;

                                //complexity deduction
                                //  how much more complex the conjunction is than the most complex of its ingredients
                                int v = cp.getOne().volume();
                                float cmplFactor =
                                        ((float) v) / (v + maxVolume);

                                m.priSet(Priority.fund(priAvg * cmplFactor, true, uu));
                                gen.add(m);
                                centroidGen++;
                            } else {
                                //Task.tryContent(cj, punc, true);
                                //logger.warn("{} failed", this);
                            }
                        } else {
                            //System.out.println("merge collapse, recalcu");
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

        public STMClusterTask(@Nullable ObjectBooleanPair<Term> cp, Truth t, long start, long end, long[] evidence, byte punc, long now) throws InvalidTaskException {
            super(cp.getOne(), punc, t, now, start, end, evidence);
        }

        @Override
        public float eternalizability() {
            return 0;
        }

        @Override
        public boolean isInput() {
            return false;
        }
    }


}
