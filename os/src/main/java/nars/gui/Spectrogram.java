package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.math.IntRange;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.ToIntFunction;

/** displays something resembling a "spectrogram" to represent the changing contents of a bag
 * TODO abstract to general-purpose spectrogram aka waterfall plot
 * */
public class Spectrogram extends Surface implements BitmapMatrixView.ViewFunction2D {


    /** T time axis capacity (history) */
    private final IntRange T = new IntRange(1, 1, 512);

    /** N item axis capacity ("bins", "states", "frequencies", etc..) */
    private final IntRange N = new IntRange(1, 1, 512);

    private BitmapMatrixView[] tn = new BitmapMatrixView[1];

    /** current time */
    private int y = 0;

    private int t;
    private int n;

    public IntToIntFunction _color;

    static final ToIntFunction HASHCODE = (Object x) ->
            Draw.colorHSB(Math.abs(x.hashCode() % 1000) / 1000.0f, 0.5f, 0.5f);
    static final IntToIntFunction BLACK = (i) -> {
        return 0;
    };

    public Spectrogram(int T, int N) {
        this.T.set(T);
        this.N.set(N);
        next(BLACK);
    }

    /** prepares the next row for rewrite */
    public synchronized void next(IntToIntFunction colorFn) {

        int t = T.intValue(), n = N.intValue();
        int y = this.y;

        BitmapMatrixView[] xy = Spectrogram.this.tn;
        if (xy == null || xy.length!=t || xy[0]==null || xy[0].h!= n) { //TODO if history changes
            xy = allocate(this.tn, t, n);
        }

        float W = w();
        float dh = h()/t;
        int last = -1;
        for (int i = 0; i < t; i++) {
            int ii = Math.abs(i - y) % t;
            ii = (t-1) - ii; //t-.. : for y-orientation HACK
            float iy = ii * dh;
            xy[i].pos(0, iy, W, iy + dh);
        }



        this.tn = xy;
        this.t = t;
        this.n = n;

        this._color = colorFn;
        xy[y%t].update();

        this.y = (y + 1);

    }

    private BitmapMatrixView[] allocate(BitmapMatrixView[] x, int t, int n) {
        BitmapMatrixView[] y = x!=null ? Arrays.copyOf(x, t) : new BitmapMatrixView[t];
        for (int i = 0; i < t; i++) {
            BitmapMatrixView yi = y[i];
            if (yi!=null && yi.w == n)
                continue;

            BitmapMatrixView r = new BitmapMatrixView(n, 1, Spectrogram.this).cellTouch(false);
            r.start(this);
            y[i] = r;
        }
        return y;
    }

    public Surface newControlPanel() {
        return new Gridding(new VectorLabel("TODO"));
    }



    @Override
    public final int color(int x, int yIgnored /* == 0 */) {
        return _color.applyAsInt(x);
    }

    @Override
    protected void compile(SurfaceRender r) {
        super.compile(r);
        r.on((gl,sr)->{
            for (BitmapMatrixView z : tn) {
                if (!z.showing())
                    z.recompile(sr);
                z.render(gl, sr);
            }
        });
    }

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {

    }

    public final <X> void next(Iterable<X> items, ToIntFunction<X> colorFn) {
        Iterator<X> ii = items.iterator();
        //int j = 0;
//        //could be black, white, any color, or noise
//        protected int colorMissing() {
//            return 0;
//        }
        next((i)->{
            if (ii.hasNext())
                return colorFn.applyAsInt(ii.next());
            else
                return 0;
        });
    }
}
