package spacegraph.input.finger;

import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.shape.PathSurface;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.space2d.widget.port.util.Wire;
import spacegraph.util.Path2D;

import javax.annotation.Nullable;

/**
 * the process of drawing a wire between two surfaces
 */
abstract public class Wiring extends FingerDragging {


    public interface Wireable {
        boolean onWireIn(@Nullable Wiring w, boolean active);

        void onWireOut(@Nullable Wiring w, boolean active);

    }

    private Path2D path;

    public final Surface start;
    private PathSurface pathVis;
    protected Surface end = null;

//    protected Windo source() {
//        return start.parent(Windo.class);
//    }
//    public Windo target() {
//        return end!=null ? end.parent(Windo.class) : null;
//    }

    protected Wiring(Surface start) {
        super(2);
        this.start = start;
    }

    @Override
    protected boolean startDrag(Finger f) {
        if (super.startDrag(f)) {
            if (this.start instanceof Wireable)
                ((Wireable) start).onWireOut(this, true);
            return true;
        }
        return false;
    }


    @Override
    public boolean escapes() {
        return true;
    }

    @Override
    protected boolean drag(Finger f) {
        if (path == null) {
            path = new Path2D(64);

            GraphEdit g = graph();
            g.addRaw(pathVis = new PathSurface(path));
        } else {
            updateEnd(f);
        }

        pathVis.add(f.pos, 64);

        return true;
    }

    private GraphEdit graph() {
        return start.parent(GraphEdit.class);
    }

    @Override
    public void stop(Finger finger) {
        if (pathVis != null) {
            graph().removeRaw(pathVis);
            pathVis = null;
        }

        if (end != null) {


            tryWire();
        }

        if (this.start instanceof Wireable)
            ((Wireable) start).onWireOut(this, false);

        if (this.end instanceof Wireable)
            ((Wireable) end).onWireIn(this, false);

    }

    /**
     * called when wire has been established
     *
     * @param start
     * @param end
     * @param y
     * @param wall
     */
    abstract protected Wire wire(Wire y, GraphEdit wall);

    protected boolean tryWire() {
        GraphEdit wall = graph();
        Wire x = //new Wire(start, end);
                wire(new Wire(start, end), wall);
        Wire y = wall.link(x);
        if (y == x) {
            start.root().debug(start, 1, () -> "wire(" + wall + ",(" + start + ',' + end + "))");
            //wire(start, end, y, wall);
            return true;
        }
        return false;
    }

    private void updateEnd(Finger finger) {
        Surface nextEnd = finger.touching.get();
        if (nextEnd != end) {

            if (nextEnd == start) {
                end = null;
            } else {

                if (end instanceof Wireable) {
                    ((Wireable) end).onWireIn(this, false);
                }

                if (nextEnd instanceof Wireable) {

                    if (((Wireable) nextEnd).onWireIn(this, true)) {
                        this.end = nextEnd;
                    } else {
                        this.end = null;
                    }

                }
            }
        }
    }
}
