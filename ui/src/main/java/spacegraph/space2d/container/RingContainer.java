package spacegraph.space2d.container;

import jcog.math.IntRange;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class RingContainer<X extends Surface> extends EmptyContainer {

    protected X[] x;

    /** T time axis capacity (history) */
    private final IntRange T = new IntRange(1, 1, 512);

    public RingContainer(X[] initArray) {
        x = initArray;
        coords = new float[initArray.length * 4];
        t(initArray.length);
        layout();
    }

    @Override
    public int childrenCount() {
        return 1;
    }

    /** TODO abstract, and other visualization options (z-curve, etc) */
    @Deprecated protected boolean horizOrVert;


    /** current time */
    protected AtomicInteger y = new AtomicInteger(0);


    float[] coords = null;

    protected abstract void reallocate(X[] x);

    /** prepares the next row for rewrite TODO make unsynchronized */
    public synchronized void next(Consumer<X> setter) {
        var t = this.T.intValue();
        var y = this.y.getAndIncrement();

        var x = this.x;
        if (x == null || x.length!=t) { //TODO if history changes
            x = this.x = Arrays.copyOf(x, t);
            this.coords = new float[t * 4];
            reallocate(x);
        } else if (x[0]==null)
            reallocate(x);

        setter.accept(x[y%t]);
        layout();
    }

    @Override
    protected void doLayout(float dtS) {

        var y = this.y.getOpaque();
        var c = coords;
        var t = T.intValue();

        float W = w(), H = h(), left = left(), right = right(), top = bottom(), bottom = top();
        var di = (horizOrVert ? W : H)/t;
        var j = 0;
        for (var i = 0; i < t; i++) {
            var ii = i;
//            Surface xyi = xy[i];
//            ii = (t - 1) - ii; //t-.. : for y-orientation HACK
            if (horizOrVert) {
                var ix = ii * di;
                c[j++] = left + ix; c[j++] = top; c[j++] = left + ix + di; c[j++] = bottom;
                //xyi.posSpectro(left, top + ix,  left + ix + di, bottom);
            } else {
                c[j++] = left;
                var iy = ii * di;
                c[j++] = top + iy; c[j++] = right; c[j++] = top+iy+di;
                //xyi.posSpectro(left,  top + iy, right, top + iy + di);
            }
        }
    }

    public void forEach(BiConsumer<X, RectFloat> each) {
        var j = 0;
        var c = this.coords;
        var xes = this.x;
        var t = T.intValue();
        var y = this.y.intValue();
        for (int i = 0, xesLength = xes.length; i < xesLength; i++) {
            var x = xes[(i + y) % t];
            if (x != null)
                each.accept(x, RectFloat.XYXY(c[j++], c[j++], c[j++], c[j++]));
            else
                j += 4;
        }
    }

    /** set the history length of the spectrogram */
    public void t(int t) {
        this.T.set(t);
    }

    @Override
    protected void renderContent(ReSurface r) {
        forEach((z, b)->{
            z.pos(b);
            z.renderIfVisible(r);
        });
    }


}
