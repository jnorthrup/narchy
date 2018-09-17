package spacegraph.space2d.container;

import jcog.TODO;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.collection.MutableArrayContainer;

/* 9-element subdivision


 */
public class Bordering<S extends Surface> extends MutableArrayContainer<S> {
    public final static int C = 0;
    public final static int N = 1;
    public final static int S = 2;
    public final static int E = 3;
    public final static int W = 4;
    public final static int NE = 5;
    public final static int NW = 6;
    public final static int SW = 7;
    public final static int SE = 8;

    /**
     * in percent of the half total size of the corresponding dimension
     */
    protected float borderWest = 0.25f;
    protected float borderEast = 0.25f;
    protected float borderSouth = 0.25f;
    protected float borderNorth = 0.25f;

    private boolean autocollapse = true;

    public Bordering() {
        super(SE+1);
    }

    public Bordering(S center) {
        this();
        set(center);
    }

    public void set(S center) {
        set(0, center);
    }

    /**
     * sets all edge sizes to a value
     */
    protected Bordering borderSize(float size) {
        borderNorth = borderSouth = borderEast = borderWest = size;
        layout(); 
        return this;
    }

    /**
     * sets a specific edge size
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
    protected void doLayout(int dtMS) {

        float X = x();
        float Y = y();
        float W = w();
        float H = h();
        float w2, h2;

        boolean aspectEqual = false;
        if (aspectEqual) {
            w2 = h2 = Math.min(W, H) / 2;
        } else {
            w2 = W / 2;
            h2 = H / 2;
        }




        float borderWest, borderEast, borderNorth, borderSouth;
        int l = length;
        borderWest = autocollapse && !(l > Bordering.W && children.getOpaque(Bordering.W) != null) ? 0 : this.borderWest;
        borderEast = autocollapse && !(l > Bordering.E && children.getOpaque(Bordering.E) != null) ? 0 : this.borderEast;
        borderNorth = autocollapse && !(l > Bordering.N && children.getOpaque(Bordering.N) != null) ? 0 : this.borderNorth;
        borderSouth = autocollapse && !(l > Bordering.S && children.getOpaque(Bordering.S) != null) ? 0 : this.borderSouth;

        for (int i = 0, childrenLength = l; i < childrenLength; i++) {
            S c = children.getOpaque(i);

            if (c == null)
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
                    y1 = 0;
                    x2 = W - borderEast * w2;
                    y2 = borderSouth * h2;
                    break;
                case Bordering.W:
                    x1 = 0;
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
                case SW:
                    x1 = 0;
                    y1 = 0;
                    x2 = borderWest * w2;
                    y2 = borderSouth * h2;
                    break;
                default:
                    throw new TODO();
            }
            
            c.pos(X + x1, Y + y1, X + x2, Y + y2);
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

        synchronized (this) {
            children.set(direction, next);
            return this;
        }
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
