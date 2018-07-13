package nars.bag;

import jcog.bag.Bag;
import jcog.bag.impl.ArrayBag;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.list.FasterList;
import jcog.pri.Prioritized;
import jcog.pri.VLink;
import jcog.pri.op.PriMerge;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;



/**
 * clusterjunctioning
 * TODO abstract into general purpose "Cluster of Bags" class
 */
public class BagClustering<X> {
    /**
     * how to interpret the bag items as vector space data
     */
    abstract public static class Dimensionalize<X> {

        final int dims;

        protected Dimensionalize(int dims) {
            this.dims = dims;
        }

        abstract public void coord(X t, double[] d);

        /**
         * default impl, feel free to override
         */
        public double distanceSq(double[] a, double[] b) {
            return Centroid.distanceCartesianSq(a, b);
        }

    }


    public final Bag<X, VLink<X>> bag;
    public final NeuralGasNet net;
    final Dimensionalize<X> model;
    /**
     * TODO allow dynamic change
     */
    protected /*Flip<*/ FasterList<VLink<X>> sorted =
            new FasterList<>();
    
    AtomicBoolean bagBusy = new AtomicBoolean(false);


    public BagClustering(Dimensionalize<X> model, int centroids, int initialCap) {


        this.model = model;

        this.net = new NeuralGasNet(model.dims, centroids, model::distanceSq);

        this.bag = new ArrayBag<>(PriMerge.max, initialCap) {

            @Nullable
            @Override
            public X key(VLink<X> x) {
                return x.id;
            }

        };


















    }

    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        forEachCluster(c -> {
            out.println(c);
            stream(c.id).forEach(i -> {
                out.print("\t");
                out.println(i);
            });
            out.println();
        });
        out.println(net.edges);
    }















    public void forEachCluster(Consumer<Centroid> c) {
        for (Centroid b : net.centroids) {
            c.accept(b);
        }
    }

    public int size() {
        return bag.size();
    }

    public <Y> void commitGroups(Y y, float forgetRate, BiConsumer<Stream<VLink<X>>, Y> each) {
        commit(forgetRate, (sorted) -> {
            int current = -1;
            int n = sorted.size();
            int bs = -1;
            for (int i = 0; i < n; i++) {
                VLink<X> x = sorted.get(i);

                if (current != x.centroid || (i == n - 1)) {
                    current = x.centroid;
                    if (bs != -1 && i - bs > 1) {
                        each.accept(IntStream.range(bs, i + 1).mapToObj(sorted::get), y);
                    }
                    bs = i;
                }
            }
        });
    }

    public void commit(float forgetRate, Consumer<List<VLink<X>>> takeSortedClusters) {

        FasterList<VLink<X>> x;

        if (bagBusy.compareAndSet(false, true)) {

            

            try {

                
                
                bag.commit(t -> {
                    X tt = t.id;
                    if ((tt instanceof Prioritized) && ((Prioritized)tt).isDeleted())
                        t.delete();
                    
                    
                });
                bag.commit(bag.forget(forgetRate)); 

                
                

                int learningIterations = 1;
                for (int i = 0; i < learningIterations; i++)
                    bag.forEach(this::learn);



            } finally {
                bagBusy.set(false);
            }
        }

        x = new FasterList<>(bag.size());
        bag.forEach(x::add);

        int s = x.size();
        if (s>0) {
            ArrayUtils.quickSort(0, s,
                    (a,b) -> Integer.compare(x.get(a).centroid, x.get(b).centroid),
                    x::swap);
//            x.sortThisByInt(xx -> xx.centroid);
            takeSortedClusters.accept(x);
        }



    }


    private void learn(VLink<X> x) {
        double x0 = x.coord[0];
        if (x0 != x0)
            model.coord(x.id, x.coord);

        x.centroid = net.put(x.coord).id;
    }

    public void clear() {
        synchronized (bag) {
            bag.clear();
            net.clear();
        }
    }

    public void put(X x, float pri) {
        bag.putAsync(new VLink<>(x, pri, model.dims)); 
    }

    public void remove(X x) {
        bag.remove(x);
    }

    /**
     * returns NaN if either or both of the items are not present
     */
    public double distance(X x, X y) {
        assert (!x.equals(y));
        @Nullable VLink<X> xx = bag.get(x);
        if (xx != null && xx.centroid >= 0) {
            @Nullable VLink<X> yy = bag.get(y);
            if (yy != null && yy.centroid >= 0) {
                return Math.sqrt(net.distanceSq.distance(xx.coord, yy.coord));
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * TODO this is O(N) not great
     */
    public Stream<VLink<X>> stream(int centroid) {
        return bag.stream().filter(y -> y.centroid == centroid);
    }

    public Stream<VLink<X>> neighbors(X x) {
        @Nullable VLink<X> link = bag.get(x);
        if (link != null) {
            int centroid = link.centroid;
            if (centroid >= 0) {
                Centroid[] nodes = net.centroids;
                if (centroid < nodes.length) 
                    return stream(centroid)
                            .filter(y -> !y.equals(x))
                            ;
            }
        }
        return Stream.empty();
    }

}
