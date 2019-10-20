package spacegraph.space2d.phys.fracture.poly2Tri;

import jcog.math.v2;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import spacegraph.space2d.phys.fracture.poly2Tri.splayTree.BTreeNode;
import spacegraph.space2d.phys.fracture.poly2Tri.splayTree.SplayTree;
import spacegraph.space2d.phys.fracture.poly2Tri.splayTree.SplayTreeAction;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;

/**
 * Merged with BDMFile (Boundary Mesh File)
 */
class Polygon {

    /**
     * vector<unsigned int>    _nVertices;   
     */
    private int[] _nVertices = null;

    /**
     * typedef map<unsigned int, Pointbase*>           PointbaseMap;
     * all vertices ... is needed as normal array,
     * map in C++ code probably because od adding into map
     * ... see _pointsKeys
     */
    private final IntObjectHashMap _points = new IntObjectHashMap();

    /**
     * Initialized in initialize() method ... number of points
     * doesn't change during iteration - all iteration in C++
     * code are done from smaller to bigger ... HashMap.keySet().iterator()
     * isn't returning keys in natural order.
     */
    private int[] _pointsKeys = null;

    /**
     * typedef map<unsigned int, Linebase*>            LineMap;
     * all edges
     */
    private final IntObjectHashMap<Linebase> _edges = new IntObjectHashMap<>(0);

    /**
     * typedef priority_queue<Pointbase> PQueue; ... use PointbaseComparatorCoordinatesReverse! (Jakub Gemrot)
     * priority queue for event points
     */
    private final PriorityQueue _qpoints = new PriorityQueue(30, new PointbaseComparatorCoordinatesReverse());

    /**
     * typedef SplayTree<Linebase*, double>            EdgeBST;
     * edge binary searching tree (splaytree)
     */
    private final SplayTree _edgebst = new SplayTree();

    /**
     * typedef list<Monopoly>          Monopolys;
     * typedef list<unsigned int>      Monopoly;
     * all monotone polygon piece list;
     */
    private final List _mpolys = new ArrayList();

    /**
     * all triangle list;
     * typedef list<Triangle>                          Triangles;
     * typedef vector<unsigned int>                    Triangle;
     */
    private final ArrayList<int[]> _triangles = new ArrayList<>();

    /**
     * typedef map<unsigned int, setAt<unsigned int> >   AdjEdgeMap;
     * data for monotone piece searching purpose;
     */
    private final IntObjectHashMap<IntHashSet> _startAdjEdgeMap = new IntObjectHashMap<>(0);

    /**
     * typedef map<unsigned int, Linebase*>            LineMap;
     * added diagonals to partition polygon to
     * monotont pieces, not all diagonals of
     * given polygon
     */
    private final IntObjectHashMap<Linebase> _diagonals = new IntObjectHashMap<>(0);

    /**
     * debug option;
     */
    private boolean _debug = false;

    /**
     * log file for debug purpose;
     */
    private final FileWriter _logfile = null;

    /**
     * This is used to change key of all items in SplayTree.
     */
    private final SplayTreeAction updateKey = new UpdateKey();

    /**
     * If _debug == true, file with this name will be used to log the messages.
     */
    

    public IntObjectHashMap points() {
        return _points;
    }

    public IntObjectHashMap<Linebase> edges() {
        return _edges;
    }

    /**
     * For params see contructor Polygon(int, int[], double[][])
     *
     * @param numContours
     * @param numVerticesInContures
     * @param vertices              ---{ CLEAR }---
     */
    private void initPolygon(int numContours, int[] numVerticesInContours, v2[] vertices) {

        /**
         * Was unsigned int!
         * Number of contours.
         */
        _nVertices = new int[numContours];
        var nextNumber = 1;
        int j;
        int i;
        for (i = 0; i < numContours; ++i) {
            for (j = 0; j < numVerticesInContours[i]; ++j) {
                _points.put(nextNumber, new Pointbase(nextNumber, vertices[nextNumber - 1].x, vertices[nextNumber - 1].y, Poly2TriUtils.INPUT));
                ++nextNumber;
            }
        }
        _nVertices[0] = numVerticesInContours[0];
        for (i = 1; i < numContours; ++i) {
            _nVertices[i] = _nVertices[i - 1] + numVerticesInContours[i];
        }
        i = 0;
        j = 1;
        var first = 1;

        while (i < numContours) {
            Linebase edge;
            for (; j + 1 <= _nVertices[i]; ++j) {
                edge = new Linebase((Pointbase) _points.get(j), (Pointbase) _points.get(j + 1), Poly2TriUtils.INPUT);
                _edges.put(Poly2TriUtils.l_id.get(), edge);
            }
            edge = new Linebase((Pointbase) _points.get(j), (Pointbase) _points.get(first), Poly2TriUtils.INPUT);
            _edges.put(Poly2TriUtils.l_id.get(), edge);

            j = _nVertices[i] + 1;
            first = _nVertices[i] + 1;
            ++i;
        }
        Poly2TriUtils.p_id.set(_nVertices[numContours - 1]);
    }

    /**
     * numContures == number of contures of polygon (1 OUTER + n INNER)
     * numVerticesInContures == array numVerticesInContures[x] == number of vertices in x. contures, 0-based
     * vertices == array of vertices, each item of array contains doubl[2] ~ {x,y}
     * First conture is OUTER -> vertices must be COUNTER CLOCKWISE!
     * Other contures must be INNER -> vertices must be CLOCKWISE!
     * Example:
     * numContures = 1 (1 OUTER CONTURE, 1 INNER CONTURE)
     * numVerticesInContures = { 3, 3 } 
     * vertices = { {0, 0}, {7, 0}, {4, 4}, 
     * {2, 2}, {2, 3}, {3, 3}  
     * }
     *
     * @param numContures           number of contures of polygon (1 OUTER + n INNER)
     * @param numVerticesInContures array numVerticesInContures[x] == number of vertices in x. contures, 0-based
     * @param vertices              array of vertices, each item of array contains doubl[2] ~ {x,y}
     */
    Polygon(int numContures, int[] numVerticesInContures, v2[] vertices) {
        Poly2TriUtils.l_id.set(0);
        Poly2TriUtils.p_id.set(0);
        initPolygon(numContures, numVerticesInContures, vertices);
        initializate();
        _debug = false;
    }

    public void writeToLog(String s) {
        if (!_debug) return;
        try {
            _logfile.write(s);
        } catch (IOException e) {
            _debug = false;
            System.out.println("Writing to LogFile (debugging) failed.");
            e.printStackTrace();
            System.out.println("Setting _debug = false, continuing the work.");
        }
    }

    private Pointbase getPoint(int index) {
        return (Pointbase) _points.get(index);
    }

    private Linebase getEdge(int index) {
        return _edges.get(index);
    }

    private Pointbase qpointsTop() {
        return (Pointbase) _qpoints.peek();
    }

    private Pointbase qpointsPop() {
        return (Pointbase) _qpoints.poll();
    }

    public void destroy() {
    }

    public boolean is_exist(double x, double y) {
        var iter = _points.keySet().intIterator();
        while (iter.hasNext()) {
            var pb = getPoint(iter.next());
            if ((pb.x == x) && (pb.y == y)) return true;
        }
        return false;
    }

    /**
     * return the previous point (or edge) id for a given ith point (or edge);
     * <p>
     * was all UNSIGNED (ints)
     */
    private int prev(int i) {
        int prevLoop = 0, currentLoop = 0;

        while (i > _nVertices[currentLoop]) {
            prevLoop = currentLoop;
            currentLoop++;
        }

        var j = 0;
        if (i == 1 || (i == _nVertices[prevLoop] + 1)) j = _nVertices[currentLoop];
        else if (i <= _nVertices[currentLoop]) j = i - 1;

        return j;
    }

    /**
     * return the next point (or edge) id for a given ith point (or edge);
     * was all UNSIGNED!
     */
    private int next(int i) {
        int prevLoop = 0, currentLoop = 0;

        while (i > _nVertices[currentLoop]) {
            prevLoop = currentLoop;
            currentLoop++;
        }

        var j = 0;
        if (i < _nVertices[currentLoop]) j = i + 1;
        else if (i == _nVertices[currentLoop]) {
            if (currentLoop == 0) j = 1;
            else j = _nVertices[prevLoop] + 1;
        }

        return j;
    }

    /**
     * rotate input polygon by angle theta, not used;
     */
    private void rotate(double theta) {
        for (var _pointsKey : _pointsKeys) (getPoint(_pointsKey)).rotate(theta);
    }













    private void initializePointsKeys() {
        _pointsKeys = _points.keySet().toSortedArray();
    }

    private void initializeEdgesKeys() {
        /**
         * See _pointsKeys ... same for _edges.
         * ---{ Today, it's you }---
         * Terry Pratchett's Death
         * Right now I'm not sure wether number of edges can't change...
         * ... better call initializeEdgesKeys() all the time ;)
         */
        var _edgesKeys = _edges.keySet().toSortedArray();
    }

    private IntHashSet getSetFromStartAdjEdgeMap(int index) {
        return _startAdjEdgeMap.getIfAbsentPut(index, ()->new IntHashSet(0));
    }

    /**
     * polygon initialization;
     * to find types of all polygon vertices;
     * create a priority queue for all vertices;
     * construct an edge set for each vertex (the set holds all edges starting from
     * the vertex, only for loop searching purpose).
     * <p>
     * ---{ CLEAR }---
     */
    private void initializate() {
        initializePointsKeys();

        for (var _pointsKey : _pointsKeys) {
            var id = _pointsKey;
            var idp = prev(id);
            var idn = next(id);

            var p = getPoint(id);
            var pnext = getPoint(idn);
            var pprev = getPoint(idp);

            if ((p.compareTo(pnext) > 0) && (pprev.compareTo(p) > 0))
                p.type = Poly2TriUtils.REGULAR_DOWN;
            else if ((p.compareTo(pprev) > 0) && (pnext.compareTo(p) > 0))
                p.type = Poly2TriUtils.REGULAR_UP;
            else {
                var area = Poly2TriUtils.orient2d(new double[]{pprev.x, pprev.y},
                        new double[]{p.x, p.y},
                        new double[]{pnext.x, pnext.y});

                if ((pprev.compareTo(p) > 0) && (pnext.compareTo(p) > 0))
                    p.type = (area > 0) ? Poly2TriUtils.END : Poly2TriUtils.MERGE;
                if ((pprev.compareTo(p) < 0) && (pnext.compareTo(p) < 0))
                    p.type = (area > 0) ? Poly2TriUtils.START : Poly2TriUtils.SPLIT;
            }

            
            
            _qpoints.add(new Pointbase(p));

            getSetFromStartAdjEdgeMap(id).add(id);
        }
    }

    /**
     * Add a diagonal from point id i to j
     * <p>
     * C++ code: was all unsigned (i,j)
     */
    private void addDiagonal(int i, int j) {
        var type = Poly2TriUtils.INSERT;

        var diag = new Linebase(getPoint(i),
                getPoint(j),
                type);
        _edges.put(diag.id(), diag);

        getSetFromStartAdjEdgeMap(i).add(diag.id());
        getSetFromStartAdjEdgeMap(j).add(diag.id());

        _diagonals.put(diag.id(), diag);

        
    }

    /**
     * handle event vertext according to vertex type;
     * Handle start vertex
     * <p>
     * C++ code: was all UNSIGNED
     */
    private void handleStartVertex(int i) {

        var y = ((Pointbase) _points.get(i)).y;

        _edgebst.inOrder(updateKey, y);

        var edge = getEdge(i);
        if (edge != null) {
            edge.setHelper(i);
            edge.setKeyValue(y);

            _edgebst.insert(edge);
        }

        if (_debug) {
            
            
            
        }
    }

    /**
     * handle event vertext according to vertex type;
     * Handle end vertex
     * <p>
     * C++ code: param i was unsigned
     */
    private void handleEndVertex(int i) {
        var y = getPoint(i).y;

        _edgebst.inOrder(updateKey, y);

        var previ = prev(i);
        var edge = getEdge(previ);
        if (edge != null) {
            var helper = edge.helper();

            if (getPoint(helper).type == Poly2TriUtils.MERGE)
                addDiagonal(i, helper);
            _edgebst.delete(edge.keyValue());

            if (_debug) {
                
                
            }
        }
    }

    /**
     * handle event vertext according to vertex type;
     * Handle split vertex
     * C++ code: i was unsigned, helper was unsigned
     */
    private void handleSplitVertex(int i) {
        var point = getPoint(i);
        double x = point.x, y = point.y;

        _edgebst.inOrder(updateKey, y);

        var leftnode = _edgebst.findMaxSmallerThan(x);
        if (leftnode != null) {
            var leftedge = (Linebase) leftnode.data();

            var helper = leftedge.helper();
            addDiagonal(i, helper);

            if (_debug) {
                
                
                
                
                
                
            }

            leftedge.setHelper(i);

            var edge = getEdge(i);
            edge.setHelper(i);
            edge.setKeyValue(y);
            _edgebst.insert(edge);
        }
    }

    /**
     * handle event vertext according to vertex type;
     * Handle merge vertex
     * C++ code: i was unsigned, previ + helper also unsigned
     */
    private void handleMergeVertex(int i) {
        var point = getPoint(i);
        double x = point.x, y = point.y;

        _edgebst.inOrder(updateKey, y);

        var previ = prev(i);
        var previEdge = getEdge(previ);
        if (previEdge != null) {
            var helper = previEdge.helper();

            var helperPoint = getPoint(helper);

            if (helperPoint.type == Poly2TriUtils.MERGE)
                addDiagonal(i, helper);

            _edgebst.delete(previEdge.keyValue());

            if (_debug) {
                
                
            }

            var leftnode = _edgebst.findMaxSmallerThan(x);
            var leftedge = (Linebase) leftnode.data();

            helper = leftedge.helper();
            helperPoint = getPoint(helper);
            if (helperPoint.type == Poly2TriUtils.MERGE)
                addDiagonal(i, helper);

            leftedge.setHelper(i);

            if (_debug) {
                
                
                
            }
        }
    }

    /**
     * handle event vertext according to vertex type;
     * Handle regular down vertex
     * C++ code: i was unsigned, previ + helper also unsigned
     */
    private void handleRegularVertexDown(int i) {
        var point = getPoint(i);

        var y = point.y;

        _edgebst.inOrder(updateKey, y);

        var previ = prev(i);

        var previEdge = getEdge(previ);
        if (previEdge != null) {

            var helper = previEdge.helper();

            var helperPoint = getPoint(helper);

            if (helperPoint.type == Poly2TriUtils.MERGE)
                addDiagonal(i, helper);

            _edgebst.delete(previEdge.keyValue());

            var edge = getEdge(i);
            edge.setHelper(i);
            edge.setKeyValue(y);
            _edgebst.insert(edge);

            if (_debug) {
                
                
                
                
                
            }
        }
    }

    /**
     * handle event vertext according to vertex type;
     * Handle regular up vertex
     * C++ code: i was unsigned, helper also unsigned
     */
    private void handleRegularVertexUp(int i) {
        var point = getPoint(i);

        double x = point.x, y = point.y;

        _edgebst.inOrder(updateKey, y);

        var leftnode = _edgebst.findMaxSmallerThan(x);
        if (leftnode != null) {

            var leftedge = (Linebase) leftnode.data();

            var helper = leftedge.helper();
            var helperPoint = getPoint(helper);
            if (helperPoint.type == Poly2TriUtils.MERGE) addDiagonal(i, helper);
            leftedge.setHelper(i);

            if (_debug) {
                
                
                
            }

        }

    }

    /**
     * main member function for polygon triangulation;
     * partition polygon to monotone polygon pieces
     * C++ code: id was unsigned
     *
     * @return success
     */
    private boolean partition2Monotone() {
        if (qpointsTop().type != Poly2TriUtils.START) {
            System.out.println("Please check your input polygon:\n1)orientations?\n2)duplicated points?\n");
            System.out.println("poly2tri stopped.\n");
            return false;
        }

        while (!_qpoints.isEmpty()) {
            var vertex = qpointsPop();

            var id = vertex.id;

            if (_debug) {
                String stype;
                switch (vertex.type) {
                    case Poly2TriUtils.START:
                        stype = "START";
                        break;
                    case Poly2TriUtils.END:
                        stype = "END";
                        break;
                    case Poly2TriUtils.MERGE:
                        stype = "MERGE";
                        break;
                    case Poly2TriUtils.SPLIT:
                        stype = "SPLIT";
                        break;
                    case Poly2TriUtils.REGULAR_UP:
                        stype = "REGULAR_UP";
                        break;
                    case Poly2TriUtils.REGULAR_DOWN:
                        stype = "REGULAR_DOWN";
                        break;
                    default:
                        System.out.println("No duplicated points please! poly2tri stopped\n");
                        return false;
                }
                
            }

            switch (vertex.type) {
                case Poly2TriUtils.START:
                    handleStartVertex(id);
                    break;
                case Poly2TriUtils.END:
                    handleEndVertex(id);
                    break;
                case Poly2TriUtils.MERGE:
                    handleMergeVertex(id);
                    break;
                case Poly2TriUtils.SPLIT:
                    handleSplitVertex(id);
                    break;
                case Poly2TriUtils.REGULAR_UP:
                    handleRegularVertexUp(id);
                    break;
                case Poly2TriUtils.REGULAR_DOWN:
                    handleRegularVertexDown(id);
                    break;
                default:
                    System.out.println("No duplicated points please! poly2tri stopped\n");
                    return false;
            }
        }
        return true;
    }

    /**
     * angle ABC for three given points, for monotone polygon searching purpose;
     * calculate angle B for A, B, C three given points
     * auxiliary function to find monotone polygon pieces
     */
    private static double angleCosb(double[] pa, double[] pb, double[] pc) {
        var dxab = pa[0] - pb[0];
        var dyab = pa[1] - pb[1];

        var dxcb = pc[0] - pb[0];
        var dycb = pc[1] - pb[1];

        var dxab2 = dxab * dxab;
        var dyab2 = dyab * dyab;
        var dxcb2 = dxcb * dxcb;
        var dycb2 = dycb * dycb;
        var ab = dxab2 + dyab2;
        var cb = dxcb2 + dycb2;

        var cosb = dxab * dxcb + dyab * dycb;
        var denom = Math.sqrt(ab * cb);

        cosb /= denom;

        return cosb;
    }

    /**
     * find the next edge, for monotone polygon searching purpose;
     * for any given edge, find the next edge we should choose when searching for
     * monotone polygon pieces;
     * auxiliary function to find monotone polygon pieces
     * C++ code: return unsigned int, eid also unsigned, same for nexte_ccw, nexte_cw
     */
    private int selectNextEdge(Linebase edge) {
        var eid = edge.endPoint(1).id;
        var edges = getSetFromStartAdjEdgeMap(eid);

        var numEdges = edges.size();
        assert (numEdges != 0);

        var nexte = 0;

        if (numEdges == 1)
            nexte = (edges.intIterator().next());
        else {
            

            int nexte_ccw = 0, nexte_cw = 0;
            double max = -2.0, min = 2.0;

            IntIterator iter = edges.toSortedList().intIterator();
            while (iter.hasNext()) {
                var it = iter.next();
                if (it == edge.id()) continue;

                var iEdge = getEdge(it);

                double[] A = {0, 0};
                A[0] = edge.endPoint(0).x;
                A[1] = edge.endPoint(0).y;
                double[] B = {0, 0};
                B[0] = edge.endPoint(1).x;
                B[1] = edge.endPoint(1).y;

                if (!edge.endPoint(1).equals(iEdge.endPoint(0))) iEdge.reverse();
                double[] C = {0, 0};
                C[0] = iEdge.endPoint(1).x;
                C[1] = iEdge.endPoint(1).y;

                var area = Poly2TriUtils.orient2d(A, B, C);
                var cosb = angleCosb(A, B, C);

                if (area > 0 && max < cosb) {
                    nexte_ccw = it;
                    max = cosb;
                } else if (min > cosb) {
                    nexte_cw = it;
                    min = cosb;
                }
            }

            nexte = (nexte_ccw != 0) ? nexte_ccw : nexte_cw;
        }

        return nexte;
    }

    /**
     * searching all monotone pieces;
     * C++ code: unsigned - nexte
     *
     * @return success
     */
    private boolean searchMonotones() {
        var loop = 0;

        IntObjectHashMap<Linebase> edges = new IntObjectHashMap(_edges);

        int i;

        while (edges.size() > _diagonals.size()) {
            loop++;

            var poly = new ArrayList();

            var edgesKeys = edges.keySet().toSortedArray();

            var it = edgesKeys[0];
            var itEdge = edges.get(it);


            var startp = itEdge.endPoint(0);
            var next = itEdge;

            poly.add(startp.id);

            if (_debug) {
                
                
            }

            for (; ; ) {

                var endp = next.endPoint(1);

                if (next.type() != Poly2TriUtils.INSERT) {
                    edges.remove(next.id());
                    getSetFromStartAdjEdgeMap(next.endPoint(0).id).remove(next.id());
                }
                if (endp == startp) break;
                poly.add(endp.id);


                var nexte = selectNextEdge(next);

                if (nexte == 0) {
                    System.out.println("Please check your input polygon:\n");
                    System.out.println("1)orientations?\n2)with duplicated points?\n3)is a simple one?\n");
                    System.out.println("poly2tri stopped.\n");
                    return false;
                }

                next = edges.get(nexte);
                if (!(next.endPoint(0).equals(endp))) next.reverse();
            }

            

            _mpolys.add(poly);
        }
        return true;
    }

    /**
     * triangulate a monotone polygon piece;
     * void triangulateMonotone(Monopoly& mpoly);
     * Monopoly == list<Monopoly>
     */
    private void triangulateMonotone(ArrayList mpoly) {
        var qvertex = new PriorityQueue(30, new PointbaseComparatorCoordinatesReverse());


        for (var it = 0; it < mpoly.size(); it++) {
            var itnext = it + 1;
            if (itnext == mpoly.size()) itnext = 0;
            var point = new Pointbase(getPoint((Integer) mpoly.get(it)));
            var pointnext = new Pointbase(getPoint((Integer) mpoly.get(itnext)));
            point.left = point.compareTo(pointnext) > 0;
            qvertex.add(point);
        }

        var spoint = new Stack();

        for (var i = 0; i < 2; i++) spoint.push(qvertex.poll());

        double[] pa = {0, 0}, pb = {0, 0}, pc = {0, 0};
        int[] v;

        
        
        
        
        while (qvertex.size() > 1) {

            var topQueuePoint = (Pointbase) qvertex.peek();
            var topStackPoint = (Pointbase) spoint.peek();

            if (topQueuePoint.left != topStackPoint.left) {

                while (spoint.size() > 1) {

                    var p1 = (Pointbase) spoint.peek();
                    spoint.pop();
                    var p2 = (Pointbase) spoint.peek();


                    v = new int[]{
                            (topQueuePoint.id - 1),
                            (p1.id - 1),
                            (p2.id - 1)};
                    _triangles.add(v);

                    

                }
                spoint.pop();
                spoint.push(topStackPoint);

            } else {

                while (spoint.size() > 1) {

                    var stack1Point = (Pointbase) spoint.peek();
                    spoint.pop();
                    var stack2Point = (Pointbase) spoint.peek();
                    spoint.push(stack1Point);

                    pa[0] = topQueuePoint.x;
                    pa[1] = topQueuePoint.y;
                    pb[0] = stack2Point.x;
                    pb[1] = stack2Point.y;
                    pc[0] = stack1Point.x;
                    pc[1] = stack1Point.y;


                    var area = Poly2TriUtils.orient2d(pa, pb, pc);
                    var left = stack1Point.left;

                    if ((area > 0 && left) || (area < 0 && !left)) {
                        v = new int[]{
                                (topQueuePoint.id - 1),
                                (stack2Point.id - 1),
                                (stack1Point.id - 1)};
                        _triangles.add(v);
                        
                        spoint.pop();
                    } else
                        break;
                }
            }
            spoint.push(topQueuePoint);
            qvertex.poll();
        }

        var lastQueuePoint = (Pointbase) qvertex.peek();
        while (spoint.size() != 1) {
            var topPoint = (Pointbase) spoint.peek();
            spoint.pop();
            var top2Point = (Pointbase) spoint.peek();

            _triangles.add(v = new int[]{lastQueuePoint.id - 1, topPoint.id - 1, top2Point.id - 1});

            
        }
    }

    /**
     * main triangulation function;
     * In _triangles is result -> polygon triangles.
     *
     * @return success
     */
    public boolean triangulation() {
        if (!partition2Monotone()) return false;
        if (!searchMonotones()) return false;

        for (var _mpoly : _mpolys) {
            triangulateMonotone((ArrayList) _mpoly);
        }

        setDebugOption(false); 

        return true;
    }

    /**
     * return all triangles
     */
    public ArrayList<int[]> triangles() {
        return _triangles;
    }

    /**
     * output file format;
     */
    public void setDebugOption(boolean debug) {
        if (debug == _debug) return;

            try {
                _logfile.close();
            } catch (IOException e) {
                System.out.println("Problem closing logfile.");
                e.printStackTrace();
                System.out.println("Continueing the work");
            }









        _debug = debug;
    }




}
