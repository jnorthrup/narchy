package nars.gui;

import jcog.data.list.FasterList;
import jcog.pri.bag.Bag;
import nars.NAR;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.video.Draw;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/** displays something resembling a "spectrogram" to represent the changing contents of a bag */
public class BagSpectrogram<X> extends Bordering implements BitmapMatrixView.ViewFunction2D {

    private final Bag<?,X> bag;
    private BitmapMatrixView[] view = new BitmapMatrixView[1];
    int offset = 0;
    private int s;
    private final Gridding views = new Gridding(Gridding.HORIZONTAL) {

        {
            margin = 0;
        }

        @Override
        protected int layoutIndex(int i) {
            return (i + offset) % view.length;
        }
        //        @Override
//        public Surface get(int s) {
//            BitmapMatrixView v = view[(s + offset) % view.length];
//            if (v == null)
//                return new EmptySurface(); //HACK
//            return v;
//        }
    };
    final FasterList<X> items = new FasterList();

    public BagSpectrogram(Bag<?,X> x, int history, NAR nar) {

        this.bag = x;

        set(views);

        Surface menu = new Gridding();

        set(S, new DurSurface(menu, nar) {



            @Override
            protected void update() {
                int cap = x.capacity();
                if (view == null || view[0]==null || view[0].h!= cap) { //TODO if history changes
                    view = new BitmapMatrixView[history];
                    for (int i = 0; i < history; i++) {
                        view[i] = new BitmapMatrixView(1, cap, BagSpectrogram.this);

                    }
                    views.set(view);
                }

                try {
                    bag.forEach((Consumer<X>) items::add);
                    s = items.size();
                    view[offset % view.length].update();
                } finally {
                    items.clear();
                }

                offset++;
                views.layout();

            }
        });
    }

    @Override
    public int update(int x, int y) {
        if (x == 0 && y < s) {
            return color(items.get(y));
        }
        return 0;
    }

    /** return RGB integer */
    private ToIntFunction<X> colorFn =
            /** default: by hashcode */
            (X x) -> Draw.colorHSB( Math.abs(x.hashCode() % 1000) / 1000.0f, 0.5f, 0.5f);

    /** return RGB integer */
    public BagSpectrogram<X> color(ToIntFunction<X> colorFn) {
        this.colorFn = colorFn;
        return this;
    }

    /** return RGB integer */
    /*abstract */protected int color(X x) {
        return colorFn.applyAsInt(x);
    }
}
