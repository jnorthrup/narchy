package spacegraph.layout;

/* 9-element subdivision


 */
public class Bordering extends Stacking {
    final static int C = 0;
    final static int N = 1;
    final static int S = 2;
    final static int E = 3;
    final static int W = 4;
    final static int NW = 5;
    final static int NE = 6;
    final static int SW = 7;
    final static int SE = 8;

    /** in percent of the total size */
    float borderFractionX = 0.5f, borderFractionY = 0.5f;

    public Bordering() {
        super();

    }
}
