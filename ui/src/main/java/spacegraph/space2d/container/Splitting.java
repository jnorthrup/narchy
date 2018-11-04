package spacegraph.space2d.container;

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

    @Deprecated
    public Splitting(X top, Y bottom, float split) {
        this(top, bottom, split, true);
    }

    public Splitting(X top, Y bottom, boolean vertical, float split) {
        this(top, bottom, split, vertical);
    }

    private Splitting(Surface top, Surface bottom, float split, boolean vertical) {
        super(top, bottom);
        this.vertical = vertical;
        split(split);
    }

    public static Splitting<?, ?> column(Surface x, float v, Surface y) {
        return new Splitting(x, y, v, true);
    }

    public static Splitting<?, ?> column(Surface x, Surface y) {
        return column(x, 0.5f, y);
    }

    public static Splitting<?, ?> row(Surface x, float v, Surface y) {
        return new Splitting(x, y, v, false);
    }

    public static Splitting<?, ?> row(Surface x, Surface y) {
        return row(x, 0.5f, y);
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

        Surface a = T(), b = B();

        if (a != null && b != null /*&& a.visible() && b.visible()*/) {

            float X = x(), Y = y(), h = h(), w = w();

            if (vertical) {

                float Ysplit = Y + split * h;

                a.pos(X, Ysplit, X + w, Y + h);

                b.pos(X, Y, X + w, Ysplit);
            } else {
                float Xsplit = X + split * w;

                a.pos(X, Y, Xsplit, Y + h);

                b.pos(Xsplit, Y, X + w, Y + h);

            }
        } else if (a != null /*&& a.visible()*/) {
            a.pos(bounds);
        } else if (b != null /*&& b.visible()*/) {
            b.pos(bounds);
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

