package nars.bag;

import jcog.bag.Bag;
import jcog.bag.impl.ConcurrentArrayBag;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.list.FasterList;
import jcog.pri.Prioritized;
import jcog.pri.VLink;
import jcog.pri.op.PriMerge;
import jcog.util.Flip;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Comparator;
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

    public final Bag<X, VLink<X>> bag;

    final Dimensionalize<X> model;

    public final NeuralGasNet net;

    final AtomicBoolean busy = new AtomicBoolean(false);


    /**
     * TODO allow dynamic change
     */
    private final short clusters;
    public Flip<FasterList<VLink<X>>> sorted = new Flip(FasterList::new);

    public BagClustering(Dimensionalize<X> model, int centroids, int initialCap) {

        this.clusters = (short) centroids;

        this.model = model;

        this.net = new NeuralGasNet(model.dims, centroids, model::distanceSq);

        this.bag = new ConcurrentArrayBag<>(PriMerge.max, initialCap) {

            @Nullable
            @Override
            public X key(VLink<X> x) {
                return x.id;
            }

        };

//        this.bag = new HijackBag<X, VLink<X>>(initialCap, 4) {
//            @Override
//            protected VLink<X> merge(VLink<X> existing, VLink<X> incoming, @Nullable MutableFloat overflowing) {
//                existing.priMax(incoming.priElseZero());
//                return existing;
//            }
//
//            @Override
//            public float pri(VLink<X> key) {
//                return key.pri();
//            }
//
//            @Override
//            public X key(VLink<X> value) {
//                return value.get();
//            }
//        };
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


//    protected class MyForget extends PriForget<VLink<X>> {
//
//        public MyForget(float priFactor) {
//            super(priFactor);
//        }
//
//        @Override
//        public void accept(VLink<X> b) {
//            super.accept(b);
//            learn(b);
//        }
//    }

    public int size() {
        return bag.size();
    }

    public <Y> void commitGroups(int iter, Y y, BiConsumer<Stream<VLink<X>>,Y> each) {
        commit(iter, (sorted) -> {
            int current = -1;
            int n = sorted.size();
            int bs = -1;
            for (int i = 0; i < n; i++) {
                VLink<X> x = sorted.get(i);
                X xx = x.get();
                if (xx instanceof Prioritized && ((Prioritized)xx).isDeleted()) {
                    x.delete();
                    sorted.set(i, null);
                    continue;
                }

                if (current != x.centroid) {
                    current = x.centroid;
                    if (bs != -1 && i - bs > 1)
                        each.accept(IntStream.range(bs, i).mapToObj(sorted::get), y);
                    bs = i;
                }
            }
        });
    }


    /**
     * how to interpret the bag items as vector space data
     */
    abstract public static class Dimensionalize<X> {

        public final int dims;

        protected Dimensionalize(int dims) {
            this.dims = dims;
        }

        abstract public void coord(X t, double[] d);

        /** default impl, feel free to override */
        public double distanceSq(double[] a, double[] b) {
            return Centroid.distanceCartesianSq(a, b);
        }

    }





    public boolean commit(int iterations, Consumer<List<VLink<X>>> takeSortedClusters) {

        if (busy.compareAndSet(false, true)) {

            try {
                synchronized (bag) {

                    int s = bag.size();
                    if (s == 0)
                        return false;

                    bag.commit(); //first, apply bag forgetting

                    //                net.compact();
                    //int cc = bag.capacity();

                    for (int i = 0; i < iterations; i++) {
                        bag.forEach(this::learn);
                    }

                    FasterList<VLink<X>> x = sorted.write();
                    x.clear();
                    bag.forEach(x::add);
                    x.sortThisByInt(xx -> xx.centroid );
                    x.sort(Comparator.comparingInt(a -> a.centroid));
                    takeSortedClusters.accept(x);
                    sorted.commit();
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            } finally {
                 busy.set(false);

            }

            return true;
        } else {
            return false;
        }

    }


    private void learn(VLink<X> x) {
        if (x.coord[0]!=x.coord[0])
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
        bag.putAsync(new VLink<>(x, pri, model.dims)); //TODO defer vectorization until after accepted
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
                return Math.sqrt( net.distanceSq.distance(xx.coord, yy.coord) );
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
                if (centroid < nodes.length) //in case of resize
                    return stream(centroid)
                            .filter(y -> !y.equals(x))
                            ;
            }
        }
        return Stream.empty();
    }

}
