package nars.op.stm;

import jcog.TODO;
import jcog.Util;
import jcog.pri.MultiLink;
import jcog.pri.PLink;
import jcog.pri.VLink;
import nars.NAR;
import nars.Task;
import nars.bag.BagClustering;
import nars.concept.Concept;
import nars.control.DurService;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Task Dimension Mapping:
 * 0: Start time
 * 1: End time
 * 2: Freq
 * 3: Conf (grouping by confidence preserves the maximum collective confidence of any group, which is multiplied in conjunction truth)
 */
public class LinkClustering extends DurService {

    //private static final Logger logger = LoggerFactory.getLogger(MySTMClustered.class);


    public final BagClustering<Task> bag;

    private final FloatFunction<Task> accept;

    float confMin;

    private int dur;
    private float truthRes;

//    final static BagClustering.Dimensionalize<Task> STMClusterModel0 = new BagClustering.Dimensionalize<Task>(4) {
//
//        @Override
//        public void coord(Task t, double[] c) {
//            c[0] = t.start();
//            c[1] = t.end();
//            c[2] = t.truth().isNegative() ? (1f - t.freq()) : t.freq(); //0..+1 //if negative, will be negated in subterms
//            c[3] = t.conf(); //0..+1
//        }
//
//    };

    final static BagClustering.Dimensionalize<Task> TimeClusterModel = new BagClustering.Dimensionalize<>(2) {

        @Override
        public void coord(Task t, double[] c) {
            c[0] = t.mid();
            c[1] = t.range();
        }

        @Override
        public double distanceSq(double[] a, double[] b) {
            return Util.sqr(
                    Math.abs(a[0] - b[0])
            ) +

                    Util.sqr(Math.abs(a[1] - b[1]));
        }
    };

    private int minConjSize, maxConjSize;

    /** the 'accept' function determines the bag insertion priority of the task.
     * this need not be the same as the task priority.
     * if this function returns NaN, then the insertion is not attempted (filtered).
     * @param nar
     * @param accept
     * @param centroids
     * @param capacity
     */
    public LinkClustering(@NotNull NAR nar, FloatFunction<Task> accept, int centroids, int capacity, int minConjSize, int maxConjSize) {
        super(nar);

        this.accept = accept;
        this.minConjSize = minConjSize;
        this.maxConjSize = maxConjSize;

        bag = new BagClustering<>(TimeClusterModel, centroids, capacity);

        nar.onTask((t) -> accept(nar, t));
    }

    protected void linkClustersChain(List<VLink<Task>> sortedbyCentroid) {

        int current = -1;
        int nTasks = sortedbyCentroid.size();
        VLink<Task> x = null;
        for (int i = 0; i < nTasks; i++) {
            VLink<Task> y = sortedbyCentroid.get(i);
            if (y.centroid!=current) {
                current = y.centroid;
            } else {
                //link to previous item
                Task tx = x.get();
                Task ty = y.get();
                float linkPri =
                        //tx.pri() * ty.pri();
                        Util.or(tx.priElseZero(), ty.priElseZero());
                throw new TODO();
//                STMLinkage.link(tx, linkPri, ty, nar);
            }
            x = y;
        }
    }

    protected void linkClustersMulti(Stream<VLink<Task>> group, NAR nar) {
        Task[] tasks = group.map(PLink::get).toArray(Task[]::new);

        Arrays.sort(tasks, Comparators.byIntFunction(Task::hashCode)); //keep them in a canonical ordering for equality testing purposes

        float pri = Util.sum(Task::priElseZero, tasks);

        MultiLink<Task,Task> task = new MultiLink<>( tasks, Function.identity(), pri );
        //MultiLink<Task,Term> term = new MultiLink<>( tasks, Task::term, pri );

        group.forEach(t -> {
            Concept tc = t.get().concept(nar, false);
            if (tc!=null) {
                tc.tasklinks().putAsync(task);
                //tc.termlinks().putAsync(term);
            }
        });

    }

    public void accept(NAR nar, @NotNull Task t) {
        long now = nar.time();
        int dur = nar.dur();
        if (!t.isEternal()) {
            float p = accept.floatValueOf(t);
            if (p == p) {
                bag.put(t, p);
                //t.conf(now, dur)
                //Util.or(t.priElseZero() , t.conf(now, dur))
                //t.priElseZero()
                //t.conf()
                //t.conf() * t.priElseZero()
            }
        }
    }


    @Override
    protected void run(NAR n, long dt) {

        confMin = nar.confMin.floatValue();
        truthRes = nar.freqResolution.floatValue();
        dur = nar.dur();

        //LongObjectHashMap<ObjectFloatPair<TasksNode>> selected = new LongObjectHashMap<>();

        //clusters where all terms occurr simultaneously at precisely the same time
        //cluster(maxConjunctionSize, 1.0f, freqCoherenceThresh);


        //int maxVol = nar.termVolumeMax.intValue() - 2;

        //bag.commit(1, this::linkClusters);
        bag.commitGroups(1, nar, this::linkClustersMulti);


    }

}
