package spacegraph.space2d.widget.port.util;

import spacegraph.input.finger.Dragging;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.space2d.widget.shape.PathSurface;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.util.Path2D;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

import static spacegraph.space2d.widget.port.TypedPort.CAST;

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

        if (start == end)
            return; //same instance

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

    protected final boolean tryWire() {
        GraphEdit g = graph();
        return tryWire((Port)start, (Port)end, g);
    }

    static boolean tryWire(Port start, Port end, GraphEdit g) {
        if (Port.connectable(start, end)) {

            if (start instanceof TypedPort && end instanceof TypedPort) {

                //TODO lazy construct and/or cache these

                //apply type checking and auto-conversion if necessary
                Class aa = ((TypedPort) start).type, bb = ((TypedPort) end).type;
                if (aa.equals(bb)) {
                    //ok
                } else {

                    List<Function> ab = CAST.convertors(aa, bb), ba = CAST.convertors(bb, aa);

                    if (!ab.isEmpty() || !ba.isEmpty()) {
                        //wire with adapter
                        PortAdapter adapter = new PortAdapter(aa, ab, bb, ba);
                        g.addWeak(adapter).pos(start.bounds.mean(end.bounds).scale(0.1f));

                        TypedPort ax = adapter.port(true);
                        if (ax !=null) {
                            g.addWire(new Wire(start, ax));
                            g.addWire(new Wire(end, ax));
                        }
                        TypedPort ay = adapter.port(false);
                        if (ay !=null) {

                            g.addWire(new Wire(ay, start));
                            g.addWire(new Wire(ay, end));
                        }
                        return true;
                    }
                }
            }

            Wire wire = g.addWire(new Wire(start, end));

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
