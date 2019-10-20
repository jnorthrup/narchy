package spacegraph.space2d.container.grid;

import jcog.data.map.CellMap;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.container.collection.MutableMapContainer;

import static jcog.Util.short2Int;

/**
 * "infinite" scrollable grid, possibly only partially visible at a given time
 * internally stores cells as hashed 2D coordinates entries in 16-bit pairs of x,y coordinates
 */
public class DynGrid<X> extends MutableMapContainer<Integer, X> implements ScrollXY.ScrolledXY {

    volatile int x1 = 0;
    volatile int y1 = 0;
    volatile int x2 = 1;
    volatile int y2 = 1;
    private transient float dx;
    private transient float dy;
    private transient float cw;
    private transient float ch;

    private final GridModel<X> model;
    private final GridRenderer<X> render;


    public DynGrid(GridModel<X> model, GridRenderer<X> render) {
        super();
        this.model = model;
        this.render = render;
    }


    @Override
    protected void starting() {
        super.starting();
        model.start(this);
    }

    @Override
    protected void stopping() {
        model.stop(this);
        super.stopping();
    }

    @Override
    protected void hide(X x, Surface s) {
        render.hide(x, s);
    }

    protected Surface surface(short x, short y, X nextValue) {
        return render.apply(x, y, nextValue);
    }

    protected X value(short sx, short sy) {
        return model.get(sx, sy);
    }

//    @Override
//    public Surface finger(Finger finger) {
//        Surface inner = super.finger(finger);
//        final int moveDragButton = 1;
//        if ((inner == null || inner == this) && finger.pressing(moveDragButton)) {
//            if (finger.tryFingering(new FingerMove(moveDragButton,
//                    0.05f, 0.05f) {
//
//                final float sx = view.x;
//                final float sy = view.y;
//
//                @Override
//                public void move(float tx, float ty) {
//                    //view(sx - tx, sy - ty);
//                }
//            }))
//                return this;
//        }
//        return inner;
//
//
//    }


    /**
     * test if a cell is currently visible
     */
    boolean cellVisible(short x, short y) {
        return (x >= x1 && x < x2)
                &&
                (y >= y1 && y < y2);
    }




    @Override
    protected void doLayout(float dtS) {

        if (parent == null)
            return;

        ScrollXY xy = parentOrSelf(ScrollXY.class);
        RectFloat v = xy.view();
        float vx = v.x, vy = v.y, vw = v.w, vh = v.h;
        this.x1 = Math.max(0, (int) Math.floor(vx));
        this.y1 = Math.max(0, (int) Math.floor(vy));
        this.x2 = Math.min(cellsX(),(int) Math.ceil(vx + vw));
        this.y2 = Math.min(cellsY(), (int) Math.ceil(vy + vh));

        dx = x();
        dy = y();
        float ww = w();
        float hh = h();
        cw = ww / vw;
        ch = hh / vh;


        cells.map.removeIf(e -> {
            Surface s = ((SurfaceCacheCell) e).surface;

            if (s == null) {
                //return true;
            } else {
                int cellID = e.key;
                short sx = (short) (cellID >> 16);
                short sy = (short) (cellID & 0xffff);
                return !cellVisible(sx, sy);
            }

            return false;
        });





        for (short sx = (short) x1; sx < x2; sx++) {
            for (short sy = (short) y1; sy < y2; sy++) {
                SurfaceCacheCell e = (SurfaceCacheCell) set(sx, sy, value(sx, sy), true);
                if (e != null) {
                    Surface s = e.surface;
                    if (s == null)
                        continue;

                    doLayout(s, vx, vy, sx, sy);
                    if (s.parent == null)
                        s.start(this);
                }
            }
        }


    }


    void doLayout(Surface s, float vx, float vy, short sx, short sy) {
        float cx = dx + (sx - vx + 0.5f) * cw;
        float cy = dy + h() - ((sy - vy + 0.5f) * ch);
        cellVisible(s, cw, ch, cx, cy);
    }


    static void cellVisible(Surface s, float cw, float ch, float cx, float cy) {
        s.pos(RectFloat.XYWH(cx, cy, cw, ch));
    }


    public final void set(short x, short y, @Nullable X v) {
        set(x, y, v, false);
    }

    /**
     * allows a model to asynchronously report changes, which may be visible or not.
     * set 'v' to null to remove an entry (followed by a subsequent non-null 'v'
     * is a way to force rebuilding of a cell.)
     * returns if there was a change
     */
    CellMap.CacheCell set(short x, short y, @Nullable X nextValue, boolean force) {
        if (!force && !cellVisible(x, y))
            return null;

        return put(short2Int(x, y), nextValue, this::renderer);
    }

    private Surface renderer(int cellID, X value) {
        short sx = (short) (cellID >> 16);
        short sy = (short) (cellID & 0xffff);
        return surface(sx, sy, value);
    }


    public int cellsX() {
        return model.cellsX();
    }


    public int cellsY() {
        return model.cellsY();
    }

    @Override
    public void update(ScrollXY s) {
        float minX = 0.5f;
        float minY = 0.5f;
        v2 min = new v2(minX, minY);
        v2 max = new v2(Math.max(minX, cellsX()), Math.max(minY, cellsY()));
        s.viewMinMax(min, max);
        s.view(max); //TODO reasonable # of items cut-off
    }
}
