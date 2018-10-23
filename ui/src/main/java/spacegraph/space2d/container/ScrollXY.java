package spacegraph.space2d.container;

import jcog.TODO;
import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.grid.*;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.util.math.v2;

import java.util.List;
import java.util.function.Function;

import static spacegraph.space2d.widget.slider.SliderModel.KnobHoriz;
import static spacegraph.space2d.widget.slider.SliderModel.KnobVert;

/**
 * see also:
 * https:
 * https:
 */
public class ScrollXY<S extends ScrollXY.ScrolledXY> extends Bordering {


    private final S model;

    /**
     * proportional in scale to bounds
     */
    private static final float defaultScrollEdge = 0.12f;

    private final FloatSlider scrollX, scrollY, scaleW, scaleH;

    /**
     * current view, in local grid coordinate
     */
    private volatile RectFloat view;
    private volatile v2 viewMin, viewMax;

    private boolean autoHideScrollForSingleColumnOrRow = false;


    public <X> ScrollXY(GridModel<X> grid, GridRenderer<X> renderer) {
        this((S) new DynGrid<>(grid, renderer));
    }

    /**
     * by default, only the first cell will be visible
     */
    public ScrollXY(S scrollable) {
        super();

        borderSize(defaultScrollEdge);

        scrollable.update(this);
        if (viewMin == null)
            throw new NullPointerException("view min set by " + scrollable);
        if (viewMax == null)
            throw new NullPointerException("view max set by " + scrollable);
        if (view == null)
            view = RectFloat.WH(viewMax.x, viewMax.y); //TODO max reasonable limit


        set(C, new Clipped((Surface) (model = scrollable)));

        set(S, this.scrollX = new FloatProportionalSlider("X", ()->0, ()->view.w/viewMax.x, ()->viewMax.x - view.w, true));
        set(E, this.scrollY = new FloatProportionalSlider("Y", ()->0, ()->view.h/viewMax.y, ()->viewMax.y - view.h, false));

        set(N, new Gridding(
                new EmptySurface(),
                this.scaleW = new FloatSlider("W",
                        new FloatSlider.FloatSliderModel() {

                            {
                                setValue(view.w);
                            }

                            @Override
                            public float min() {
                                return viewMin.x;
                            }

                            @Override
                            public float max() {
                                return viewMax.x;
                            }
                        }
                ).type(KnobHoriz),
                new EmptySurface()
        ));
        set(W, new Gridding(
                new EmptySurface(),
                this.scaleH = new FloatSlider("H",
                        new FloatSlider.FloatSliderModel() {

                            {
                                setValue(view.h);
                            }

                            @Override
                            public float min() {
                                return viewMin.y;
                            }

                            @Override
                            public float max() {
                                return viewMax.y;
                            }
                        }
                ).type(KnobVert),
                new EmptySurface()
        ));
        scrollX.on((sx, x) -> scroll(x, view.y, view.w, view.h));
        scrollY.on((sy, y) -> scroll(view.x, y, view.w, view.h));
        scaleW.on((sx, w) -> scroll(view.x, view.y, w, view.h));
        scaleH.on((sy, h) -> scroll(view.x, view.y, view.w, h));

    }

    public void viewMax(v2 viewMax) {
        this.viewMax = viewMax;
        //TODO update if changed
    }

    public void viewMin(v2 viewMin) {
        this.viewMin = viewMin;
        //TODO update if changed
    }

    public ScrollXY<S> view(RectFloat view) {
        if (this.view == null) {
            this.view = view; //initial
            layoutModel();
        } else {

            //break suspected deadlock
            //Exe.invoke(() -> {
                //this.view = view.fence(RectFloat.WH(viewMax.x, viewMax.y)); //TODO cache
                this.view = view;
                layoutModel();
            //});
        }
        return this;
    }

    /** manually trigger layout */
    public final void update() {
        model.update(this);
        layoutModel();
    }

    private void layoutModel() {
        S m = this.model;
        if (m instanceof Container)
            ((Container)m).layout();
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
    public ScrollXY<S> setScrollBar(boolean xOrY, boolean scrollVisible, boolean scaleVisible) {
        if (xOrY) {
            scrollX.visible(scrollVisible);
            borderSize(S, scrollVisible ? defaultScrollEdge : 0);
            scaleW.visible(scaleVisible);
            borderSize(N, scaleVisible ? defaultScrollEdge : 0);
        } else {
            scrollY.visible(scrollVisible);
            borderSize(E, scrollVisible ? defaultScrollEdge : 0);
            scaleH.visible(scaleVisible);
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


    /**
     * sets the x, y position as a fraction of the entire model bounds.
     * if a coordinate is NaN, that coordinate is not affected,
     * allowing shift of either or both X and Y coordinates of the
     * visible cell window.
     */
    public ScrollXY<S> view(float x, float y, float w, float h) {
        return view(RectFloat.X0Y0WH(x, y, w, h));
    }

    protected void scroll(float x, float y, float w, float h) {

        float x1, x2, y1, y2;

        float maxW = viewMax.x, maxH = viewMax.y;

        if (maxW == 1 && autoHideScrollForSingleColumnOrRow) {
            x1 = 0;
            x2 = 1;
            setScrollBar(true, false, false);
        } else {
            x1 = x;
            x2 = x1 + w;
//            if (w < maxW) {
//                x = ((((x / maxW) - 0.5f) * 2 /* -1..+1 */ * (1f - w / maxW)) / 2 + 0.5f) * maxW;
//            } else {
//                x = maxW / 2;
//            }
//            x1 = (x - w / 2);
//            x2 = (x + w / 2);
        }

        if (maxH == 1 && autoHideScrollForSingleColumnOrRow) {
            y1 = 0;
            y2 = 1;
            setScrollBar(false, false, false);
        } else {

            y1 = y;
            y2 = y1 + h;
//            if (h < maxH) {
//                y = ((((y / maxH) - 0.5f) * 2 /* -1..+1 */ * (1f - h / maxH)) / 2 + 0.5f) * maxH;
//            } else {
//
//                y = maxH / 2;
//            }
//            y1 = (y - h / 2);
//            y2 = (y + h / 2);


        }

        view(x1, y1, x2-x1, y2-y1);

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


    public static <X> ScrollXY<DynGrid<X>> array(GridRenderer<X> builder, X... list) {
        return new ScrollXY<>(ListModel.of(list), builder);
    }

    public static <X> ScrollXY<DynGrid<X>> list(GridRenderer<X> builder, List<X> list) {
        return new ScrollXY<>(ListModel.of(list), builder);
    }

    public static <X> ScrollXY<DynGrid<X>> list(Function<X, Surface> builder, List<X> list) {
        return new ScrollXY<>(ListModel.of(list), GridRenderer.value(builder));
    }

    public static <X> ScrollXY<DynGrid<X>> listCached(Function<X, Surface> builder, List<X> list, int cacheCapacity) {
        return new ScrollXY<>(ListModel.of(list), GridRenderer.valueCached(builder, cacheCapacity));
    }

    private class FloatProportionalSlider extends FloatSlider {

        /** proportional knob width */
        private final FloatSupplier knob;

        public FloatProportionalSlider(String label, FloatSupplier min, FloatSupplier knob, FloatSupplier max, boolean hOrV) {
            super(label, new FloatSlider.FloatSliderModel() {
                    @Override
                    public float min() {
                        return min.asFloat();
                    }

                    @Override
                    public float max() {
                        return max.asFloat();
                    }
                }
            );

            this.knob = knob;
            type(hOrV ? new KnobHoriz() : new KnobVert());
        }

        @Override
        public boolean prePaint(SurfaceRender r) {

            float k = knob.asFloat();

            if (!Float.isFinite(k)) //HACK
                k = 1f;
            else
                k = Util.unitize(k);

            ((FloatSliderModel.Knob)slider.ui).knob = k;

            return super.prePaint(r);
        }
    }
}
