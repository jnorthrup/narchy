package spacegraph.space2d.container;

import jcog.math.IntRange;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class RingContainer<X extends Surface> extends Surface {

    protected X[] x;

    /** T time axis capacity (history) */
    private final IntRange T = new IntRange(1, 1, 512);
    private int t;

    public RingContainer(X[] initArray) {
        x = initArray;
        coords = new float[initArray.length * 4];
        t(initArray.length);
    }

    /** TODO abstract, and other visualization options (z-curve, etc) */
    @Deprecated protected boolean horizOrVert;


    /** current time */
    protected AtomicInteger y = new AtomicInteger(0);


    float[] coords = null;

    abstract protected void reallocate(X[] x);

    /** prepares the next row for rewrite TODO make unsynchronized */
    public synchronized void next(Consumer<X> setter) {
        int t = this.T.intValue();
        int y = this.y.getAndIncrement();

        X[] x = this.x;
        float[] c = this.coords;
        if (x == null || x.length!=t) { //TODO if history changes
            x = this.x = Arrays.copyOf(x, t);
            c = this.coords = new float[t * 4];
            reallocate(x);
        } else if (x[0]==null)
            reallocate(x);


        float W = w(), H = h(), left = left(), right = right(), top = top(), bottom = bottom();
        float di = (horizOrVert ? W : H)/t;
        int j = 0;
        for (int i = 0; i < t; i++) {
            int ii = Math.abs(i - y) % t;
//            Surface xyi = xy[i];
            ii = (t - 1) - ii; //t-.. : for y-orientation HACK
            if (horizOrVert) {
                float ix = ii * di;
                c[j++] = left + ix; c[j++] = top; c[j++] = left + ix + di; c[j++] = bottom;
                //xyi.posSpectro(left, top + ix,  left + ix + di, bottom);
            } else {
                float iy = ii * di;
                c[j++] = left; c[j++] = top + iy; c[j++] = right; c[j++] = top+iy+di;
                //xyi.posSpectro(left,  top + iy, right, top + iy + di);
            }
        }

        this.t = t;


        setter.accept(x[y%t]);
    }

    public void forEach(BiConsumer<X, RectFloat> each) {
        int j = 0;
        float[] c = this.coords;
        for (X x : this.x) {
            if (x!=null)
                each.accept(x, RectFloat.XYXY(c[j++], c[j++], c[j++], c[j++]));
            else
                j+=4;
        }
    }

    /** set the history length of the spectrogram */
    public void t(int t) {
        this.T.set(t);
    }
    @Override
    protected void compile(SurfaceRender r) {
        super.compile(r);
        forEach((z, b)->{
            z.pos(b);
            z.recompile(r);
        });
    }

}
