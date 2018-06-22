package nars.op.stm;

import jcog.Util;
import jcog.pri.VLink;
import nars.NAR;
import nars.Task;
import nars.bag.BagClustering;
import nars.control.DurService;
import nars.util.TimeAware;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Task Dimension Mapping:
 * 0: Start time
 * 1: End time
 * 2: Freq
 * 3: Conf (grouping by confidence preserves the maximum collective confidence of any group, which is multiplied in conjunction truth)
 */
public abstract class ChainClustering extends DurService {

    


    public final BagClustering<Task> bag;

    private final FloatFunction<Task> accept;

    float confMin;

    protected int dur;














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


    /** the 'accept' function determines the bag insertion priority of the task.
     * this need not be the same as the task priority.
     * if this function returns NaN, then the insertion is not attempted (filtered).
     * @param nar
     * @param accept
     * @param centroids
     * @param capacity
     */
    public ChainClustering(@NotNull NAR nar, FloatFunction<Task> accept, int centroids, int capacity) {
        super(nar);

        this.accept = accept;
        bag = new BagClustering<>(TimeClusterModel, centroids, capacity);

        nar.onTask((t) -> accept(nar, t));
    }

      protected void linkClustersChain(Stream<VLink<Task>> sortedByCentroidStream, TimeAware timeAware) {

        List<VLink<Task>> sortedbyCentroid = sortedByCentroidStream.collect(Collectors.toList());
        int current = -1;
        int nTasks = sortedbyCentroid.size();
        VLink<Task> x = null;
          for (VLink<Task> y: sortedbyCentroid) {
              if (y == null)
                  continue;
              if (y.centroid != current) {
                  current = y.centroid;
              } else {

                  Task tx = x.id;
                  Task ty = y.id;
                  link(tx, ty);

              }
              x = y;
          }
    }

    abstract protected void link(Task tx, Task ty);




    public void accept(TimeAware timeAware, Task t) {
        long now = timeAware.time();
        int dur = timeAware.dur();
        if (!t.isEternal()) {
            float p = accept.floatValueOf(t);
            if (p == p) {
                bag.put(t, p);
                
                
                
                
                
            }
        }
    }


    @Override
    protected void run(NAR n, long dt) {

        confMin = nar.confMin.floatValue();

        dur = nar.dur();

        

        
        


        

        
        bag.commitGroups(nar, nar.forgetRate.floatValue(), this::linkClustersChain);


    }

}
