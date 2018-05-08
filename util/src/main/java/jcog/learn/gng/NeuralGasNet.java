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
 * from: https://github.com/scadgek/NeuralGas
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
                //new SemiDenseShortUndirectedGraph((short) maxNodes);
                new DenseIntUndirectedGraph((short) centroids);

        this.centroids = new Centroid[centroids];

        this.rangeMinMax = new double[dimension * 2];
        Arrays.fill(rangeMinMax, Float.NaN);
        this.rangeAdaptRate = 1f /(1f + centroids);

        this.distanceSq = distanceSq != null ? distanceSq : Centroid.DistanceFunction::distanceCartesianSq;


        this.iteration = 0;
        this.dimension = dimension;
        this.maxNodes = centroids;


        //default values
        setLambda(centroids*2);
        setMaxEdgeAge(centroids*2);

        setAlpha(0.8);
        setBeta(0.9);

        setWinnerUpdateRate(0.5f / centroids, 0.25f / centroids);


        /** nodes should begin with randomized coordinates */
        for (int i = 0; i < centroids; i++) {
            this.centroids[i] = newCentroid(i, dimension);
        }


//        pw = new PrintWriter("resources/output.txt");
//
//        for (Node node : nodes)
//        {
//            pw.println(node.getWeights()[0] + " " + node.getWeights()[1] + " " + node.getWeights()[2] + " black");
//        }
//        pw.println("*");
    }

    public void forEachNode(Consumer<N> each) {
        for (Centroid n : centroids)
            each.accept((N) n);
    }

    public void clear() {
        edges.clear();
        //Arrays.fill(node, null);
    }

    @NotNull
    public N newCentroid(int i, int dims) {
        return (N) new Centroid(i, dims);
    }

    public N closest(double[] x) {
        //find closest nodes
        double minDistSq = Double.POSITIVE_INFINITY;
        Centroid closest = null;

        //TODO iterate fairly in case equal distance
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
            return null; //not initialized yet HACK

        //find closest nodes
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
            double dd = n.localDistanceSq(); //TODO cache this localDist
            if (dd < minDist2) {
                nextClosestNode = j;
                minDist2 = dd;
            }
        }



        if (closest == -1) {
            //throw new RuntimeException("closest=" + closest + ", nextClosest=" + nextClosestNode);
            //return lastNode;
            return null;
        }

        assert (closest != nextClosestNode);

        //update local error of the "winner"
        this.centroids[closest].updateLocalError(x, winnerUpdateRate);

        //update weights for "winner"'s neighbours
        short sc = closest;
        edges.edgesOf(closest, (connection, age) -> {
            this.centroids[connection].update(x, winnerNeighborUpdateRate);
        });
        edges.addToEdges(sc, -1);


        //remove connections with age > maxAge
        //edges.removeEdgeIf(c -> c <= 0);

        //reset connection between "winners" to new
        if (nextClosestNode != -1)
            edges.setEdge(closest, nextClosestNode, ttl);


        //if iteration is lambda
        if (iteration++ % lambda == 0) {

            edges.removeVertex(furthest);
            removed((N) this.centroids[furthest]);

            //find node with maximal local error

            short maxErrorID = -1;
            {
                double maxError = Double.NEGATIVE_INFINITY;
                for (int i = 0, nodeLength = this.centroids.length; i < nodeLength; i++) {
                    Centroid n = this.centroids[i];
                    if (i == furthest)
                        continue; //skip furthest which was supposed to be removed, and will be replaced below
                    if (n.localError() > maxError) {
                        maxErrorID = (short) i;
                        maxError = n.localError();
                    }
                }

                if (maxErrorID == -1) {
                    throw new RuntimeException("maxErrorID=null");
                }
            }


            //if (e has edges for maxErrorID..)
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

                //remove connection between them
                edges.removeEdge(maxErrorID, maxErrorNeighborID);

                //System.out.println("creating new node " + nextID + " in: " + node);

                //create node between errorest nodes
                N newNode = newCentroid(furthest, dimension);
                randomizeCentroid(rangeMinMax, newNode);


                Centroid maxErrorNeighbor = this.centroids[maxErrorNeighborID];
                Centroid maxErrorNode = this.centroids[maxErrorID];
                newNode.set(maxErrorNode, maxErrorNeighbor);
                this.centroids[furthest] = newNode;

                if (maxErrorID == furthest) {
                    throw new RuntimeException("new node has same id as max error node");
                }

                //create connections between them
                edges.setEdge(maxErrorID, furthest, ttl);
                edges.setEdge(maxErrorNeighborID, furthest, ttl);

                //update errors of the error nodes
                maxErrorNode.mulLocalError(alpha);
                maxErrorNeighbor.mulLocalError(alpha);
            }
        }


        //System.out.println(node.size() + " nodes, " + edgeSet().size() + " edges in neuralgasnet");

        //update errors of the nodes
        for (Centroid n : this.centroids) {
            n.mulLocalError(beta);
        }

//            //save positions
//            for (Node node : nodes)
//            {
//                pw.println(node.getWeights()[0] + " " + node.getWeights()[1] + " " + node.getWeights()[2] + " black");
//            }
//            pw.println("*");


        //System.out.println("Iteration: " + iteration++);

        //pw.close();

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
//        Set<N> vv = node;
//        int vs = vv.size();
//        if (vs < 2)
//            throw new UnsupportedOperationException();
//
//
//        while (true) { //HACK
//            int n = (int) Math.floor(Math.random() * vs);
//            Iterator<N> vi = vv.iterator();
//            while (vi.hasNext()) {
//                N v = vi.next();
//                if (n-- == 0) {
//                    if (v != except)
//                        return v;
//                }
//            }
//        }
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

    //    private void addEdge(Connection<N> connection) {
//
//        addEdge(connection.from, connection.to, connection);
//    }

//    public double[] getDimensionRange(final int dimension) {
//        final double[] x = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
//
//        for (Node node : node) {
//            double v = node.getEntry(dimension);
//            if (v < x[0]) x[0] = v;
//            if (v > x[1]) x[1] = v;
//        }
//
//        return x;
//    }

//    /**
//     * pulls a dimension out of all the nodes, as an array, and sorts it
//     */
//    public double[] getDimension(int dim) {
//        double[] d = new double[node.size()];
//        int i = 0;
//        for (Node n : node) {
//            d[i++] = n.getEntry(dim);
//        }
//        Arrays.sort(d);
//        return d;
//    }
}

//package nars.rl.gng;
//
//import org.xml.sax.SAXException;
//
//import javax.xml.parsers.ParserConfigurationException;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class Main {
//
//    private List<Node> nodes;
//    private List<Connection> connections;
//
//    public Main() {
//        nodes = new ArrayList<Node>();
//        connections = new ArrayList<Connection>();
//    }
//
//    public List<Node> getNodes() {
//        return nodes;
//    }
//
//    public void setNodes(List<Node> nodes) {
//        this.nodes = nodes;
//    }
//
//    public List<Connection> getConnections() {
//        return connections;
//    }
//
//    public void setConnections(List<Connection> connections) {
//        this.connections = connections;
//    }
//
//    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
//        NeuralGasNet network = new NeuralGasNet(new Config("resources/config.xml"));
//        network.learn(new Dataset());
//
//        System.out.println("Learning finished");
//        for (Node node : network.nodes)
//        {
//            System.out.println(node.getWeights()[0]+ " " + node.getWeights()[1] + " " + node.getWeights()[2]);
//        }
//
////        Main main = new Main();
////
////        Random random = new Random();
////
////        Node node1 = new Node();
////        node1.setWeights(new double[]{random.nextDouble(), random.nextDouble(), random.nextDouble()});
////        node1.setLocalError(0);
////
////        Node node2 = new Node();
////        node2.setWeights(new double[]{random.nextDouble(), random.nextDouble(), random.nextDouble()});
////        node2.setLocalError(0);
////
////        main.getNodes().add(node1);
////        main.getNodes().add(node2);
////
////        double[] x = {random.nextDouble(), random.nextDouble(), random.nextDouble()};
////        Node closestNode = main.getNodes().get(0);
////        Node secondClosestNode = main.getNodes().get(0);
////        double min = Double.MAX_VALUE;
////        for (Node node : main.getNodes()) {
////            if (main.distance(x, node.getWeights()) < min) {
////                secondClosestNode = closestNode;
////                closestNode = node;
////            }
////        }
////
////        closestNode.setLocalError(closestNode.getLocalError() + main.distance(x, closestNode.getWeights()));
////
////
////        System.out.println();
//    }
//
//    public double distance(double[] x, double[] y) {
//        double coord1Diff = Math.pow(x[0] - y[0], 2);
//        double coord2Diff = Math.pow(x[1] - y[1], 2);
//        double coord3Diff = Math.pow(x[2] - y[2], 2);
//        return Math.sqrt(coord1Diff + coord2Diff + coord3Diff);
//    }
//}
