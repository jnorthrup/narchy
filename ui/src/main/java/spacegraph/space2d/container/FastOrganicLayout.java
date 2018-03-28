//package spacegraph.layout;
//
//import nars.gui.ConceptWidget;
//import org.apache.commons.lang3.mutable.MutableFloat;
//import spacegraph.*;
//import spacegraph.obj.EDraw;
//
//import java.awt.*;
//
///**
// * Fast organic layout algorithm, adapted from JGraph
// */
//public class FastOrganicLayout<X> implements SpaceTransform<X> {
//
////    @Range(min = 0, max = 1f)
////    public final MutableFloat nodeSpeed = new MutableFloat(0.02);
//
//    /**
//     * Specifies if the top left corner of the input cells should be the origin
//     * of the layout result. Default is true.
//     */
//    protected boolean useInputOrigin;
//
//    /**
//     * Specifies if all edge points of traversed edge should be removed.
//     * Default is true.
//     */
//    protected boolean resetEdges = true;
//
//
//    /**
//     * The force constant by which the attractive forces are divided and the
//     * replusive forces are multiple by the square of. The value equates to the
//     * average radius there is of free space around each node. Default is 50.
//     */
//
//    public final MutableFloat forceConstant = new MutableFloat(24f);
//
////    @Range(min = 0.5f, max = 4f)
////    public final MutableFloat spacing = new MutableFloat(1f);
//
//
//
//
//    /**
//     * Minimal distance limit. Default is 2. Prevents of dividing by zero.
//     */
//    protected float minDistanceLimit;
//
//    /**
//     * The maximum distance between vertex, beyond which their repulsion no
//     * longer has an effect
//     */
//    protected float maxDistanceLimit;
//
//    /**
//     * Start value of temperature. Default is 200.
//     */
//    protected float initialTemp;
//
//    /**
//     * Temperature to limit displacement at later stages of layout.
//     */
//    protected float temperature;
//
//    /**
//     * Total number of iterations to run the layout though.
//     */
//    protected int maxIterations = 1;
//
//
//    /**
//     * An array of locally stored X co-ordinate displacements for the vertex.
//     */
//    protected float[][] disp;
//
//
//    /**
//     * An array of locally stored co-ordinate positions for the vertex.
//     */
//    protected float[][] cellLocation;
//
//    /**
//     * The approximate radius of each cell, nodes only.
//     */
//    protected float[] radius;
//
////    /**
////     * The approximate radius squared of each cell, nodes only.
////     */
////    protected float[] radiusSquared;
//
//
//    /**
//     * Local copy of cell neighbours.
//     */
//    protected short[][] neighbors;
//
//    /**
//     * final normalization step to center all nodes
//     */
//    private static final boolean center = false;
//
//    private float nodeSpeed = 0.6f;
//    private float radiusScale = 4f;
//
//    //private final FasterList<VDraw> cells = new FasterList<>();
//
//
//    /**
//     * Constructs a new fast organic layout for the specified graph.
//     */
//    public FastOrganicLayout() {
//
//
//        setInitialTemp(0.2f);
//        setMinDistanceLimit(3f);
//        setMaxDistanceLimit(200f);
//
//    }
//
//
//    /**
//     *
//     */
//    public boolean isUseInputOrigin() {
//        return useInputOrigin;
//    }
//
//    /**
//     * @param value
//     */
//    public void setUseInputOrigin(boolean value) {
//        useInputOrigin = value;
//    }
//
//    /**
//     *
//     */
//    public boolean isResetEdges() {
//        return resetEdges;
//    }
//
//    /**
//     * @param value
//     */
//    public void setResetEdges(boolean value) {
//        resetEdges = value;
//    }
//
//
//    /**
//     *
//     */
//    public float getMaxIterations() {
//        return maxIterations;
//    }
//
//    /**
//     * @param value
//     */
//    public void setMaxIterations(int value) {
//        maxIterations = value;
//    }
//
//
//    /**
//     *
//     */
//    public float getMinDistanceLimit() {
//        return minDistanceLimit;
//    }
//
//    /**
//     * @param value
//     */
//    public void setMinDistanceLimit(float value) {
//        minDistanceLimit = value;
//    }
//
//    /**
//     * @return the maxDistanceLimit
//     */
//    public float getMaxDistanceLimit() {
//        return maxDistanceLimit;
//    }
//
//    /**
//     * @param maxDistanceLimit the maxDistanceLimit to set
//     */
//    public void setMaxDistanceLimit(float maxDistanceLimit) {
//        this.maxDistanceLimit = maxDistanceLimit;
//    }
//
//    /**
//     *
//     */
//    public float getInitialTemp() {
//        return initialTemp;
//    }
//
//    /**
//     * @param value
//     */
//    public void setInitialTemp(float value) {
//        initialTemp = value;
//    }
//
//    /**
//     * Reduces the temperature of the layout from an initial setting in a linear
//     * fashion to zero.
//     */
//    protected void reduceTemperature(int iteration) {
//        temperature = initialTemp * (1.0f - iteration / maxIterations);
//    }
//
//    @Override
//    public void update(SpaceGraph<X> g, AbstractSpace<X, Spatial<X>> src, float dt) {
//
//        int n = src.size();
//
//        //? graph.getBoundsForCells(vertexArray, false, false, true) : null;
//
//
//
//
//        if ((cellLocation == null) || (cellLocation.length != n)) {
//            disp = new float[n][2];
//            cellLocation = new float[n][2];
//            //if (neighbors == null || neighbors.length<n)
//            neighbors = new short[n][];
//            radius = new float[n];
//            //radiusSquared = new float[n];
//        }
//
//        // Create a map of vertex first. This is required for the array of
//        // arrays called neighbours which holds, for each vertex, a list of
//        // ints which represents the neighbours cells to that vertex as
//        // the indices into vertexArray
//
//        //final float spacing = this.spacing.get();
//
//        short[][] neighbors = this.neighbors;
//
//        float[][] cl = this.cellLocation;
//
//        for (int ii = 0; ii < n; ii++) {
//            final int i = ii;
//
//            ConceptWidget V = (ConceptWidget)src.get(i); //HACK
//
//            //TODO is this necessary?
//            /*if (!graph.containsVertex(vd.getVertex()))
//                continue;*/
//
//            /*if (vd == null) {
//                vd = new VDraw(vertex);
//                displayed.put(vertex, vd);
//            }*/
//
//            // Set up the mapping from array indices to cells
//
//            //mxRectangle bounds = getVertexBounds(vertex);
//
//            // Set the X,Y value of the internal version of the cell to
//            // the center point of the vertex for better positioning
//
//            float vr = radius[i] = V.radius * radiusScale; /*getRadius()*/
//            float width = vr * 2f; //bounds.getWidth();
//            float height = width; //vr * 2f; //bounds.getHeight();
//
//
//            // Randomize (0, 0) locations
//            //TODO re-use existing location
//
//
//            final float x = V.x();
//            final float y = V.y();
//
//            float[] cli = cl[i];
//            cli[0] = x + width / 2f;
//            cli[1] = y + height / 2f;
//
//            // Moves cell location back to top-left from center locations used in
//            // algorithm, resetting the edge points is part of the transaction
//
//
//            disp[i][0] = 0;
//            disp[i][1] = 0;
//
//            // Get lists of neighbours to all vertex, translate the cells
//            // obtained in indices into vertexArray and store as an array
//            // against the original cell index
//            //VDraw VDraw = vertexArray.get(i).getVertex();
//            //ProcessingGraphCanvas.VDraw vd = displayed.get(v);
//
//
//            //TODO why does a vertex disappear from the graph... make this unnecessary
//
//
//            //Set<E> edges = vd.getEdges();
//            java.util.List<EDraw> edges = V.edges;
//            int ne = V.edgeCount();
//
//
//            short[] ni = neighbors[i];
//            if (ni == null || ni.length!=ne) {
//                neighbors[i] = ni = new short[ne];
//            }
//
//            for (int j = 0; j < ne; j++) {
//
//                ni[j] = edges.get(j).target.order; //THIS CAN CAUSE A PROBLEM
//
//
//                // Check the connected cell in part of the vertex list to be
//                // acted on by this layout
//                // Else if index of the other cell doesn't correspond to
//                // any cell listed to be acted upon in this layout. Set
//                // the index to the value of this vertex (a dummy self-loop)
//                // so the attraction force of the edge is not calculated
//
//            }
//        }
//
//        temperature = initialTemp;
//
//        // Main iteration loop
//
//        for (int iteration = 0; iteration < maxIterations; iteration++) {
//            calcRepulsion(); // Calculate repulsive forces on all vertex
//            calcAttraction(); // Calculate attractive forces through edge
//            calcPositions();
//            reduceTemperature(iteration);
//        }
//
//        float minx = 0, miny = 0, maxx = 0, maxy = 0;
//
//        //float speed = nodeSpeed.floatValue();
//
//        float[] radius = this.radius;
//
//        for (int i = 0; i < n; i++) {
//            ConceptWidget vd = (ConceptWidget) src.get(i); //HACK
//            float[] ci = cl[i];
//
//            //cellLocation[i][0] -= 1/2.0; //geo.getWidth() / 2.0;
//            //cellLocation[i][1] -= 1/2.0; //geo.getHeight() / 2.0;
//
//
//            float r = (float) radius[i];
//
//            float x = /*graph.snap*/(ci[0] - r);
//            float y = /*graph.snap*/(ci[1] - r);
//
//
//            vd.move((float) x, (float) y, 0, nodeSpeed);
//
//            if (i == 0) {
//                minx = maxx = x;
//                miny = maxy = y;
//            } else {
//                if (x < minx) minx = x;
//                if (y < miny) miny = y;
//                if (x > maxx) maxx = x;
//                if (y > maxy) maxy = y;
//            }
//        }
//
//        if (center) {
//            // Modifies the cloned geometries in-place. Not needed
//            // to clone the geometries again as we're in the same
//            // undoable change.
//            float dx = -(maxx + minx) / 2f;
//            float dy = -(maxy + miny) / 2f;
//
//            Rectangle initialBounds = null; //new mxRectangle(-100, -50, 100, 50);
//            if (initialBounds != null) {
//                dx += initialBounds.getX();
//                dy += initialBounds.getY();
//            }
//
//            for (int i = 0; i < n; i++) {
//                ((ConceptWidget)src.get(i)).moveDelta((float) dx, (float) dy, 0);
//            }
//        }
//    }
//
//    /**
//     * Takes the displacements calculated for each cell and applies them to the
//     * local cache of cell positions. Limits the displacement to the current
//     * temperature.
//     */
//    protected void calcPositions() {
//        float temperature = this.temperature;
//        float tempSq = temperature * temperature;
//
//        float[][] disp = this.disp;
//        int n = disp.length;
//
//        for (int index = 0; index < n; index++) {
//            // Get the distance of displacement for this node for this
//            // iteration
//
//            float[] displacement = disp[index];
//
//            float xind = displacement[0];
//            float yind = displacement[1];
//            // reset displacements
//            displacement[0] = 0;
//            displacement[1] = 0;
//
//            float deltaLengthSq = (xind * xind + yind * yind);
//
//            // Normalize and scale the displacement vector if larger than max speed (temperature)
//            float dtemp;
//            if (deltaLengthSq < tempSq) {
//                dtemp = 1;
//            } else {
//                //if (deltaLengthSq < tempSq) {
//                    //dtemp = temperature;
//                //} else {
//                    dtemp = temperature / (float)Math.sqrt(deltaLengthSq);
//                //}
//            }
//
//            // Update the cached cell locations
//            float[] ci = cellLocation[index];
//            ci[0] += xind * dtemp;
//            ci[1] += yind * dtemp;
//
//        }
//    }
//
//    /**
//     * Calculates the attractive forces between all laid out nodes linked by
//     * edge
//     */
//    protected void calcAttraction() {
//        // Check the neighbours of each vertex and calculate the attractive
//        // force of the edge connecting them
//
//        final float forceConstant = this.forceConstant.floatValue();
//        //final float spacing = this.spacing.floatValue();
//
//        float[][] cl = this.cellLocation;
//        int n = cl.length;
//
//        //float[] radiusSquared = this.radiusSquared;
//        short[][] neighbors = this.neighbors;
//
//        float minDistanceLimit = this.minDistanceLimit;
//        float minDistanceLimitSq = minDistanceLimit * minDistanceLimit;
//
//        int numNeighbors = neighbors.length;
//
//        float[][] disp = this.disp;
//        float[] radius = this.radius;
//
//        for (int i = 0; i < numNeighbors; i++) {
//
//            short[] neighbor = neighbors[i];
//
//            if (neighbor == null || cl[i] == null)
//                continue;
//
//            float ir = radius[i];
//
//            for (int nj = 0; nj < neighbor.length; nj++) {
//                // Get the index of the othe cell in the vertex array
//                int j = neighbor[nj];
//
//
//                if (j < 0 || j >= n) {
//                    //System.err.println("vert " + i + ": " + Arrays.toString(neighbor) + " wtf " + n);
//                    continue;
//                }
//                if (i == j)
//                    continue; // Do not proceed self-loops
//
//
//                float[] clj = cl[j];
//                float[] cli = cl[i];
//                float rr = radius[j] + ir;
//                float xDelta = cli[0] - clj[0];
//                float yDelta = cli[1] - clj[1];
//
//                // The distance between the nodes
//                float distSq = (xDelta * xDelta + yDelta * yDelta);
//                    // - (spacing * (radiusSquared[i] + radiusSquared[j]));
//
//                if (distSq < minDistanceLimitSq /*-rr*rr*/)
//                    continue;
//
//                distSq = Math.max(0f, (float)Math.sqrt(distSq)-rr); //subtract radius
//
//                if (distSq < this.minDistanceLimit)
//                    continue;
//
//                float force = distSq / forceConstant;
//
//                float displacementX = (xDelta) * force;
//                float displacementY = (yDelta) * force;
//
//
//                disp[i][0] -= displacementX;
//                disp[i][1] -= displacementY;
//                disp[j][0] += displacementX;
//                disp[j][1] += displacementY;
//
//            }
//        }
//    }
//
//    /**
//     * Calculates the repulsive forces between all laid out nodes
//     */
//    protected void calcRepulsion() {
//
//        //float[] radius = this.radius;
//        float[][] disp = this.disp;
//
//        float[][] cl = this.cellLocation;
//        final int vertexCount = cl.length;
//
//        float minDistanceLimitSq = minDistanceLimit*minDistanceLimit;
//        float maxDistanceLimitSq = maxDistanceLimit*maxDistanceLimit;
//
//
//        final float forceConstantSq = this.forceConstant.floatValue()*this.forceConstant.floatValue();
//
//        for (int i = 0; i < vertexCount; i++) {
//
//            float[] ci = cl[i];
//
//            for (int j = i; j < vertexCount; j++) {
//
//                float[] cj = cl[j];
//
//                if ((ci != null) && (cj != null)) {
//                    float xDelta = ci[0] - cj[0];
//                    float yDelta = ci[1] - cj[1];
//
////                    if (xDelta == 0) {
////                        xDelta = movementThreshold*2f * Math.random();
////                    }
////
////                    if (yDelta == 0) {
////                        yDelta = movementThreshold*2f * Math.random();
////                    }
//
//                    // Distance between nodes
//                    float deltaLength = ((xDelta * xDelta) + (yDelta * yDelta));
//
//
////                    float deltaLengthWithRadius = deltaLength - radius[i]
////                            - radius[j];
//
//                    if (deltaLength> maxDistanceLimitSq) {
//                        // Ignore vertex too far apart
//                        continue;
//                    }
//
//                    if (deltaLength < minDistanceLimitSq) {
//                        deltaLength = minDistanceLimitSq;
//                    } else {
//                        deltaLength = /*Math.sqrt*/(deltaLength);
//                    }
//
//                    float force = forceConstantSq / (deltaLength/*Sq*/);
//
//                    float displacementX = (xDelta /*/ deltaLength*/) * force;
//                    float displacementY = (yDelta /*/ deltaLength*/) * force;
//
//                    disp[i][0] += displacementX;
//                    disp[i][1] += displacementY;
//                    disp[j][0] -= displacementX;
//                    disp[j][1] -= displacementY;
//                }
//            }
//        }
//    }
//
//
//}