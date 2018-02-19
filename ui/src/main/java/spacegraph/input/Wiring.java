package spacegraph.input;

import spacegraph.Ortho;
import spacegraph.Path2D;
import spacegraph.Surface;
import spacegraph.widget.meta.SketchedPath;

import javax.annotation.Nullable;

/** the process of drawing a wire between two surfaces */
public class Wiring extends FingerDragging {

    public interface Wireable {
        void onWireIn(@Nullable Wiring w, boolean active);
        void onWireOut(@Nullable Wiring w, boolean active);
    }

    Path2D path;

    final Surface start;
    private SketchedPath pathVis;
    private Surface end = null;

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
            path = new Path2D(16);
            ((Ortho)(start.root())).window.add(pathVis = new SketchedPath(path));
        }

        path.add(f.pos, 32);

        updateEnd(f);

        return true;
    }

    @Override
    public void stop(Finger finger) {
        if (pathVis!=null) {
            ((Ortho)(start.root())).window.remove(pathVis);
            pathVis = null;
        }

        //updateEnd(finger);

        if (this.start instanceof Wireable)
            ((Wireable)start).onWireOut(this, false);

        if (this.end instanceof Wireable)
            ((Wireable)end).onWireIn(this, false);

        start.root().debug(start, 1, ()->"WIRE: " + start + " -> " + end);
    }

    private void updateEnd(Finger finger) {
        Surface nextEnd = ((Ortho) start.root()).onTouch(finger, null);
        if (nextEnd!=end) {
            if (end instanceof Wireable) {
                ((Wireable)end).onWireIn(this, false);
            }
            this.end = nextEnd;
            if (end instanceof Wireable) {
                ((Wireable)end).onWireIn(this, true);
            }
        }
    }
}
