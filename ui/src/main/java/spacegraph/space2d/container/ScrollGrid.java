package spacegraph.space2d.container;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.math.FloatRange;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.slider.BaseSlider;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.video.Draw;

import static jcog.Util.short2Int;

public class ScrollGrid<X> extends Bordering {

    private final GridModel<X> model;
    private final GridRenderer<X> render;
    private final MutableContainer content;

    private final ConcurrentFastIteratingHashMap<Integer,GridCell<X>> cache =
            new ConcurrentFastIteratingHashMap(new GridCell[0]);

    static class GridCell<X> {
        final X value;

        /** x,y coordinates of this cell encoded as a pair of 16-bit short's */
        public final int cell;

        Surface surface;

        GridCell(short x, short y, X value) {
            this(Util.short2Int(x, y), value);
        }

        GridCell(int cell, X value) {
            this.cell = cell;
            this.value = value;
        }

    }

    public interface GridModel<X> {
        int cellsX();
        int cellsY();

        /** return null to remove the content of a displayed cell */
        @Nullable X get(int x, int y);

        default void start(ScrollGrid x) { }
        default void stop() { }
    }

    short cellVisXmin = 0, cellVisXmax = 1;
    short cellVisYmin = 0, cellVisYmax = 1;

    private final FloatRange scrollX, scrollY, scrollW, scrollH;
    private final FloatSlider sliderX, sliderY, sliderW, sliderH;


    @FunctionalInterface interface GridRenderer<X> {
        Surface apply(int x, int y, X value);
    }

    public ScrollGrid(GridModel<X> model, GridRenderer<X> render, int visX, int visY) {
        this(model, render);
        view(0, 0, visX, visY);
    }

    /** by default, only the first cell will be visible */
    public ScrollGrid(GridModel<X> model, GridRenderer<X> render) {
        super();
        this.model = model;
        this.render = render;

        set(C, content = new MutableContainer());

        set(S, this.sliderX = new FloatSlider("X",
                scrollX = new FloatRange(0, 0, 1)
        ).type(BaseSlider.Knob));

        set(E, this.sliderY = new FloatSlider("Y",
                scrollY = new FloatRange(0, 0, 1)
        ).type(BaseSlider.Knob)); //TODO make vertical

        set(N, new Gridding(
                new EmptySurface(), //HACK
                this.sliderW = new FloatSlider("W",
                    scrollW = new FloatRange(0.5f, 0, 1)),
                new EmptySurface()  //HACK
        ));
        set(W, new Gridding(
                new EmptySurface(), //HACK
                this.sliderH = new FloatSlider("H",
                    scrollH = new FloatRange(0.5f, 0, 1)),
                new EmptySurface()  //HACK
        ));
        sliderX.on((sx,x)-> viewShift(x, Float.NaN));
        sliderY.on((sy,y)-> viewShift(Float.NaN, y));


    }

    /** sets the x, y position as a fraction of the entire model bounds.
     * if a coordinate is NaN, that coordinate is not affected,
     * allowing shift of either or both X and Y coordinates of the
     * visible cell window.
     */
    public ScrollGrid viewShift(float xFrac, float yFrac) {

        int nx1, nx2, ny1, ny2;

        if (xFrac==xFrac) {
            float visW = (cellVisXmax-cellVisXmin); //how many cells currently visible
            //float visX = (cellVisXmax+cellVisXmin)/2; //coordinate of cell currently centered upon
            int totalW = model.cellsX();
            visW = Math.min(totalW, visW); //in case model shrunk
            float mx = visW/2;
            float visXnext = mx+(xFrac * (totalW-mx*2)); //TODO shrink the actual moveable space based on half of visW
            float x1 = visXnext - visW/2, x2 = visXnext + visW/2;

            if (x2 >= totalW) {
                //hit right
                x2 = totalW;
                x1 = x2 - visW;
            } else if (x1 <= 0) {
                //hit left
                x1 = 0;
                x2 = x1 + visW;
            }
            nx1 = (short)Math.round(x1);
            nx2 = (short)Math.round(x2);
            //System.out.println(xFrac + " " + cellVisXmin + " " + cellVisXmax);
        } else {
            nx1 = cellVisXmin; nx2 = cellVisXmax;
        }

        if (yFrac == yFrac) {
            //TODO ..
            ny1 = cellVisYmin; ny2 = cellVisYmax;
        } else {
            ny1 = cellVisYmin; ny2 = cellVisYmax;
        }

        view(nx1, ny1, nx2, ny2);

        return this;
    }

    public ScrollGrid view(int x1, int y1, int x2, int y2) {
        if (invalidCoordinate(x1) || invalidCoordinate(x2) || x2 <= x1) 
            throw new RuntimeException("non-positive width or x coordinate");
        if (invalidCoordinate(y1) || invalidCoordinate(y2) || y2 <= y1)
            throw new RuntimeException("non-positive height or y coordinate");

        view((short)x1, (short)y1, (short)x2, (short)y2);
        return this;
    }

    static boolean invalidCoordinate(int xy) {
        return xy < 0 || xy > Short.MAX_VALUE - 1;
    }

    /** set viewing window in cell coordinates */
    private ScrollGrid view(short x1, short y1, short x2, short y2) {
        cellVisXmin = x1; cellVisYmin = y1;
        cellVisXmax = x2; cellVisYmax = y2;

        //TODO supress update if these coordinates didnt change

        //refresh cache
        for (short  x = x1; x < x2; x++) {
            for (short y = y1; y < y2; y++) {
                set(x, y, model.get(x, y));
            }
        }

        layout();

        return this;
    }

    /** refresh visible cells */
    @Override protected void doLayout(int dtMS) {

        super.doLayout(dtMS);

        float dx = content.x();
        float dy = content.y();
        float ww = content.w();
        float hh = content.h();
        float cw = ww/(cellVisXmax-cellVisXmin);
        float ch = hh/(cellVisYmax-cellVisYmin);


        //remove or hibernate cache entry surfaces which are not visible
        //and set the layout positions of those which are
        cache.forEachValue(e -> {
            int cellID = e.cell;
            Surface s = e.surface;

            boolean deleted = false;
            if ((s == null) //remove the unused entry
                    ||
                (s.parent == null)) { //the surface removed itself, or something else did
                deleted = true;
            } else {
                short x = (short) (cellID >> 16);
                short y = (short) (cellID & 0xffff);
                if (!cellVisible(x, y)) {
                    content.remove(s); //remove the surface
                    e.surface = null;
                    deleted = true;  //remove the entry
                } else {

                    //layout(s, x, y);
                    float cx = dx + (x - cellVisXmin + 0.5f) * cw;
                    float cy = dy + (y - cellVisYmin + 0.5f) * ch;
                    s.pos(RectFloat2D.XYWH(cx, cy, cw, ch));
                }
            }

            if (deleted) {
                cache.remove(cellID);
            }
        });
        content.layout();
    }

//    /** x and y should correspond to a currently visible cell */
//    protected void layout(Surface s, short x, short y) {
//    }

    /** allows a model to asynchronously report changes, which may be visible or not.
     *  set 'v' to null to remove an entry (followed by a subsequent non-null 'v'
     *  is a way to force rebuilding of a cell.)
     * */
    protected void set(short x, short y, @Nullable X v) {
        if (!cellVisible(x, y))
            return; //ignore

        GridCell<X> entry = cache.compute(short2Int(x, y), (index, existingEntry) -> {
            if (existingEntry != null) {

                if (v == null || existingEntry.value == null || existingEntry.value.equals(v)) {
                    return existingEntry; //same value or removal
                }
            }

            return new GridCell<>(index, v); //replace or create new cell
        });

        if (v == null && entry.surface!=null) {
            //removal
            content.remove(entry.surface);
            layout();
        } else if (entry.surface == null) {
            content.add(entry.surface = render.apply(x, y, entry.value));
            layout();
        } else {
            //no change
        }
    }

    /** test if a cell is currently visible */
    public boolean cellVisible(short x, short y) {
        return (x >= cellVisXmin && x < cellVisXmax)
                &&
               (y >= cellVisYmin && y < cellVisYmax);
    }

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            model.start(this);
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



    public static void main(String[] args) {

        GridModel<String> model = new GridModel<String>() {

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
                (x,y,s) -> {
                    PushButton p = new PushButton(s) {
                        @Override
                        protected void paintWidget(GL2 gl, RectFloat2D bounds) {
                            Draw.colorHash(gl, x ^ y, 0.5f, 0.75f, 0.85f);
                            Draw.rect(gl, bounds);
                        }
                    };
                    return p;
                }, 16, 16), 1024, 800);
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
