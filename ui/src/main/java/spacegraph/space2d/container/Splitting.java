package spacegraph.space2d.container;

import jcog.TODO;
import jcog.Util;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.collection.MutableArrayContainer;

/**
 * Splits a surface into a top and bottom or left and right sections
 */
public class Splitting<X extends Surface, Y extends Surface> extends MutableArrayContainer {

    private volatile float split;
    private volatile boolean vertical;

    public Splitting() {
        this(new EmptySurface(), new EmptySurface(), 0.5f, true);
    }

    @Deprecated public Splitting(X top, Y bottom, float split) {
        this(top, bottom, split, true);
    }

    public Splitting(X top, Y bottom, boolean vertical, float split) {
        this(top, bottom, split, vertical);
    }

    private Splitting(Surface top, Surface bottom, float split, boolean vertical) {
        super(top,bottom);
        this.vertical = vertical;
        split(split);
    }

    Splitting vertical() {
        if (!this.vertical) {
            vertical = true;
            layout();
        }
        return this;
    }

    Splitting horizontal() {
        if (this.vertical) {
            vertical = false;
            layout();
        }
        return this;
    }

    public Splitting split(float split) {
        float s = this.split;
        this.split = split;
        if (!Util.equals(s, split, 0.0001f))
            layout();
        return this;
    }

    public Splitting split(X top, Y bottom, float split) {
        put(0, top);
        put(1, bottom);
        split(split);
        return this;
    }

    @Override
    public void doLayout(int dtMS) {


        


        if (!vertical)
            throw new TODO();

        float X = x();
        float Y = y();
        float h = h();
        float w = w();
        float Ysplit = Y + split * h;

        Surface top = T();
        if (top!=null) {
            top.pos(X, Ysplit, X+w, Y+h);
        }

        Surface bottom = B();
        if (bottom != null) {
            bottom.pos(X,  Y, X+w, Ysplit);
        }

    }

    public final Splitting<X, Y> T(X s) {
        put(0, s);
        return this;
    }
    public final Splitting<X, Y> B(Y s) {
        put(1, s);
        return this;
    }
    public final Splitting<X, Y> L(X s) {
        put(0, s);
        return this;
    }
    public final Splitting<X, Y> R(Y s) {
        put(1, s);
        return this;
    }
    private X T() {
        return (X) get(0);
    }
    private Y B() {
        return (Y) get(1);
    }
    public final X L() {
        return (X) get(0);
    }
    public final Y R() {
        return (Y) get(1);
    }

}

