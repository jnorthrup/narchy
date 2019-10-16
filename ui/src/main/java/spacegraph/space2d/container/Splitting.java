package spacegraph.space2d.container;

import jcog.Util;
import jcog.math.v2;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerRenderer;
import spacegraph.input.finger.state.SurfaceDragging;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.collection.MutableArrayContainer;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;

/**
 * Splits a surface into a top and bottom or left and right sections
 */
public class Splitting<X extends Surface, Y extends Surface> extends MutableArrayContainer<Surface> {

    private static final float resizeMargin = 0.0125f;
    private float split = Float.NaN;
    private boolean vertical;
    //TODO float marginPct = ...;
    private float minSplit = 0, maxSplit = 1;

    public Splitting() {
        this(null, 0.5f, true, null);
    }

    @Deprecated
    public Splitting(X top, float split, Y bottom) {
        this(top, split, true, bottom);
    }

    public Splitting(X top, float split, boolean vertical, Y bottom) {
        super(null, null, null);
        this.vertical = vertical;
        set(top, split, bottom);
    }

    public static Splitting<?, ?> column(Surface top, float v, Surface bottom) {
        return new Splitting<>(bottom, 1 - v, true, top /* semantically reversed */);
    }

    public static Splitting<?, ?> column(Surface top, Surface bottom) {
        return column(top, 0.5f, bottom);
    }

    public static Splitting<?, ?> row(Surface left, float v, Surface right) {
        return new Splitting<>(left, v, false, right);
    }

    public static Splitting<?, ?> row(Surface x, Surface y) {
        return row(x, 0.5f, y);
    }

    public final Splitting<X, Y> horizontal(boolean h) {
        return vertical(!h);
    }

    public Splitting<X, Y> vertical(boolean v) {
        boolean w = this.vertical;
        if (w != v) {
            this.vertical = v;
            layout();
        }
        return this;
    }

    public Splitting<X, Y> vertical() {
        vertical = true;
        layout();
        return this;
    }

    Splitting<X, Y> horizontal() {
        vertical = false;
        layout();
        return this;
    }

    public Splitting<X, Y> split(float split) {
        float s = this.split;
        if (!Util.equals(s, Spatialization.EPSILON)) {
            this.split = split;
            layout();
        }
        return this;
    }

    public Splitting<X, Y> set(X top, float split, Y bottom) {
        split(split);
        T(top);
        B(bottom);
        return this;
    }

    @Nullable
    public Surface resizer() {
        return get(2);
    }

    @Override
    public void doLayout(float dtS) {

        Surface a = T(), b = B();

        Surface r = this.resizer();
        if (a != null && b != null /*&& a.visible() && b.visible()*/) {

            float X = x(), Y = y(), h = h(), w = w();

            if (vertical) {

                float Ysplit = Y + split * h;

                RectFloat r3 = RectFloat.XYXY(X, Ysplit, X + w, Y + h);
                a.pos(r3);

                RectFloat r2 = RectFloat.XYXY(X, Y, X + w, Ysplit);
                b.pos(r2);

                if (r != null) {
                    RectFloat r1 = RectFloat.XYXY(X, Ysplit - resizeMargin / 2 * h, X + w, Ysplit + resizeMargin / 2 * h);
                    r.pos(r1);
                    r.show();
                }
            } else {
                float Xsplit = X + split * w;

                RectFloat r3 = RectFloat.XYXY(X, Y, Xsplit, Y + h);
                a.pos(r3);

                RectFloat r2 = RectFloat.XYXY(Xsplit, Y, X + w, Y + h);
                b.pos(r2);

                if (r != null) {
                    RectFloat r1 = RectFloat.XYXY(Xsplit - resizeMargin / 2 * w, Y, Xsplit + resizeMargin / 2 * w, Y + h);
                    r.pos(r1);
                    r.show();
                }
            }

            a.show();
            b.show();
        } else if (a != null /*&& a.visible()*/) {
            a.show();
            b.hide();
            a.pos(bounds);
            if (r != null) r.hide();
        } else if (b != null /*&& b.visible()*/) {
            a.hide();
            b.show();
            b.pos(bounds);
            if (r != null) r.hide();
        } else {
            a.hide();
            b.hide();
            if (r != null) r.hide();
        }


    }

    @Override
    public int childrenCount() {
        return 2;
    }

    public final Splitting<X, Y> T(X s) {
        setAt(0, s);
        return this;
    }

    public final Splitting<X, Y> B(Y s) {
        setAt(1, s);
        return this;
    }

    public final Splitting<X, Y> L(X s) {
        T(s);
        return this;
    }

    public final Splitting<X, Y> R(Y s) {
        B(s);
        return this;
    }

    public final X T() {
        return (X) get(0);
    }

    public final Y B() {
        return (Y) get(1);
    }

    public final X L() {
        return T();
    }

    public final Y R() {
        return B();
    }


    public Splitting resizeable() {
        return resizeable(0.1f, 0.9f);
    }

    public Splitting resizeable(float minSplit, float maxSplit) {
        this.minSplit = minSplit;
        this.maxSplit = maxSplit;
        synchronized (this) {
            if (this.resizer() == null) {
                setAt(2, new ResizeBar()); //TODO
            }
        }
        return this;
    }

    /**
     * TODO button to toggle horiz/vert/auto and swap
     */
    private class Resizer extends Widget {
        final SurfaceDragging drag = new SurfaceDragging(this, 0) {

            @Override
            public FingerRenderer renderer(Finger finger) {
                return vertical ? FingerRenderer.rendererResizeNS : FingerRenderer.rendererResizeEW;
            }


            @Override
            public boolean drag(Finger f) {

                    focus();
                    v2 b = f.posRelative(Splitting.this);
                    float pct = vertical ? b.y : b.x;
                    float p = Util.clamp(pct, minSplit, maxSplit);
                    split(p);
                    return true;

            }

        };

        {
            color.set(0.1f, 0.1f, 0.1f, 0.5f);
        }

        @Override
        public Surface finger(Finger f) {
            return f.test(drag) ? this : this /*null*/;
        }
    }

    private class ResizeBar extends Bordering {

        final Surface hv = new CheckBox("*").on(vertical).on((BooleanProcedure)
                (Splitting.this::vertical));

        final PushButton swap = new PushButton("<->").clicked(() -> {
            //synchronized (Splitting.this) {
            X a = Splitting.this.L();
            if (a != null) {
                Y b = Splitting.this.R();
                if (b != null) {
                    if (children.compareAndSet(0, a, b)) {
                        children.setFast(1, a);
                    }
                }
            }

        });

        ResizeBar() {
            set(new Resizer());
        }

        @Override
        protected void doLayout(float dtS) {
            if (vertical) {
                north(null);
                south(null);
                east(hv);
                west(swap);
            } else {
                east(null);
                west(null);
                north(hv);
                south(swap);
            }

            super.doLayout(dtS);
        }
    }


}

