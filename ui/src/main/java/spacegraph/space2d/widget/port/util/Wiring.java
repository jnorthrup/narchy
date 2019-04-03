package spacegraph.space2d.widget.port.util;

import spacegraph.input.finger.Dragging;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.EditGraph2D;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.space2d.widget.shape.PathSurface;
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

            EditGraph2D g = graph();
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

    private final EditGraph2D graph() {
        return start.parentOrSelf(EditGraph2D.class);
    }

    @Override
    public final void stop(Finger finger) {

        if (pathVis != null) {
            pathVis.remove();
            pathVis = null;
        }

        if (start == end)
            return; //same instance

        if (end != null) {
            if (tryWire()) {
                if (this.start instanceof Wireable)
                    ((Wireable) start).onWireOut(this, false);

                if (this.end instanceof Wireable)
                    ((Wireable) end).onWireIn(this, false);

                return;
            }

        }



        return;

    }

    protected final boolean tryWire() {
        EditGraph2D g = graph();
        return tryWire((Port)start, (Port)end, g);
    }

    static boolean tryWire(Port start, Port end, EditGraph2D g) {
        if (Port.canConnect(start, end)) {

            if (start instanceof TypedPort && end instanceof TypedPort) {

                //TODO lazy construct and/or cache these

                //apply type checking and auto-conversion if necessary
                Class ta = ((TypedPort) start).type;
                Class tb = ((TypedPort) end).type;
                if (!ta.equals(tb) /* TODO && direct ancestor comparison */ ) {
                    Class aa = ta, bb = tb;
                    if (aa.equals(bb)) {
                        //ok
                    } else {

                        List<Function> ab = CAST.applicable(aa, bb), ba = CAST.applicable(bb, aa);

                        if (!ab.isEmpty() || !ba.isEmpty()) {
                            //wire with adapter
                            PortAdapter adapter = new PortAdapter(aa, ba, bb, ab);
                            g.addUndecorated(adapter).pos(start.bounds.mean(end.bounds).scale(0.5f));

                            TypedPort ax = adapter.port(true);
                            TypedPort ay = adapter.port(false);

                            g.addWire(new Wire(start, ax));
                            g.addWire(new Wire(ay, end));

                            return true;
                        }
                    }
                }
            }

            Wire wire = g.addWire(new Wire(start, end));
            return wire!=null;
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
