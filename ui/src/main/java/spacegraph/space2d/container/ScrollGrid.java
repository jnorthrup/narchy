package spacegraph.space2d.container;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.video.Draw;

import static jcog.Util.short2Int;

/** see also:
 *      https://www.youtube.com/watch?v=ffMpyLQ9YA0
 *      https://www.youtube.com/watch?v=Gi_hoOLFtpo
 * */
public class ScrollGrid<X> extends Bordering {


    static final float MIN_DISPLAYED_CELLS = 0.25f; //max zoom-in hard limit
    static final int MAX_DISPLAYED_CELLS_X = 32;    //max zoom-out hard limit
    static final int MAX_DISPLAYED_CELLS_Y = 32;    //max zoom-out hard limit

    private final GridModel<X> model;
    private final GridRenderer<X> render;
    private final MutableContainer content;

    private final ConcurrentFastIteratingHashMap<Integer, GridCell<X>> cache =
            new ConcurrentFastIteratingHashMap(new GridCell[0]);

    private final FloatSlider sliderX, sliderY, sliderW, sliderH;
    /**
     * current view, in local grid coordinate
     */
    RectFloat2D view = RectFloat2D.Zero;

    /**
     * sub-grid offset (remainder)
     */
    @Deprecated
    transient private float ox = 0, oy = 0;

    /**
     * caches the x,y ranges of cells which are at least partially visible
     */
    private volatile transient short cellVisXmin = 0, cellVisXmax = 0, cellVisYmin = 0, cellVisYmax = 0;

    public ScrollGrid(GridModel<X> model, GridRenderer<X> render, int visX, int visY) {
        this(model, render);
        view(0, 0, visX, visY);
    }

    /**
     * by default, only the first cell will be visible
     */
    public ScrollGrid(GridModel<X> model, GridRenderer<X> render) {
        super();
        this.model = model;
        this.render = render;

        set(C, new Clipped(content = new MutableContainer(
                //true
                false
        ) {
            @Override
            protected void doLayout(int dtMS) {
                if (parent != null) {

                    float dx = content.x();
                    float dy = content.y();
                    float ww = content.w();
                    float hh = content.h();
                    float cw = ww / view.w;
                    float ch = hh / view.h;

                    //refresh cache
                    boolean changedContent = false;

                    short cellVisXmax = ScrollGrid.this.cellVisXmax;
                    short cellVisXmin = ScrollGrid.this.cellVisXmin;
                    short cellVisYmax = ScrollGrid.this.cellVisYmax;
                    short cellVisYmin = ScrollGrid.this.cellVisYmin;

                    for (short sx = cellVisXmin; sx < cellVisXmax; sx++) {
                        for (short sy = cellVisYmin; sy < cellVisYmax; sy++) {
                            changedContent |= ScrollGrid.this.set(sx, sy, model.get(sx, sy), true);
                        }
                    }
                    //remove or hibernate cache entry surfaces which are not visible
                    //and set the layout positions of those which are
                    cache.forEachValue(e -> {
                        int cellID = e.cell;
                        Surface s = e.surface;

                        boolean deleted = false;
                        if (s == null) { //remove the unused entry
                            deleted = true;
                        } else {
                            short sx = (short) (cellID >> 16);
                            short sy = (short) (cellID & 0xffff);
                            if (!cellVisible(sx, sy)) {
                                e.surface = null;
                                content.remove(s); //remove the surface
                                deleted = true;  //remove the entry
                            } else {

                                //layout(s, x, y);
                                float cx = dx + (sx - view.x + 0.5f) * cw;
                                float cy = dy + (sy - view.y + 0.5f) * ch;
                                s.pos(RectFloat2D.XYWH(cx, cy, cw, ch));
                            }
                        }

                        if (deleted) {
                            cache.remove(cellID);
                        }
                    });


                }

                super.doLayout(dtMS);
            }
        }));

        set(S, this.sliderX = new FloatSlider("X",
                new FloatSlider.FloatSliderModel(0 /* left initial pos */) {
                    @Override
                    public float min() {
                        return 0;
                    }

                    @Override
                    public float max() {
                        return model.cellsX();
                    }
                }
        ).type(SliderModel.Knob));

        set(E, this.sliderY = new FloatSlider("Y",
                new FloatSlider.FloatSliderModel(0) {
                    @Override
                    public float min() {
                        return 0;
                    }

                    @Override
                    public float max() {
                        return model.cellsY();
                    }
                }
        ).type(SliderModel.Knob)); //TODO make vertical


        set(N, new Gridding(
                new EmptySurface(), //HACK
                this.sliderW = new FloatSlider("W",
                        new FloatSlider.FloatSliderModel(1) {

                            @Override
                            public float min() {
                                return MIN_DISPLAYED_CELLS;
                            }

                            @Override
                            public float max() {
                                return Math.min(model.cellsX(), MAX_DISPLAYED_CELLS_X);
                            }
                        }
                ),
                new EmptySurface()  //HACK
        ));
        set(W, new Gridding(
                new EmptySurface(), //HACK
                this.sliderH = new FloatSlider("H",
                        new FloatSlider.FloatSliderModel(1) {

                            @Override
                            public float min() {
                                return MIN_DISPLAYED_CELLS;
                            }

                            @Override
                            public float max() {
                                return Math.min(model.cellsY(), MAX_DISPLAYED_CELLS_Y);
                            }
                        }
                ),
                new EmptySurface()  //HACK
        ));
        sliderX.on((sx, x) -> view(x, view.y));
        sliderY.on((sy, y) -> view(view.x, y));
        sliderW.on((sx, w) -> view(view.x, view.y, w, view.h));
        sliderH.on((sy, h) -> view(view.x, view.y, view.w, h));

    }

    static boolean invalidCoordinate(float xy) {
        return xy < 0 || xy > Short.MAX_VALUE - 1;
    }

    public static void main(String[] args) {

        GridModel<String> model = new GridModel<>() {

            @Override
            public String get(int x, int y) {
                return x + "," + y;
            }

            @Override
            public int cellsX() {
                return 64;
            }

            @Override
            public int cellsY() {
                return 64;
            }
        };
        SpaceGraph.window(new ScrollGrid<String>(model,
                (x, y, s) -> {
                    Surface p = new PushButton(s) {
                        @Override
                        protected void paintWidget(GL2 gl, RectFloat2D bounds) {
                            Draw.colorHash(gl, x ^ y, 0.2f, 0.3f, 0.85f);
                            Draw.rect(gl, bounds);
                        }
                    };
                    return new Widget(p);
                }, 8, 4), 1024, 800);
    }

    public ScrollGrid view(float x, float y) {
        return view(x, y, view.w, view.h);
    }

    private ScrollGrid view(RectFloat2D v) {
        return view(v.x, v.y, v.w, v.h);
    }

    /**
     * sets the x, y position as a fraction of the entire model bounds.
     * if a coordinate is NaN, that coordinate is not affected,
     * allowing shift of either or both X and Y coordinates of the
     * visible cell window.
     */
    public ScrollGrid view(float x, float y, float w, float h) {

        float px = x;
        float py = y;

        RectFloat2D v = view;

        float x1, x2, y1, y2;
        float maxW = model.cellsX();
        w = Math.min(w, maxW);

        x = ((((x/maxW)-0.5f)*2 /* -1..+1 */ * (1f - w/maxW)) / 2 + 0.5f ) * maxW;
        x1 = (x-w/2);
        x2 = (x+w/2);
        if (x1 < 0) { x1 = 0; x2 = w; }
        if (x2 > maxW) { x2 = maxW; x1 = maxW-w; }

        float maxH = model.cellsY();
        h = Math.min(h, maxH);
        y = ((((y/maxH)-0.5f)*2 /* -1..+1 */ * (1f - h/maxH)) / 2 + 0.5f ) * maxH;
        y1 = (y-h/2);
        y2 = (y+h/2);
        if (y1 < 0) { y1 = 0; y2 = h; }
        if (y2 > maxH) { y2 = maxH; y1 = maxH-h; }

        RectFloat2D nextView = RectFloat2D.XYXY(x1, y1, x2, y2);
        if (!v.equals(nextView, Spatialization.EPSILONf)) {
//            sliderX.value(px); //for when invoked by other than the slider
//            sliderY.value(py); //for when invoked by other than the slider
//            sliderW.value(w); //for when invoked by other than the slider
//            sliderH.value(h); //for when invoked by other than the slider
        }


        float vLeft = x1;
        short vLeftI = (short) Math.floor(x1);
        float vTop = y1;
        short vTopI = (short) Math.floor(y1);
        this.ox = vLeft - vLeftI;
        this.oy = vTop - vTopI;
        short vRightI = (short) Math.ceil(x2 + 1);
        short vBottomI = (short) Math.ceil(y2 + 1);

        if (invalidCoordinate(vLeftI) || invalidCoordinate(vRightI) || vRightI <= vLeftI)
            throw new RuntimeException("non-positive width or x coordinate: " + vLeftI + ".." + vRightI);
        if (invalidCoordinate(vTopI) || invalidCoordinate(vBottomI) || vBottomI <= vTopI)
            throw new RuntimeException("non-positive height or y coordinate: " + vTopI + ".." + vBottomI);

        cellVisXmin = vLeftI;
        cellVisYmin = vTopI;
        cellVisXmax = vRightI;
        cellVisYmax = vBottomI;

        view = nextView;

        System.out.println(view);

        content.layout(); //layout regardless because the sub-grid position may have changed

        return this;
    }

    public String summary() {
        return (view + " -> [" +
                cellVisXmin + ".." + cellVisXmax + "," +
                cellVisYmin + ".." + cellVisYmax + "] + ("
                + ox + "," + oy + ")"
        );
    }

    /**
     * refresh visible cells
     */
    @Override
    protected void doLayout(int dtMS) {


        super.doLayout(dtMS);

//        System.out.println(cache.size());
//        System.out.println(content.size());
    }

    public final boolean set(short x, short y, @Nullable X v) {
        return set(x, y, v, false);
    }

    /**
     * allows a model to asynchronously report changes, which may be visible or not.
     * set 'v' to null to remove an entry (followed by a subsequent non-null 'v'
     * is a way to force rebuilding of a cell.)
     * returns if there was a change
     */
    protected boolean set(short x, short y, @Nullable X v, boolean force) {

        if (!force && !cellVisible(x, y))
            return false; //ignore

        GridCell<X> entry = cache.computeIfAbsent(short2Int(x, y), (index) -> {
//            if (existingEntry != null) {
//
//                if (v == null || existingEntry.value == null || existingEntry.value.equals(v)) {
//                    return existingEntry; //same value or removal
//                }
//            }
//
            return new GridCell<>(index, v); //replace or create new cell
        });

        Surface existingSurface = entry.surface;

        boolean create = false, delete = false;

        if (existingSurface != null) {
            if (v == null) {
                //removal
                delete = true;
            } else {
                if (entry.value.equals(v)) {
                    //equal value, dont re-create surface
                } else {
                    delete = true;
                    create = true;
                }
            }
        } else { //if (existingSurface == null) {
            create = true;
        }

        if (delete) {
            entry.surface = null;
            content.remove(existingSurface);
        }

        if (create)
            content.add(entry.surface = render.apply(x, y, entry.value));

        return create || delete;
    }

    /**
     * test if a cell is currently visible
     */
    public boolean cellVisible(short x, short y) {
        return (x >= cellVisXmin && x < cellVisXmax)
                &&
                (y >= cellVisYmin && y < cellVisYmax);
    }

//    /** x and y should correspond to a currently visible cell */
//    protected void layout(Surface s, short x, short y) {
//    }

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            model.start(this);

            layout();

            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            model.stop();
            return true;
        }
        return false;
    }


    public interface GridModel<X> {
        int cellsX();

        int cellsY();

        /**
         * return null to remove the content of a displayed cell
         */
        @Nullable X get(int x, int y);

        default void start(ScrollGrid<X> x) {
        }

        default void stop() {
        }
    }

    @FunctionalInterface
    interface GridRenderer<X> {
        Surface apply(int x, int y, X value);
    }

    static class GridCell<X> {
        /**
         * x,y coordinates of this cell encoded as a pair of 16-bit short's
         */
        public final int cell;
        final X value;
        Surface surface;

        GridCell(short x, short y, X value) {
            this(Util.short2Int(x, y), value);
        }

        GridCell(int cell, X value) {
            this.cell = cell;
            this.value = value;
        }

    }

}

//package automenta.spacenet.space.object.data;
//
//        import java.util.Map;
//
//        import javolution.util.FastMap;
//        import automenta.spacenet.Starts;
//        import automenta.spacenet.act.Repeat;
//        import automenta.spacenet.space.geom2.Rect;
//        import automenta.spacenet.space.object.widget.slider.Slider;
//        import automenta.spacenet.space.object.widget.slider.Slider.SliderType;
//        import automenta.spacenet.var.number.DoubleVar;
//        import automenta.spacenet.var.number.IfDoubleChanges;
//        import automenta.spacenet.var.number.IntegerVar;
//        import automenta.spacenet.var.vector.IfVector2Changes;
//        import automenta.spacenet.var.vector.Vector2;
//
//
///** displays a sub-matrix of a 2D matrix array of rectangles, useful for grid arrangements and (editable) text rectangles */
//public class MatrixRect extends Rect implements Starts {
//
//
//
//    DoubleVar cellAspect = new DoubleVar(1.0);
//
//
//    DoubleVar minX = new DoubleVar(0);
//    DoubleVar minY = new DoubleVar(0);
//    DoubleVar maxX = new DoubleVar(0);
//    DoubleVar maxY = new DoubleVar(0);
//
//    DoubleVar visCX = new DoubleVar(0);
//    DoubleVar visCY = new DoubleVar(0);
//    DoubleVar visWidth = new DoubleVar(0);
//    DoubleVar visHeight = new DoubleVar(0);
//
//    private Map<Integer, Map<Integer,Rect>> cell = new FastMap();
//
//    protected Rect content;
//
//    private Slider xSlider, ySlider;
//
//
//    private DoubleVar updatePeriod = new DoubleVar(0.02);
//
//
//    protected boolean needsRefresh = false;
//
//
//    private IntegerVar numCells = new IntegerVar(0);
//
//
//    private DoubleVar cellAspectMin = new DoubleVar(1);
//    private DoubleVar cellAspectMax = new DoubleVar(1);
//
//
//    private DoubleVar maxWidth = new DoubleVar(0);
//    private DoubleVar maxHeight = new DoubleVar(0);
//
//
//    private Slider widthSlider;
//
//
//    private Slider heightSlider;
//
//
//    protected DoubleVar sliderCX = new DoubleVar(0);
//    protected DoubleVar sliderCY = new DoubleVar(0);
//
//    private DoubleVar sliderMaxCX = new DoubleVar(0);
//    private DoubleVar sliderMaxCY = new DoubleVar(0);
//    private DoubleVar sliderMinCX = new DoubleVar(0);
//    private DoubleVar sliderMinCY = new DoubleVar(0);
//
//    private DoubleVar sliderVisWidth = new DoubleVar(0);
//    private DoubleVar sliderVisHeight = new DoubleVar(0);
//    private DoubleVar sliderMaxWidth = new DoubleVar(0);
//    private DoubleVar sliderMaxHeight = new DoubleVar(0);
//
//
//    private DoubleVar autoAspectScale = new DoubleVar(-1.0);
//
//
//    private DoubleVar sliderMaxScale = new DoubleVar(5.0);
//
//
//    private Slider scaleSlider;
//
//    public MatrixRect() {
//        super();
//    }
//
//    @Override public void start() {
//
//        content = add(new Rect());
//        content.tangible(false);
//
//        xSlider = add(new Slider(sliderCX, sliderMinCX, sliderMaxCX, new DoubleVar(0.1), SliderType.Horizontal));
//        ySlider = add(new Slider(sliderCY, sliderMinCY, sliderMaxCY, new DoubleVar(0.1), SliderType.Vertical));
//        xSlider.span(-0.4, -0.45 , 0.4, -0.5);
//        ySlider.span(0.45, -0.4 , 0.5, 0.4);
//
//
//        scaleSlider = add(new Slider(sliderVisHeight, new DoubleVar(1), sliderMaxScale, new DoubleVar(0.2), SliderType.Vertical));
//        widthSlider = add(new Slider(sliderVisWidth, new DoubleVar(1), sliderMaxWidth, new DoubleVar(0.2), SliderType.Horizontal));
//        heightSlider = add(new Slider(sliderVisHeight, new DoubleVar(1), sliderMaxHeight, new DoubleVar(0.2), SliderType.Vertical));
//
//        add(new IfDoubleChanges(getVisCX(), getVisCY(), getVisWidth(), getVisHeight()) {
//            @Override public void afterDoubleChanges(DoubleVar doubleVar, Double previous, Double next) {
//                sliderCX.set(getVisCX().d());
//                sliderCY.set(getVisCY().d());
//                sliderVisWidth.set(getVisWidth().d());
//                sliderVisHeight.set(getVisHeight().d());
//            }
//        });
//
//        add(new IfDoubleChanges(sliderCX, sliderCY, sliderVisWidth, sliderVisHeight) {
//            @Override public void afterDoubleChanges(DoubleVar doubleVar, Double previous, Double next) {
//                needsRefresh = true;
//            }
//        });
//        add(new IfVector2Changes(getAbsoluteSize()) {
//            @Override public void afterVectorChanged(Vector2 v, double dx, double dy) {
//                double autoAspectScale = getAutoAspectScale().d();
//                if (autoAspectScale!=-1) {
//                    needsRefresh = true;
//                }
//            }
//        });
//
//
//        add(new Repeat() {
//            @Override public double repeat(double t, double dt) {
//                updateMatrix();
//                return getUpdatePeriod().get();
//            }
//        });
//
//        needsRefresh = true;
//        updateMatrix();
//    }
//
//    protected void updateMatrix() {
//        if (needsRefresh) {
//            layout();
//        }
//    }
//
//    private DoubleVar getMaxWidth() {
//        return maxWidth;
//    }
//    private DoubleVar getMaxHeight() {
//        return maxHeight ;
//    }
//
//
//    protected DoubleVar getUpdatePeriod() {
//        return updatePeriod ;
//    }
//
//    @Override public void stop() {	}
//
//    public void removeAll() {
//        synchronized (cell) {
//
//            getNumCells().set(0);
//            cell.clear();
//
//            if (content!=null)
//                content.clear();
//
//            needsRefresh = true;
//        }
//
//    }
//
//    public void put(int x, int y, Rect r) {
//        synchronized (cell) {
//
//            Map<Integer, Rect> row = cell.get(y);
//            if (row == null) {
//                row =new FastMap<Integer,Rect>();
//                cell.put(y, row);
//            }
//
//            if (row.get(x)!=null) {
//                Rect removed = row.remove(x);
//                if (removed == r)
//                    return;
//                content.remove(removed);
//            }
//            else {
//                getNumCells().add(1);
//            }
//
//            row.put(x, r);
//
//            if (getNumCells().i() == 1) {
//                minX.set(x);
//                maxX.set(x);
//                minY.set(y);
//                maxY.set(y);
//            }
//            else {
//                if (x < minX.i()) minX.set(x);
//                if (x > maxX.i()) maxX.set(x);
//                if (y < minY.i()) minY.set(y);
//                if (y > maxY.i()) maxY.set(y);
//            }
//
//
//            content.add(r);
//
//            needsRefresh = true;
//        }
//    }
//
//    public Rect get(int x, int y) {
//        try {
//            return cell.get(y).get(x);
//        }
//        catch (Exception e) {
//            return null;
//        }
//    }
//
//
//    protected void layout() {
//        synchronized (cell) {
//
//            maxWidth.set(getMaxX().i() - getMinX().i());
//            maxHeight.set(getMaxY().i() - getMinY().i());
//
//
//            double d = Math.ceil(sliderVisHeight.d());
//            d = Math.min( d, getMaxY().d() - getMinY().d() );
//            d = Math.max(1, d);
//            visHeight.set(d);
//
//            d = Math.ceil(sliderVisWidth.d());
//            d = Math.min( d, getMaxX().d() - getMinX().d() );
//            d = Math.max(1, d);
//            visWidth.set(d);
//
//            double autoAspectScale = getAutoAspectScale().d();
//            if (autoAspectScale!=-1) {
//                getAutoAspectScale().set( scaleSlider.getValue().d() );
//                double sx = getAbsoluteSize().x();
//                double sy = getAbsoluteSize().y();
//                double sa = sy / sx;
//
//
//                visWidth.set( (1.0 / sa) * autoAspectScale);
//                visHeight.set(  sa * autoAspectScale );
//
//                scaleSlider.span(-0.5, 0.25, -0.45, -0.25);
//                scaleSlider.visible(true);
//
//                widthSlider.visible(false);
//                heightSlider.visible(false);
//            }
//            else {
//                widthSlider.span(-0.25, 0.5, 0.25, 0.45);
//                heightSlider.span(-0.5, 0.25, -0.45, -0.25);
//
//                widthSlider.visible(true);
//                heightSlider.visible(true);
//
//                scaleSlider.visible(false);
//            }
//
//
//            double v = sliderCX.d();
//            v = Math.max(getMinX().d(), v);
//            v = Math.min(getMaxX().d(), v);
//            visCX.set(v);
//
//            v = sliderCY.d();
//            v = Math.max(getMinY().d(), v);
//            v = Math.min(getMaxY().d(), v);
//            visCY.set(v);
//
//
//
//            double dx = 1 + maxX.d() - minX.d();
//            double dy = 1 + maxY.d() - minY.d();
//
//
//
//            if (getNumCells().i() == 0) {
//                needsRefresh = false;
//                return;
//            }
//
//            content.scale(0.9, 0.9);
//
//            double height = 0;
//            double width = 0;
//
//            double cellAspect = (getCellAspectMax().d() + getCellAspectMin().d()) / 2.0;
//
//            //invert to match slider's output
//            double vy = getMaxY().d() * ( 1.0 - ( getVisCY().d() / (getMaxY().d())));
//
//            int startY = (int)Math.floor(vy - getVisHeight().d()/2.0);
//            int stopY = (int)Math.ceil(vy + getVisHeight().d()/2.0);
//            int startX = (int)Math.floor(getVisCX().d() - getVisWidth().d()/2.0);
//            int stopX = (int)Math.ceil(getVisCX().d() + getVisWidth().d()/2.0);
//
//            if (startX < getMinX().i()) {
//                startX = getMinX().i();
//                stopX = getMinX().i() + getVisWidth().i();
//            }
//            if (stopX > getMaxX().i()) {
//                stopX = getMaxX().i();
//                startX = getMaxX().i() - getVisWidth().i();
//            }
//            if (startY < getMinY().i()) {
//                startY = getMinY().i();
//                stopY = getMinY().i() + getVisHeight().i();
//            }
//            if (stopY > getMaxY().i()) {
//                stopY = getMaxY().i();
//                startY = getMaxY().i() - getVisHeight().i();
//            }
//
//            startX = Math.max( startX, getMinX().i() );
//            stopX = Math.min( getMaxX().i(), stopX );
//
//            startY = Math.max( startY, getMinY().i() );
//            stopY = Math.min( getMaxY().i(), stopY );
//
//            //calculate width and height to normalize
//            for (int y = startY; y <= stopY; y++) {
//                double yScale = cellAspect;
//
//                Map<Integer, Rect> row = cell.get(y);
//
//                double w = 0;
//                if (row!=null) {
//                    for (int x = startX; x <= stopX; x++) {
//                        double xScale = 1.0 / cellAspect;
//                        w += xScale;
//                    }
//                }
//                if (w > width)
//                    width = w;
//
//                height += yScale;
//            }
//
//            double px;
//            double py = -0.5;
//
//            for (int y = getMinY().i(); y <= getMaxY().i(); y++) {
//                px = -0.5;
//                double yScale = cellAspect/height;
//                Map<Integer, Rect> row = cell.get(y);
//
//                if (row!=null) {
//                    for (int x = getMinX().i(); x <= getMaxX().i(); x++) {
//                        Rect r = row.get(x);
//                        if (r!=null) {
//                            r.visible(false);
//                        }
//                    }
//                }
//            }
//
//            for (int y = stopY; y >= startY; y--) {
//                px = -0.5;
//                double yScale = cellAspect/height;
//                Map<Integer, Rect> row = cell.get(y);
//
//                if (row!=null) {
//                    for (int x = startX; x <= stopX; x++) {
//                        double xScale = 1.0 / cellAspect / width;
//                        Rect r = row.get(x);
//                        if (r!=null) {
//                            r.getPosition().set(px + xScale/2.0, py + yScale/2.0, 0);
//                            r.getSize().set(xScale, yScale);
//                            r.visible(true);
//                        }
//                        px += xScale;
//                    }
//                }
//                py += yScale;
//            }
//
////			sliderCX.set(visCX.d());
////			sliderCY.set(visCY.d());
//
//            sliderMaxWidth.set(getMaxWidth().d());
//            sliderMaxHeight.set(getMaxHeight().d());
//
//            xSlider.getKnobLength().set( Math.min(1.0, getVisWidth().d() / getMaxWidth().d()) );
//            ySlider.getKnobLength().set( Math.min(1.0, getVisHeight().d() / getMaxHeight().d()) );
//
//            sliderMinCX.set(getMinX().d() );
//            sliderMaxCX.set(getMaxX().d() );
//
//            sliderMinCY.set(getMinY().d() );
//            sliderMaxCY.set(getMaxY().d() );
//
//            //System.out.println(getVisWidth().d() + ", " + getVisHeight().d() + " : " + startX + ".." + stopX + "," + startY + ".." + stopY);
//
//
////			System.out.println(visWidth.d() + ": " + sliderMinCX.d() + " < " +sliderCX.d() + " < " + sliderMaxCX.d());
////			System.out.println(visHeight.d() + ": " + sliderMinCY.d() + " < " +sliderCY.d() + " < " + sliderMaxCY.d());
////			System.out.println(sliderMaxWidth.d() + " , " + sliderMaxHeight.d());
//
//            needsRefresh = false;
//        }
//
//    }
//
//
//    /** if =-1, auto aspect is disabled */
//    public DoubleVar getAutoAspectScale() {
//        return autoAspectScale ;
//    }
//
//    public IntegerVar getNumCells() {
//        return numCells ;
//    }
//
//    /** maximum cell X coordinate */
//    public DoubleVar getMaxX() {	return maxX;	}
//
//    /** maximum cell Y coordinate */
//    public DoubleVar getMaxY() {	return maxY;	}
//
//    /** minimum cell X coordinate */
//    public DoubleVar getMinX() {	return minX;	}
//
//    /** minimum cell Y coordinate */
//    public DoubleVar getMinY() {	return minY;	}
//
//    /** center X of visible cells */
//    public DoubleVar getVisCX() {
//        return visCX;
//    }
//
//    /** center Y of visible cells */
//    public DoubleVar getVisCY() {
//        return visCY;
//    }
//
//    /** number of visible cells tall */
//    public DoubleVar getVisHeight() {
//        return visHeight;
//    }
//
//    /** number of visible cells wide */
//    public DoubleVar getVisWidth() {
//        return visWidth;
//    }
//
//    public DoubleVar getCellAspectMin() {
//        return cellAspectMin;
//    }
//    public DoubleVar getCellAspectMax() {
//        return cellAspectMax;
//    }
//
//    //	protected double getXScale(double x) {
//    //	double dx = Math.abs( x - getCx().d() );
//    //	double dw = getWidth().d()/2.0;
//    //	if (dx < dw) {
//    //		return 1.0;
//    //	}
//    //	double sx = 1.0 / (dx - dw + 1.0);
//    //	if (sx < getVisibleThreshold()) {
//    //		return 0.0;
//    //	}
//    //	return sx;
//    //}
//    //
//    //protected double getYScale(double y) {
//    //	double dy = Math.abs( y - getCy().d() );
//    //	double dh = getHeight().d()/2.0;
//    //	if (dy < dh) {
//    //		return 1.0;
//    //	}
//    //	double sy = 1.0 / (dy - dh + 1.0);
//    //	if (sy < getVisibleThreshold()) {
//    //		return 0.0;
//    //	}
//    //	return sy;
//    //}
//    //
//    //public double getVisibleThreshold() {
//    //	return 0.05;
//    //}
//
//    public Rect getContent() {
//        return content;
//    }
//
//}
