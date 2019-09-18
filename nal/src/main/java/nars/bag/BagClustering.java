package nars.bag;

import jcog.data.list.FasterList;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.pri.VLink;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PriReferenceArrayBag;
import jcog.pri.bag.impl.SimpleBufferedBag;
import jcog.pri.op.PriMerge;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
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

    /** each option here has its own subtle consequences. be careful */
    final PriMerge merge =
            //PriMerge.replace;
            PriMerge.max;


    public final Bag<X, VLink<X>> bag;
    public final NeuralGasNet net;
    final Dimensionalize<X> model;
    /**
     * TODO allow dynamic change
     */


    public BagClustering(Dimensionalize<X> model, int centroids, int initialCap) {

        this.model = model;

        this.net = new NeuralGasNet(model.dims, centroids, model::distanceSq);

        this.bag = new SimpleBufferedBag<>(new PriReferenceArrayBag<>(merge, initialCap) {
                    @Override
                    protected int histogramBins(int s) {
                        return 0; //disabled
                    }
                });

    }

    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        forEachCluster(c -> {
            out.println(c);
            stream(c._id).forEach(i -> {
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

    public void forEachCentroid(Consumer<FasterList<X>> each) {
        forEachCentroid(FasterList::new, each);
    }

    public <L extends List<X>> void forEachCentroid(IntToObjectFunction<L> listBuilder, Consumer<L> each) {
        iterateCentroids(listBuilder).forEach(each);
    }

    private <L extends List<X>> Iterable<L> iterateCentroids(IntToObjectFunction<L> listBuilder) {

        int s = bag.size();
        if (s == 0)
            return Collections.EMPTY_LIST;
        else {

            int cc = net.centroidCount();
            IntObjectHashMap<L> x = new IntObjectHashMap<>(cc);
            int meanItemsPerCentroid = (int)Math.ceil(((float)s)/cc);

            bag.forEach((xx) -> {
                int c = xx.centroid;
                if (c >= 0) {
                    X xxx = xx.get();
                    if (xxx!=null)
                        x.getIfAbsentPut(c, ()->listBuilder.apply(meanItemsPerCentroid)).add(xxx);
                }
            });
            if (x.isEmpty())
                return Collections.EMPTY_LIST;

            return x.values();
        }
    }
    public void learn(float forgetRate, int learningIterations) {

            @Nullable Consumer<VLink<X>> f = bag.forget(forgetRate);
            bag.commit(v -> v.update(f));

//            net.alpha.setAt(0.8f / s);
            float lambdaFactor = 1f;
            net.setLambdaPeriod((int) Math.ceil((bag.capacity()) * lambdaFactor));

            Consumer<VLink<X>> l = this::learn;
            for (int i = 0; i < learningIterations; i++)
                bag.forEach(l);
//        }
    }

//    private List<VLink<X>> itemsSortedByCentroid(Random rng) {
//
//        int s = bag.size();
//        if (s == 0)
//            return List.of();
//
//        FasterList<VLink<X>> x = new FasterList<>(s);
//        bag.forEach(x::add);
//
//
//        s = x.size();
//        if (s > 2) {
//
//            int shuffle = rng.nextInt();
//            IntToIntFunction shuffler = (c) -> c ^ shuffle;
//
//            ArrayUtils.quickSort(0, s,
//                    (a, b) -> a == b ? 0 : Integer.compare(
//                            shuffler.applyAsInt(x.get(a).centroid),
//                            shuffler.applyAsInt(x.get(b).centroid)),
//                    x::swap);
//
//        }
//
//
//        return x;
//    }


    private void learn(VLink<X> x) {
        double x0 = x.coord[0];
        if (x0 != x0) {
            model.coord(x.get(), x.coord);
        }

        Centroid y = net.put(x.coord);
        x.centroid = y.id;
    }

    public void clear() {
        synchronized (bag) {
            bag.clear();
            net.clear();
        }
    }

    public final void put(X x, float pri) {
        if (pri == pri)
            bag.putAsync(new VLink<>(x, pri, model.dims));
    }

    public final void remove(X x) {
        bag.remove(x);
    }

    /**
     * returns NaN if either or both of the items are not present
     */
    public double distance(X x, X y) {
        //assert (!x.equals(y));
        assert (x != y);

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
    public Stream<VLink<X>> stream(int internalID) {
        return bag.stream().filter(y -> y.centroid == internalID);
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
