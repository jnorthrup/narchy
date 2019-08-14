package spacegraph.space2d.widget.meter;

import jcog.math.IntRange;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.RingContainer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.util.function.ToIntFunction;

/** displays something resembling a "spectrogram" to represent the changing contents of a bag
 * TODO abstract to general-purpose spectrogram aka waterfall plot
 * */
public class Spectrogram extends RingContainer<BitmapMatrixView> implements BitmapMatrixView.ViewFunction2D {


    /** N item axis capacity ("bins", "states", "frequencies", etc..) */
    public final IntRange N = new IntRange(0, 0, 512);


    public IntToIntFunction _color;

    static final ToIntFunction HASHCODE = (Object x) ->
            Draw.colorHSB(Math.abs(x.hashCode() % 1000) / 1000.0f, 0.5f, 0.5f);
    static final IntToIntFunction BLACK = (i) -> 0;

    public Spectrogram(boolean leftOrDown, int T, int N) {
        super(new BitmapMatrixView[T]);
        this.horizOrVert = leftOrDown;
        this._color = BLACK;
        this.N.set(N);
    }


    @Override protected void reallocate(BitmapMatrixView[] x) {
        int n = this.N.getAsInt();
        // {
        //        //|| xy[0]==null  (xy[0].w * xy[0].h) != n
        //    }
        for (int i = 0; i < x.length; i++) {
            BitmapMatrixView yi = this.x[i];
            if (yi!=null && yi.w * yi.h == n)
                continue;

            int W, H;
            if (horizOrVert) {
                W = 1; H = n;
            } else {
                W = n; H = 1;
            }
            BitmapMatrixView r = new BitmapMatrixView(W, H, this);
            r.cellTouch(false);
            r.pos(RectFloat.Unit);
            r.start(this);
            this.x[i] = r;
        }
    }

    public Surface newControlPanel() {
        return new Gridding(new VectorLabel("TODO"));
    }



    @Override
    public final int color(int x, int y) {
        return _color.applyAsInt(horizOrVert ? y : x);
    }

    @Override
    public void renderContent(ReSurface r) {

        r.on((gl,sr)->{
            //float W = w(), H = h();

            forEach((BitmapMatrixView z, RectFloat b)->{
                if (!z.tex.ready()) {
                    z.tex.commit(gl);
                    z.show();
                }
                z.tex.paint(gl, b);
            });

        });
    }

    public void next(IntToIntFunction color) {
        this._color = color;
        next(BitmapMatrixView::update);
    }
//    public void next(Tensor data, FloatToIntFunction color) {
//        this._color = color;
//        next((BitmapMatrixView b)->b.update());
//    }


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

}
