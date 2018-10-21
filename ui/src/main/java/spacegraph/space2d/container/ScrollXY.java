package spacegraph.space2d.container;

import jcog.TODO;
import jcog.Util;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.*;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;

import java.util.List;
import java.util.function.Function;

import static spacegraph.space2d.widget.slider.SliderModel.KnobVert;

/** see also:
 *      https:
 *      https:
 * */
public class ScrollXY<S extends ScrollXY.ScrolledXY> extends Bordering {


    private static final float MIN_DISPLAYED_CELLS = 0.25f;
//    private static final int MAX_DISPLAYED_CELLS_X = 32;
    private static final int MAX_DISPLAYED_CELLS = 32;

    private final S model;




    /** proportional in scale to bounds */
    private static final float defaultScrollEdge = 0.12f;

    private final FloatSlider scrollX, scrollY, scaleW, scaleH;

    /**
     * current view, in local grid coordinate
     */
    private volatile RectFloat view = RectFloat.Zero;

    private static final boolean autoHideScrollForSingleColumnOrRow = true;


    public <X> ScrollXY(GridModel<X> grid, GridRenderer<X> renderer) {
        this((S)new DynGrid<>(grid, renderer));
    }

    /**
     * by default, only the first cell will be visible
     */
    public ScrollXY(S scrollable) {
        super();
        borderSize(defaultScrollEdge);


        set(C, new Clipped((Surface)(model = scrollable)));

        set(S, this.scrollX = new FloatSlider("X",
                new FloatSlider.FloatSliderModel() {
                    @Override
                    public float min() {
                        return 0;
                    }

                    @Override
                    public float max() {
                        return model.cellsX();
                    }
                }
        ).type(SliderModel.KnobHoriz));

        set(E, this.scrollY = new FloatSlider("Y",
                new FloatSlider.FloatSliderModel() {
                    @Override
                    public float min() {
                        return 0;
                    }

                    @Override
                    public float max() {
                        return model.cellsY();
                    }
                }
        ).type(KnobVert)); 


        set(N, new Gridding(
                new EmptySurface(), 
                this.scaleW = new FloatSlider("W",
                        new FloatSlider.FloatSliderModel() {

                            {  setValue(1); }

                            @Override
                            public float min() {
                                return MIN_DISPLAYED_CELLS;
                            }

                            @Override
                            public float max() {
                                return Util.clamp(model.cellsX() * 1.25f, MIN_DISPLAYED_CELLS, MAX_DISPLAYED_CELLS);
                            }
                        }
                ),
                new EmptySurface()  
        ));
        set(W, new Gridding(
                new EmptySurface(), 
                this.scaleH = new FloatSlider("H",
                        new FloatSlider.FloatSliderModel() {

                            {  setValue(1); }

                            @Override
                            public float min() {
                                return MIN_DISPLAYED_CELLS;
                            }

                            @Override
                            public float max() {
                                return Util.clamp(model.cellsY() * 1.25f, MIN_DISPLAYED_CELLS, MAX_DISPLAYED_CELLS);
                            }
                        }
                ).type(KnobVert),
                new EmptySurface()  
        ));
        scrollX.on((sx, x) -> view(x, view.y));
        scrollY.on((sy, y) -> view(view.x, y));
        scaleW.on((sx, w) -> view(view.x, view.y, w, view.h));
        scaleH.on((sy, h) -> view(view.x, view.y, view.w, h));


        
        view(0,0, model.cellsX(), model.cellsY());
    }

    /** the current view */
    public final RectFloat view() {
        return view;
    }

    /** set the view window's center of focus, re-using the current width and height */
    private ScrollXY<S> view(float x, float y) {
        return view(x, y, view.w, view.h);
    }

    /** set the view window's center and size of focus, in grid coordinates */
    private ScrollXY<S> view(RectFloat v) {
        return view(v.x, v.y, v.w, v.h);
    }


    /** enables requesting entries from the -1'th row and -1'th column of
     * the model to use as 'pinned' row header cells
     */
    public ScrollXY<S> setHeader(boolean rowOrColumn, boolean enabled) {
        throw new TODO();
    }

    /** enables or disables certain scrollbar-related features per axis */
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

        RectFloat v = view;

        float x1, x2, y1, y2;

        float maxW = model.cellsX();
        float maxH = model.cellsY();

        if (maxW == 1 && autoHideScrollForSingleColumnOrRow) {
            x1 = 0;
            x2 = 1;
            setScrollBar(true, false, false);
        } else {
            if (w < maxW) {
                x = ((((x / maxW) - 0.5f) * 2 /* -1..+1 */ * (1f - w / maxW)) / 2 + 0.5f) * maxW;
            } else {
                x = maxW/2; 
            }
            x1 = (x - w / 2);
            x2 = (x + w / 2);








        }

        if (maxH == 1 && autoHideScrollForSingleColumnOrRow) {
            y1 = 0;
            y2 = 1;
            setScrollBar(false, false, false);
        } else {

            
            if (h < maxH) {
                y = ((((y / maxH) - 0.5f) * 2 /* -1..+1 */ * (1f - h / maxH)) / 2 + 0.5f) * maxH;
            } else {
                
                y = maxH / 2;
            }
            y1 = (y - h / 2);
            y2 = (y + h / 2);








        }

        RectFloat nextView = RectFloat.XYXY(x1, y1, x2, y2);
        if (!v.equals(nextView, Spatialization.EPSILONf)) {




        }

        view = nextView;

        model.layout(
                view,
                (short) Math.max(0, Math.floor(x1)),
                (short) Math.max(0, Math.floor(y1)),
                (short) Math.min(maxW,Math.ceil(x2)),
                (short) Math.min(maxH,Math.ceil(y2))
        ); 

        

        return this;
    }



//    public final void refresh() {
//        view(view);
//    }


    public interface ScrolledXY {
        int cellsX();
        int cellsY();

        void layout(RectFloat view, short x1, short y1, short x2, short y2);
    }


    public static <X> ScrollXY<DynGrid<X>> array(GridRenderer<X> builder, X... list) {
        return new ScrollXY<>( ListModel.of(list), builder);
    }
    public static <X> ScrollXY<DynGrid<X>> list(GridRenderer<X> builder, List<X> list) {
        return new ScrollXY<>( ListModel.of(list), builder);
    }
    public static <X> ScrollXY<DynGrid<X>> list(Function<X,Surface> builder, List<X> list) {
        return new ScrollXY<>( ListModel.of(list), GridRenderer.value(builder));
    }
    public static <X> ScrollXY<DynGrid<X>> listCached(Function<X,Surface> builder, List<X> list, int cacheCapacity) {
        return new ScrollXY<>( ListModel.of(list), GridRenderer.valueCached(builder, cacheCapacity));
    }

}




















































































































































































































































































































































































































































































































