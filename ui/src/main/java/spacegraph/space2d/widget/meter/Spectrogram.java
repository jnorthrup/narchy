package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.math.IntRange;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.grid.Gridding;
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
    public final IntRange N = new IntRange(0, 0, 512);


    /** TODO abstract, and other visualization options (z-curve, etc) */
    @Deprecated private final boolean horizOrVert;

    private MyBitmapMatrixView[] tn = new MyBitmapMatrixView[1];

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

    public Spectrogram(boolean leftOrDown, int T, int N) {
        this.horizOrVert = leftOrDown;
        this.T.set(T);
        this.N.set(N);
        next(BLACK);
    }

    /** prepares the next row for rewrite */
    public synchronized void next(IntToIntFunction colorFn) {

        int t = T.intValue(), n = N.intValue();
        int y = this.y;

        MyBitmapMatrixView[] xy = Spectrogram.this.tn;
        if (xy == null || xy.length!=t || xy[0]==null || (xy[0].w * xy[0].h) != n) { //TODO if history changes
            xy = allocate(this.tn, t, n);
        }

        float W = w(), H = h(), left = left(), right = right(), top = top(), bottom = bottom();
        float di = (horizOrVert ? W : H)/t;
        for (int i = 0; i < t; i++) {
            int ii = Math.abs(i - y) % t;
            MyBitmapMatrixView xyi = xy[i];
            ii = (t - 1) - ii; //t-.. : for y-orientation HACK
            if (horizOrVert) {
                float ix = ii * di;
                xyi.posSpectro(left, top + ix,  left + ix + di, bottom);
            } else {
                float iy = ii * di;
                xyi.posSpectro(left,  top + iy, right, top + iy + di);
            }
        }



        this.tn = xy;
        this.t = t;
        this.n = n;

        this._color = colorFn;
        xy[y%t].update();

        this.y = (y + 1);

    }

    private MyBitmapMatrixView[] allocate(MyBitmapMatrixView[] x, int t, int n) {
        MyBitmapMatrixView[] y = x!=null ? Arrays.copyOf(x, t) : new MyBitmapMatrixView[t];
        for (int i = 0; i < t; i++) {
            MyBitmapMatrixView yi = y[i];
            if (yi!=null && yi.w * yi.h == n)
                continue;

            int W, H;
            if (horizOrVert) {
                W = 1; H = n;
            } else {
                W = n; H = 1;
            }
            MyBitmapMatrixView r = new MyBitmapMatrixView(W, H);
            r.start(this);
            y[i] = r;
        }
        return y;
    }

    public Surface newControlPanel() {
        return new Gridding(new VectorLabel("TODO"));
    }



    @Override
    public final int color(int x, int y) {
        return _color.applyAsInt(horizOrVert ? y : x);
    }

    @Override
    protected void compile(SurfaceRender r) {
        super.compile(r);
        r.on((gl,sr)->{
            //float W = w(), H = h();
            for (MyBitmapMatrixView z : tn) {
//                if (!z.showing())
//                    z.recompile(sr);
//                z.render(gl, sr);

//                gl.glPushMatrix();
                //gl.glTranslatef(z.px1, z.py1, 0);
                //gl.glScalef(W * (z.px2 - z.px1), H * (z.py2 - z.py1), 1);
                if (!z.tex.ready()) {
                    z.tex.commit(gl);
                    z.show();
                }
                z.tex.paint(gl, RectFloat.XYXY(z.px1, z.py1, z.px2, z.py2));
//                gl.glPopMatrix();
            }
        });
    }


//    public final <X> void next(Iterable<X> items, ToIntFunction<X> colorFn) {
//        Iterator<X> ii = items.iterator();
//        //int j = 0;
////        //could be black, white, any color, or noise
////        protected int colorMissing() {
////            return 0;
////        }
//        next((i)->{
//            if (ii.hasNext())
//                return colorFn.applyAsInt(ii.next());
//            else
//                return 0;
//        });
//    }

    private class MyBitmapMatrixView extends BitmapMatrixView {
        private float px1, py1, px2, py2;

        public MyBitmapMatrixView(int w, int h) {
            super(w, h, Spectrogram.this);
            cellTouch(false);
            pos(RectFloat.Unit);
            //pos = 0,0..1,1
        }

        void posSpectro(float x1, float y1, float x2, float y2) {
            this.px1 = x1; this.py1 = y1; this.px2 = x2; this.py2 = y2;
        }


        //        public void setPosSpectro(float )
    }

}
