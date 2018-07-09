package spacegraph.space2d.phys.fracture.poly2Tri;

import java.util.concurrent.atomic.AtomicInteger;

class Poly2TriUtils {

    
    public static final double PI = Math.PI;

    
    public static final int UNKNOWN = 1;
    public static final int INPUT = 2;
    public static final int INSERT = 3;
    public static final int START = 4;
    public static final int END = 5;
    public static final int MERGE = 6;
    public static final int SPLIT = 7;
    public static final int REGULAR_UP = 8;
    public static final int REGULAR_DOWN = 9;

    public static String typeToString(int type) {
        switch (type) {
            case Poly2TriUtils.UNKNOWN:
                return "UNKNOWN";
            case Poly2TriUtils.INPUT:
                return "INPUT";
            case Poly2TriUtils.INSERT:
                return "INERT";
            case Poly2TriUtils.START:
                return "START";
            case Poly2TriUtils.END:
                return "END";
            case Poly2TriUtils.MERGE:
                return "MERGE";
            case Poly2TriUtils.SPLIT:
                return "SPLIT";
            case Poly2TriUtils.REGULAR_UP:
                return "REGULAR_UP";
            case Poly2TriUtils.REGULAR_DOWN:
                return "REGULAR_DOWN";
            default:
                return "??? (" + type + ')';
        }
    }
	
	/*
	    class   Pointbase;
	    class   Linebase;

		template <class T, class KeyType> class         SplayTree;
		typedef map<unsigned int, Pointbase*>           PointbaseMap;
		typedef map<unsigned int, Linebase*>            LineMap;
		typedef priority_queue<Pointbase>               PQueue;
		typedef SplayTree<Linebase*, double>            EdgeBST;
		typedef list<unsigned int>                      Monopoly;
		typedef list<Monopoly>                          Monopolys; 
		typedef vector<unsigned int>                    Triangle;
		typedef list<Triangle>                          Triangles;
		typedef map<unsigned int, set<unsigned int> >   AdjEdgeMap;
	 */

    

    /**
     * In original poly2tri there is an exact arithemtic from
     * Jonathan Shewchuk ... sorry I didn't have time to reimplement
     * that (also I don't know if you can reimplement it 1:1 ...)
     */
    public static double orient2d(double[] pa, double[] pb, double[] pc) {
        double detleft, detright;

        double pc0 = pc[0];
        double pc1 = pc[1];
        detleft = (pa[0] - pc0) * (pb[1] - pc1);
        detright = (pa[1] - pc1) * (pb[0] - pc0);

        return detleft - detright;
    }

    public static final AtomicInteger l_id = new AtomicInteger(); 
    public static final AtomicInteger p_id = new AtomicInteger(); 

}
