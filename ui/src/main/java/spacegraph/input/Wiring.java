package spacegraph.input;

import spacegraph.Ortho;
import spacegraph.Path2D;
import spacegraph.Surface;
import spacegraph.widget.meta.SketchedPath;
import spacegraph.widget.windo.PhyWall;

import javax.annotation.Nullable;

/** the process of drawing a wire between two surfaces */
public class Wiring extends FingerDragging {


    public interface Wireable {
        boolean onWireIn(@Nullable Wiring w, boolean active);
        void onWireOut(@Nullable Wiring w, boolean active);

    }

    Path2D path;

    private final Surface start;
    private SketchedPath pathVis;
    private Surface end = null;

    public PhyWall.PhyWindow source() {
        return start.parent(PhyWall.PhyWindow.class);
    }
    public PhyWall.PhyWindow target() {
        return end!=null ? end.parent(PhyWall.PhyWindow.class) : null;
    }

    public Wiring(Surface start) {
        super(0);
        this.start = start;
    }

    @Override
    public boolean start(Finger f) {
        if (super.start(f)) {

            if (this.start instanceof Wireable)
                ((Wireable)start).onWireOut(this, true);

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
            ((Ortho)(start.root())).addOverlay(pathVis = new SketchedPath(path));
        }

        path.add(f.pos, 64);

        updateEnd(f);

        return true;
    }

    @Override
    public void stop(Finger finger) {
        if (pathVis!=null) {
            ((Ortho)(start.root())).removeOverlay(pathVis);
            pathVis = null;
        }

        start.root().debug(start, 1, ()->"WIRE: " + start + " -> " + end);

        if (this.start instanceof Wireable)
            ((Wireable)start).onWireOut(this, false);

        if (this.end instanceof Wireable)
            ((Wireable)end).onWireIn(this, false);

    }

    private void updateEnd(Finger finger) {
        Surface nextEnd = finger.touching;
        if (nextEnd!=end) {
            if (end instanceof Wireable) {
                ((Wireable)end).onWireIn(this, false);
            }

            if (nextEnd instanceof Wireable) {

                //start filtering end
                //if (!(start instanceof Wireable) || ((Wireable)start).acceptWireTo(end)) {

                    // end filtering start
                    if (((Wireable) nextEnd).onWireIn(this, true)) {
                        this.end = nextEnd;
                        return;
                    }
                //}
                this.end = null;

            }
        }
    }
}
