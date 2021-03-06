package spacegraph.space2d.container;

import jcog.TODO;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.collection.MutableArrayContainer;

/* 9-element subdivision


 */
public class Bordering<S extends Surface> extends MutableArrayContainer<S> {
    public static final int C = 0;
    public static final int N = 1;
    public static final int S = 2;
    public static final int E = 3;
    public static final int W = 4;
    public static final int NE = 5;
    public static final int NW = 6;
    public static final int SW = 7;
    public static final int SE = 8;

    /**
     * in percent of the half total size of the corresponding dimension
     */
    protected float borderWest = 0.25f;
    protected float borderEast = 0.25f;
    protected float borderSouth = 0.25f;
    protected float borderNorth = 0.25f;

    public Bordering() {
        super(SE+1);
    }

    public Bordering(S center) {
        this();
        set(center);
    }

    public Bordering<S> set(S center) {
        set(C, center);
        return this;
    }

    /**
     * sets all edge sizes to a value
     * TODO layout only if changed
     */
    protected Bordering<S> borderSize(float size) {
        borderNorth = borderSouth = borderEast = borderWest = size;
        layout();
        return this;
    }

    /**
     * sets a specific edge size
     * TODO layout only if changed
     */
    public Bordering borderSize(int direction, float size) {
        switch (direction) {
            case N:
                borderNorth = size;
                break;
            case S:
                borderSouth = size;
                break;
            case E:
                borderEast = size;
                break;
            case W:
                borderWest = size;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        layout();
        return this;
    }

    @Override
    protected void doLayout(float dtS) {

        float X = x();
        float Y = y();
        float W = w();
        float H = h();

//        boolean aspectEqual = false;
//        if (aspectEqual) {
//            w2 = h2 = Math.min(W, H) / 2;
//        } else {
        float w2 = W / 2.0F;
        float h2 = H / 2.0F;
//        }


        boolean se = get(Bordering.SE) != null;
        boolean ne = get(Bordering.NE) != null;
        boolean sw = get(Bordering.SW) != null;
        boolean nw = get(Bordering.NW) != null;
        boolean autocollapse = true;
        float borderWest = autocollapse && !(sw || nw || get(Bordering.W) != null) ? (float) 0 : this.borderWest;
        float borderEast = autocollapse && !(se || ne || get(Bordering.E) != null) ? (float) 0 : this.borderEast;
        float borderNorth = autocollapse && !(ne || nw || get(Bordering.N) != null) ? (float) 0 : this.borderNorth;
        float borderSouth = autocollapse && !(se || sw || get(Bordering.S) != null) ? (float) 0 : this.borderSouth;

        for (int i = 0, childrenLength = 9; i < childrenLength; i++) {

            S c = get(i);

            if (c == null || !c.visible())
                continue;

            float x1, y1, x2, y2;

            switch (i) {
                case C:
                    x1 = borderWest * w2;
                    y1 = borderSouth * h2;
                    x2 = W - borderEast * w2;
                    y2 = H - borderNorth * h2;
                    break;
                case N:
                    x1 = borderWest * w2;
                    y1 = H - borderNorth * h2;
                    x2 = W - borderEast * w2;
                    y2 = H;
                    break;
                case S:
                    x1 = borderWest * w2;
                    y1 = (float) 0;
                    x2 = W - borderEast * w2;
                    y2 = borderSouth * h2;
                    break;
                case Bordering.W:
                    x1 = (float) 0;
                    y1 = borderSouth * h2;
                    x2 = borderWest * w2;
                    y2 = H - borderNorth * h2;
                    break;
                case E:
                    x1 = W - borderEast * w2;
                    y1 = borderSouth * h2;
                    x2 = W;
                    y2 = H - borderNorth * h2;
                    break;
                case NE:
                    x1 = W - borderEast * w2;
                    y1 = H - borderNorth * h2;
                    x2 = W;
                    y2 = H;
                    break;
                case NW:
                    x1 = (float) 0;
                    y1 = H - borderNorth * h2;
                    x2 = borderWest * w2;
                    y2 = H;
                    break;
                case SW:
                    x1 = (float) 0;
                    x2 = borderWest * w2;
                    y1 = (float) 0;
                    y2 = borderSouth * h2;
                    break;
                case SE:
                    x1 = W - borderEast * w2;
                    x2 = W;
                    y1 = (float) 0;
                    y2 = borderSouth * h2;
                    break;
                default:
                    throw new TODO();
            }

//            if (x2 - x1 < ScalarValue.EPSILON || y2-y1 < ScalarValue.EPSILON) {
//                c.hide();
//            } else {
            RectFloat r = RectFloat.XYXY(X + x1, Y + y1, X + x2, Y + y2);
            c.pos(r);
            //                c.show();

//            }
        }

    }


    /**
     * replace center content
     */
    public Bordering center(S next) {
        set(C, next);
        return this;
    }

    public Bordering set(int direction, S next, float borderSizePct) {
        borderSize(direction, borderSizePct);
        return set(direction, next);
    }

    public Bordering set(int direction, S next) {
        if (direction >= 9)
            throw new ArrayIndexOutOfBoundsException();

        setAt(direction, next);
        return this;
    }

    public Bordering north(S x) {
        return set(Bordering.N, x);
    }

    public Bordering south(S x) {
        return set(Bordering.S, x);
    }

    public Bordering east(S x) {
        return set(Bordering.E, x);
    }

    public Bordering west(S x) {
        return set(Bordering.W, x);
    }

    public Bordering northwest(S x) {
        return set(Bordering.NW, x);
    }

    public Bordering northeast(S x) {
        return set(Bordering.NE, x);
    }

    public Bordering southwest(S x) {
        return set(Bordering.SW, x);
    }

    public Bordering southeast(S x) {
        return set(Bordering.SE, x);
    }
}
