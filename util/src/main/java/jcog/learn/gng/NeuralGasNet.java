package jcog.learn.gng;

import com.google.common.base.Joiner;
import jcog.learn.gng.impl.Centroid;
import jcog.learn.gng.impl.DenseIntUndirectedGraph;
import jcog.learn.gng.impl.ShortUndirectedGraph;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import jcog.tree.rtree.point.DoubleND;
import jcog.tree.rtree.rect.HyperRectDouble;
import jcog.tree.rtree.rect.MutableHyperRectDouble;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * from: https:
 * TODO use a graph for incidence structures to avoid some loops
 */
public class NeuralGasNet<N extends Centroid>  /*extends SimpleGraph<N, Connection<N>>*/ {


    public final int dimension;

    /**
     * the bounds of all the centroids in all dimensions (ex: for normalizing their movement and/or distance functions)
     * stored as a 1D double[] with every pair of numbers corresponding
     * to min/max bounds of each dimension, so it will have dimension*2 elements
     */
    private final MutableHyperRectDouble rangeMinMax;

    public final ShortUndirectedGraph edges;
    public final Centroid[] centroids;

    public final Centroid.DistanceFunction distanceSq;

    private int iteration;


    private final int maxNodes;
    private int lambdaPeriod;
    private int ttl;
    public final FloatRange alpha = new FloatRange(0.8f, 0, 1f);
    public final FloatRange beta = new FloatRange(0.9f, 0, 1f);


    /**
     * faster point learning for the winner node
     */
    private double winnerUpdateRate;

    /**
     * slower point learning for the neighbors of a winner node
     */
    private double winnerNeighborUpdateRate;


    public int getLambdaPeriod() {
        return lambdaPeriod;
    }

    /**
     * lifespan of an node
     */
    public void setLambdaPeriod(int lambdaPeriod) {
        this.lambdaPeriod = lambdaPeriod;
    }

    public void setWinnerUpdateRate(double rate, double neighborRate) {
        this.winnerUpdateRate = rate;
        this.winnerNeighborUpdateRate = neighborRate;
    }

    public void setBeta(float beta) {
        this.beta.set(beta);
    }

    public void setMaxEdgeAge(int maxAge) {
        this.ttl = maxAge;
    }

    public int getTtl() {
        return ttl;
    }

    public double getWinnerUpdateRate() {
        return winnerUpdateRate;
    }


    public NeuralGasNet(int dimension, int centroids) {
        this(dimension, centroids, null);
    }

    public NeuralGasNet(int dimension, int centroids, Centroid.DistanceFunction distanceSq) {
        super();


        edges =

                new DenseIntUndirectedGraph((short) centroids);

        this.centroids = new Centroid[centroids];

        this.rangeMinMax = new MutableHyperRectDouble(dimension);

        this.distanceSq = distanceSq != null ? distanceSq : Centroid.DistanceFunction::distanceCartesianSq;


        this.iteration = 0;
        this.dimension = dimension;
        this.maxNodes = centroids;


        setLambdaPeriod(centroids * 2);
        setMaxEdgeAge(centroids * 2);


        setWinnerUpdateRate(0.5f / centroids, 0.25f / centroids);


        clear();
    }

    public void forEachNode(Consumer<N> each) {
        for (Centroid n : centroids)
            each.accept((N) n);
    }

    public void clear() {
        edges.clear();

        /** nodes should begin with randomized coordinates */
        for (int i = 0; i < centroids.length; i++) {
            this.centroids[i] = newCentroid(i, dimension);
        }
    }

    public N newCentroid(int i, int dims) {
        return (N) new Centroid(i, dims);
    }

    public N closest(double[] x) {

        double minDistSq = Double.POSITIVE_INFINITY;
        Centroid closest = null;


        for (Centroid n : centroids) {
            double dist;
            if ((dist = distanceSq.distance(n.getDataRef(), x)) < minDistSq) {
                closest = n;
                minDistSq = dist;
            }
        }

        return (N) closest;
    }


    public HyperRectDouble bounds() {
        MutableHyperRectDouble b = null;
        //double[] err = new double[dimension];

        for (int i1 = 0, centroidsLength = centroids.length; i1 < centroidsLength; i1++) {
            Centroid n = centroids[i1];
            if (n.active()) {
                if (b == null) {
                    b = new MutableHyperRectDouble(new DoubleND(n.toArray()));
                } else {
                    b.mbrSelf(n.getDataRef());
                }
                //err[i] = n.localError();
            }
        }
//        for (int k = 0; k < dimension; k++) {
//            b.grow(k, err[k]);
//        }
        return b;
    }

    /**
     * translates all nodes uniformly
     */
    public void translate(double[] x) {
        for (Centroid n : centroids) {
            n.add(x);
        }
    }

    private transient double maxError;
    private transient short _maxErrorNeighbour;

    public synchronized N put(double... x) {
        if (x.length != dimension)
            throw new ArrayIndexOutOfBoundsException();

        float alpha = this.alpha.floatValue();
        if (alpha < ScalarValue.EPSILON || lambdaPeriod == 0)
            return null;


        double minDist = Double.POSITIVE_INFINITY;
        double maxDist = Double.NEGATIVE_INFINITY;
        short closest = -1;
        short furthest = -1;


        int nodes = maxNodes;


        for (short j = 0; j < nodes; j++) {
            Centroid nj = this.centroids[j];

            double dd = nj.learn(x, distanceSq);

            if (j == 0) {
                furthest = closest = j;
                maxDist = minDist = dd;
            } else {
                if (dd > maxDist) {
                    furthest = j;
                    maxDist = dd;
                }
                if (dd < minDist) {
                    closest = j;
                    minDist = dd;
                }
            }
        }

        double minDist2 = Double.POSITIVE_INFINITY;
        short nextClosestNode = -1;
        for (short j = 0; j < nodes; j++) {
            Centroid n = this.centroids[j];
            if (n == null)
                continue;
            if (j == closest) continue;
            double dd = n.localDistanceSq();
            if (dd < minDist2) {
                nextClosestNode = j;
                minDist2 = dd;
            }
        }


        if (closest == -1) {


            return null;
        }

        assert (closest != nextClosestNode);


        this.centroids[closest].updateLocalError(x, winnerUpdateRate);


        edges.edgesOf(closest, (connection, age) -> this.centroids[connection].lerp(x, winnerNeighborUpdateRate));
        edges.addToEdges(closest, -1);


        if (nextClosestNode != -1)
            edges.setEdge(closest, nextClosestNode, ttl);


        if (iteration++ % lambdaPeriod == 0) {

            edges.removeVertex(furthest);
            removed((N) this.centroids[furthest]);


            short maxErrorID = -1;
            {
                double maxError = Double.NEGATIVE_INFINITY;
                for (int i = 0, nodeLength = this.centroids.length; i < nodeLength; i++) {
                    Centroid n = this.centroids[i];
                    if (i == furthest)
                        continue;
                    if (n.localError() > maxError) {
                        maxErrorID = (short) i;
                        maxError = n.localError();
                    }
                }

                if (maxErrorID == -1) {
                    throw new RuntimeException("maxErrorID=null");
                }
            }


            maxError = Double.NEGATIVE_INFINITY;
            _maxErrorNeighbour = -1;

            edges.edgesOf(maxErrorID, (otherNodeID) -> {

                Centroid otherNode = this.centroids[otherNodeID];

                if (otherNode.localError() > maxError) {
                    _maxErrorNeighbour = otherNodeID;
                    maxError = otherNode.localError();
                }
            });

            if (_maxErrorNeighbour != -1) {

                short maxErrorNeighborID = _maxErrorNeighbour;


                edges.removeEdge(maxErrorID, maxErrorNeighborID);


                N newNode = newCentroid(furthest, dimension);
                randomizeCentroid(rangeMinMax, newNode);


                Centroid maxErrorNeighbor = this.centroids[maxErrorNeighborID];
                Centroid maxErrorNode = this.centroids[maxErrorID];
                newNode.set(maxErrorNode, maxErrorNeighbor);
                this.centroids[furthest] = newNode;

                if (maxErrorID == furthest) {
                    throw new RuntimeException("new node has same id as max error node");
                }


                edges.setEdge(maxErrorID, furthest, ttl);
                edges.setEdge(maxErrorNeighborID, furthest, ttl);


                maxErrorNode.mulLocalError(alpha);
                maxErrorNeighbor.mulLocalError(alpha);
            }
        }


        float beta = this.beta.floatValue();
        for (Centroid n : this.centroids)
            n.mulLocalError(beta);

        return node(closest);
    }

    public void randomizeCentroid(MutableHyperRectDouble bounds, N newNode) {
        for (int i = 0; i < dimension; i++)
            newNode.randomizeUniform(i, bounds.coord(i, false), bounds.coord(i, true));
    }


    public final N node(int i) {
        return (N) centroids[i];
    }

//    private short randomNode() {
//        return (short) (Math.random() * centroids.length);
//    }

    /**
     * called before a node will be removed
     */
    protected void removed(N furthest) {

    }

    public Stream<N> nodeStream() {
        return Stream.of(centroids).filter(Centroid::active).map(n -> (N) n);
    }

    public void compact() {
        edges.compact();
    }

    public int size() {
        return centroids.length;
    }

    @Override
    public String toString() {
        return Joiner.on("\n").join(centroids);
    }


    public int centroidCount() {
        return centroids.length;
    }
}





















































































