package spacegraph.space2d.widget.port.util;

import jcog.Util;
import jcog.math.v2;
import jcog.math.v3;
import jcog.reflect.ExtendedCastGraph;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import spacegraph.input.finger.Dragging;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.space2d.widget.shape.PathSurface;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.util.Path2D;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * the process of drawing a wire between two surfaces
 */
public class Wiring extends Dragging {


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

    public Wiring(int button, Surface start) {
        super(button);
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
    protected boolean drag(Finger f) {
        if (path == null) {

            GraphEdit g = graph();
            if (g!=null)
                g.addRaw(pathVis = new PathSurface(path = new Path2D(64)));
            else
                return false; //detached component no longer or never was in graph

        } else {
            updateEnd(f);
        }

        pathVis.add(f.posGlobal(pathVis), 64);

        return true;
    }

    private final GraphEdit graph() {
        return start.parent(GraphEdit.class);
    }

    @Override
    public final void stop(Finger finger) {


        if (end != null) {
            if (!tryWire())
                return; //fail
        }

        if (this.start instanceof Wireable)
            ((Wireable) start).onWireOut(this, false);

        if (this.end instanceof Wireable)
            ((Wireable) end).onWireIn(this, false);

        if (pathVis != null) {
            pathVis.remove();
            pathVis = null;
        }

    }

    protected boolean tryWire() {

        if (Port.connectable((Port)start, (Port)end)) {

            GraphEdit g = graph();

            Wire wire = g.addWire(new Wire(start, end));
            wire.connected();

            start.root().debug(start, 1, wire);

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

                if (end instanceof Wireable)
                    ((Wireable) end).onWireIn(this, false);


                if (nextEnd instanceof Wireable)
                    this.end = ((Wireable) nextEnd).onWireIn(this, true) ? nextEnd : null;

            }
        }
    }

}
