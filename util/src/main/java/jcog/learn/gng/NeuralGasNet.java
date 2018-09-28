package jcog.learn.gng;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.learn.gng.impl.Centroid;
import jcog.learn.gng.impl.DenseIntUndirectedGraph;
import jcog.learn.gng.impl.ShortUndirectedGraph;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * from: https:
 * TODO use a graph for incidence structures to avoid some loops
 */
public class NeuralGasNet<N extends Centroid>  /*extends SimpleGraph<N, Connection<N>>*/ {


    public final int dimension;

    /** the bounds of all the centroids in all dimensions (ex: for normalizing their movement and/or distance functions)
     * stored as a 1D double[] with every pair of numbers corresponding
     * to min/max bounds of each dimension, so it will have dimension*2 elements
     */
    public final double[] rangeMinMax;

    public final ShortUndirectedGraph edges;
    public final Centroid[] centroids;

    public final Centroid.DistanceFunction distanceSq;

    private int iteration;


    private final int maxNodes;
    private int lambda;
    private int ttl;
    private double alpha;
    private double beta;

    /**
     * faster point learning for the winner node
     */
    private double winnerUpdateRate;

    /**
     * slower point learning for the neighbors of a winner node
     */
    private double winnerNeighborUpdateRate;

    private final float rangeAdaptRate;

    public int getLambda() {
        return lambda;
    }

    /**
     * lifespan of an node
     */
    public void setLambda(int lambda) {
        this.lambda = lambda;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public void setWinnerUpdateRate(double rate, double neighborRate) {
        this.winnerUpdateRate = rate;
        this.winnerNeighborUpdateRate = neighborRate;
    }

    public void setBeta(double beta) {
        this.beta = beta;
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

        this.rangeMinMax = new double[dimension * 2];
        Arrays.fill(rangeMinMax, Float.NaN);
        this.rangeAdaptRate = 1f /(1f + centroids);

        this.distanceSq = distanceSq != null ? distanceSq : Centroid.DistanceFunction::distanceCartesianSq;


        this.iteration = 0;
        this.dimension = dimension;
        this.maxNodes = centroids;


        
        setLambda(centroids*2);
        setMaxEdgeAge(centroids*2);

        setAlpha(0.8);
        setBeta(0.9);

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



    /**
     * translates all nodes uniformly
     */
    public void translate(double[] x) {
        for (Centroid n : centroids) {
            n.add(x);
        }
    }

    public N put(double... x) {

        if (alpha == 0 || lambda == 0)
            return null; 

        
        double minDist = Double.POSITIVE_INFINITY;
        double maxDist = Double.NEGATIVE_INFINITY;
        short closest = -1;
        short nextClosestNode = -1;
        short furthest = -1;


        final int nodes = maxNodes;


        range(x, rangeAdaptRate);

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

        
        short sc = closest;
        edges.edgesOf(closest, (connection, age) -> {
            this.centroids[connection].update(x, winnerNeighborUpdateRate);
        });
        edges.addToEdges(sc, -1);


        
        

        
        if (nextClosestNode != -1)
            edges.setEdge(closest, nextClosestNode, ttl);


        
        if (iteration++ % lambda == 0) {

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


            
            final double[] maxError = {Double.NEGATIVE_INFINITY};
            short _maxErrorNeighbour[] = {-1};
            edges.edgesOf(maxErrorID, (otherNodeID) -> {

                Centroid otherNode = this.centroids[otherNodeID];

                if (otherNode.localError() > maxError[0]) {
                    _maxErrorNeighbour[0] = otherNodeID;
                    maxError[0] = otherNode.localError();
                }
            });

            if (_maxErrorNeighbour[0] != -1) {

                short maxErrorNeighborID = _maxErrorNeighbour[0];

                
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


        

        
        for (Centroid n : this.centroids) {
            n.mulLocalError(beta);
        }









        

        

        return node(closest);
    }

    public void randomizeCentroid(double[] r, N newNode) {
        for (int i = 0; i < dimension; i++)
            newNode.randomizeUniform(i, r[i*2], r[i*2+1]);
    }

    public void range(double[] coord, float adapt) {
        int dim = coord.length;
        int k = 0;



        for (int d = 0; d < dim; d++) {
            double c = coord[d];

            double curMin = rangeMinMax[k];

                rangeMinMax[k] = ((curMin != curMin) || (curMin > c)) ? c : Util.lerp(adapt, curMin, c);

            k++;

            double curMax = rangeMinMax[k];

                rangeMinMax[k] = ((curMax != curMax) || (curMax < c)) ? c : Util.lerp(adapt, curMax, c);
            k++;
        }

    }


    public final N node(int i) {
        return (N) centroids[i];
    }

    private short randomNode() {
        return (short) (Math.random() * centroids.length);

















    }

    /**
     * called before a node will be removed
     */
    protected void removed(N furthest) {

    }

    public Stream<N> nodeStream() {
        return Stream.of(centroids).map(n -> (N) n);
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

    




























}





















































































