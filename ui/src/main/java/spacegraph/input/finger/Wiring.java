package spacegraph.input.finger;

import spacegraph.space2d.Surface;
import spacegraph.space2d.hud.Ortho;
import spacegraph.space2d.widget.meta.SketchedPath;
import spacegraph.space2d.widget.windo.Dyn2DSurface;
import spacegraph.util.Path2D;

import javax.annotation.Nullable;

/** the process of drawing a wire between two surfaces */
public class Wiring extends FingerDragging {


    public interface Wireable {
        boolean onWireIn(@Nullable Wiring w, boolean active);
        void onWireOut(@Nullable Wiring w, boolean active);

    }

    Path2D path;

    protected final Surface start;
    private SketchedPath pathVis;
    protected Surface end = null;

    public Dyn2DSurface.PhyWindow source() {
        return start.parent(Dyn2DSurface.PhyWindow.class);
    }
    public Dyn2DSurface.PhyWindow target() {
        return end!=null ? end.parent(Dyn2DSurface.PhyWindow.class) : null;
    }

    public Wiring(Surface start) {
        super(2);
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

        if (end != null) {
            start.root().debug(start, 1, () -> "WIRE: " + start + " -> " + end);

            onWired();
        }

        if (this.start instanceof Wireable)
            ((Wireable)start).onWireOut(this, false);

        if (this.end instanceof Wireable)
            ((Wireable)end).onWireIn(this, false);

    }

    protected void onWired() {

    }

    private void updateEnd(Finger finger) {
        Surface nextEnd = finger.touching.get();
        if (nextEnd!=end) {

            if (nextEnd == start) {
                end = null; //dont allow self-loop
                return;
            }

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
