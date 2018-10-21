package spacegraph.space2d.container.grid;

import jcog.data.map.CellMap;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.container.collection.MutableMapContainer;

import static jcog.Util.short2Int;

/**
 * "infinite" scrollable grid, possibly only partially visible at a given time
 * internally stores cells as hashed 2D coordinates entries in 16-bit pairs of x,y coordinates
 */
public class DynGrid<X> extends MutableMapContainer<Integer, X> implements ScrollXY.ScrolledXY {

    volatile short x1 = 0, y1 = 0, x2 = 1, y2 = 1;
    private transient RectFloat view;
    private transient float dx, dy, cw, ch;

    private final GridModel<X> model;
    private final GridRenderer<X> render;


    public DynGrid(GridModel<X> model, GridRenderer<X> render) {
        super();
        this.model = model;
        this.render = render;
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
            model.stop(this);
            return true;
        }
        return false;
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

    @Override public void layout(RectFloat view, short x1, short y1, short x2, short y2) {
        this.view = view;

        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;

        layout();
    }


    @Override
    protected void doLayout(int dtMS) {

        if (parent == null)
            return;

        dx = x();
        dy = y();
        float ww = w();
        float hh = h();
        cw = ww / view.w;
        ch = hh / view.h;


        cells.map.removeIf(e -> {
            Surface s = ((SurfaceCacheCell) e).surface;

            if (s == null) {
                //return true;
            } else {
                int cellID = e.key;
                short sx = (short) (cellID >> 16);
                short sy = (short) (cellID & 0xffff);
                if (!cellVisible(sx, sy)) {
                    return true;
                }
            }

            return false;
        });


        short x1 = this.x1, y1 = this.y1, x2 = this.x2, y2 = this.y2;

        for (short sx = x1; sx < x2; sx++) {
            for (short sy = y1; sy < y2; sy++) {
                SurfaceCacheCell e = (SurfaceCacheCell) set(sx, sy, value(sx, sy), true);
                if (e != null) {
                    Surface s = e.surface;
                    if (s == null)
                        continue;

                    doLayout(s, sx, sy);
                    if (s.parent == null)
                        s.start(this);
                }
            }
        }

    }


    void doLayout(Surface s, short sx, short sy) {
        float cx = dx + (sx - view.x + 0.5f) * cw;
        float cy = dy + h() - ((sy - view.y + 0.5f) * ch);
        cellVisible(s, cw, ch, cx, cy);
    }


    void cellVisible(Surface s, float cw, float ch, float cx, float cy) {
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


    @Override
    public int cellsX() {
        return model.cellsX();
    }

    @Override
    public int cellsY() {
        return model.cellsY();
    }


}
