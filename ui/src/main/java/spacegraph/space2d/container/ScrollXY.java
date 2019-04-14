package spacegraph.space2d.container;

import jcog.TODO;
import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.DynGrid;
import spacegraph.space2d.container.grid.GridModel;
import spacegraph.space2d.container.grid.GridRenderer;
import spacegraph.space2d.container.grid.ListModel;
import spacegraph.space2d.container.unit.Clipped;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.XYSlider;

import java.util.List;
import java.util.function.Function;

import static jcog.Util.lerp;
import static spacegraph.space2d.widget.slider.SliderModel.KnobHoriz;
import static spacegraph.space2d.widget.slider.SliderModel.KnobVert;

/**
 * see also:
 * https:
 * https:
 */
public class ScrollXY<S extends ScrollXY.ScrolledXY> extends Bordering {


    public S content = null;

    /**
     * proportional in scale to bounds
     */
    private static final float defaultScrollEdge = 0.12f;

    private final FloatSlider scrollX, scrollY;
    private final XYSlider scale;

    /**
     * current view, in local grid coordinate
     */
    private RectFloat view;
    private v2 viewMin = new v2(0,0);
    //private volatile v2 viewDefault = new v2(0,0);
    protected v2 viewMax = new v2(1,1);

    private final boolean autoHideScroll = true;


    public <X> ScrollXY(GridModel<X> grid, GridRenderer<X> renderer) {
        this((S) new DynGrid<>(grid, renderer));
    }

    public ScrollXY(S scrollable) {
        this();
        set(scrollable);
    }
    /**
     * by default, only the first cell will be visible
     */
    public ScrollXY() {
        super();

        this.scale = new XYSlider();
        this.scrollX = new FloatProportionalSlider("X", () -> 0, () -> view.w / viewMax.x, () -> viewMax.x - view.w, true);
        this.scrollY = new FloatProportionalSlider("Y", () -> 0, () -> view.h / viewMax.y, () -> viewMax.y - view.h, false);

        set(E,scrollY);
        set(S,scrollX);
        set(SE, scale);

        scrollX.on((sx, x) -> scroll(x, view.y, view.w, view.h));
        scrollY.on((sy, y) -> scroll(view.x, y, view.w, view.h));
        scale.set(1,1);
        scale.on((w, h)-> scroll(view.x, view.y, lerp(w, viewMin.x, viewMax.x), lerp(h, viewMin.y, viewMax.y)));


    }

    public synchronized void set(S scrollable) {

        scrollable.update(this);
        if (viewMin == null)
            throw new NullPointerException("view min set by " + scrollable);
        if (viewMax == null)
            throw new NullPointerException("view max set by " + scrollable);
        if (view == null)
            view = RectFloat.WH(viewMax.x, viewMax.y); //TODO max reasonable limit

        borderSize(defaultScrollEdge);

        set(C, new Clipped((Surface) (content = scrollable)));
    }

    public ScrollXY<S> viewMax(v2 viewMax) {
        this.viewMax = viewMax;
        this.viewMin = new v2(Math.min(viewMax.x, viewMin.x),Math.min(viewMax.y, viewMin.y));
        layoutModel(); //TODO update if changed
        return this;
    }

    public ScrollXY<S> viewMin(v2 viewMin) {
        this.viewMin = viewMin;
        this.viewMax = new v2(Math.max(viewMax.x, viewMin.x),Math.max(viewMax.y, viewMin.y));
        layoutModel(); //TODO update if changed
        return this;
    }

    public ScrollXY<S> view(v2 view) {
        return view(view.x, view.y);
    }

    public ScrollXY<S> view(float w, float h) {
        RectFloat view1 = RectFloat.WH(w, h);
        if (this.view == null) {
            this.view = view1; //initial
            layoutModel();
        } else {

            //break suspected deadlock
//            Exe.invoke(() -> {
                this.view = view1;
                layoutModel();
//            });
        }
        return this;
    }

    /** manually trigger layout */
    public final void update() {
        content.update(this);
        layoutModel();
    }

    private void layoutModel() {

        if (view!=null) {
            float vw = Util.clamp(view.w, viewMin.x, viewMax.x);
            float vh = Util.clamp(view.h, viewMin.y, viewMax.y);
            scale.set(Util.normalize(view.w, viewMin.x, viewMax.x),Util.normalize(view.h, viewMin.y, viewMax.y));
            this.view = RectFloat.X0Y0WH(
                    Util.clamp(view.x, 0, viewMax.x),
                    Util.clamp(view.y, 0, viewMax.y),
                    vw, vh);
        }


        S m = this.content;

        if (m instanceof ContainerSurface)
            ((ContainerSurface)m).layout();
    }

    /**
     * the current view
     */
    public final RectFloat view() {
        return view;
    }

//    /** set the view window's center of focus, re-using the current width and height */
//    private ScrollXY<S> view(float x, float y) {
//        return view(x, y, view.w, view.h);
//    }

//    /** set the view window's center and size of focus, in grid coordinates */
//    private ScrollXY<S> view(RectFloat v) {
//        return view(v.x, v.y, v.w, v.h);
//    }


    /**
     * enables requesting entries from the -1'th row and -1'th column of
     * the model to use as 'pinned' row header cells
     */
    public ScrollXY<S> setHeader(boolean rowOrColumn, boolean enabled) {
        throw new TODO();
    }

    /**
     * enables or disables certain scrollbar-related features per axis
     */
    private ScrollXY<S> setScrollBar(boolean xOrY, boolean scrollVisible, boolean scaleVisible) {
        if (xOrY) {
            scrollX.visible(scrollVisible);
            borderSize(S, scrollVisible ? defaultScrollEdge : 0);
            //scaleW.visible(scaleVisible);
            borderSize(N, scaleVisible ? defaultScrollEdge : 0);
        } else {
            scrollY.visible(scrollVisible);
            borderSize(E, scrollVisible ? defaultScrollEdge : 0);
            //scaleH.visible(scaleVisible);
            borderSize(W, scaleVisible ? defaultScrollEdge : 0);
        }


        return this;
    }

//    /** limits the scaling range per axis */
//    public ScrollXY<X> setCellScale(boolean xOrY, float minScale, float maxScale) {
//        throw new TODO();
//    }
//
//    /** limits the viewing range per axis */
//    public ScrollXY<X> setCellView(boolean xOrY, float minCoord, float maxCoord) {
//        throw new TODO();
//    }


    public final ScrollXY scroll(float w, float h) {
        return scroll(0, 0, w, h);
    }

    public/*synchronized*/ ScrollXY scroll(float x, float y, float w, float h) {

        float x1, x2;
        x1 = x;
        x2 = x+w;
        setScrollBar(true, (!autoHideScroll || w > viewMax.x), true);

        float y1, y2;
        y1 = y;
        y2 = y1 + h;
        setScrollBar(false, (!autoHideScroll || h > viewMax.y), true);

//        viewMax(new v2(x2-x1, y2-y1)); //HACK

        scale.visible(scrollX.visible()||scrollY.visible());

        RectFloat nextView = RectFloat.X0Y0WH(x1, y1, x2 - x1, y2 - y1);
        if (!nextView.equals(view)) {
            this.view = nextView;
            layoutModel();
            layout();
        }

        return this;
    }




//    public final void refresh() {
//        view(view);
//    }


    public interface ScrolledXY {

        /**
         * implementors expected to initially set the view bounds on init, and update the view window if necessary
         */
        void update(ScrollXY s);
    }


    public static <X> ScrollXY<DynGrid<X>> array(GridRenderer<X> builder, X...list) {
        return new ScrollXY<>(ListModel.of(list), builder);
    }

    public static <X> ScrollXY<DynGrid<X>> list(GridRenderer<X> builder, List<X> list) {
        return list((Function<X,Surface>)builder, list);
    }

    public static <X> ScrollXY<DynGrid<X>> list(Function<X, Surface> builder, List<X> list) {
        ScrollXY<DynGrid<X>> s = new ScrollXY<>(ListModel.of(list), GridRenderer.value(builder));
        return s;
    }

    public static <X> ScrollXY<DynGrid<X>> listCached(Function<X, Surface> builder, List<X> list, int cacheCapacity) {
        ScrollXY<DynGrid<X>> s = new ScrollXY<>(ListModel.of(list), GridRenderer.valueCached(builder, cacheCapacity));
        return s;
    }

    private static class FloatProportionalSlider extends FloatSlider {

        /** proportional knob width */
        private final FloatSupplier knob;

        public FloatProportionalSlider(String label, FloatSupplier min, FloatSupplier knob, FloatSupplier max, boolean hOrV) {
            super(new FloatSliderModel() {
                    @Override
                    public float min() {
                        return min.asFloat();
                    }

                    @Override
                    public float max() {
                        return max.asFloat();
                    }
                }, label
            );

            this.knob = knob;
            type(hOrV ? new KnobHoriz() : new KnobVert());
        }

        @Override
        public boolean preRender(ReSurface r) {

            float k = knob.asFloat();

            if (!Float.isFinite(k)) //HACK
                k = 1f;
            else
                k = Util.unitize(k);

            ((FloatSliderModel.Knob)slider.ui).knob = k;

            return super.preRender(r);
        }
    }
}
