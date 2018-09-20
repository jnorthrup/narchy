package spacegraph.space2d.widget.windo;

import spacegraph.input.finger.Wiring;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.button.PushButton;

/** attempts to establish a default link if the wiring was successful */
public class LinkingWiring extends Wiring {

    public LinkingWiring(Surface start) {
        super(start);
    }

    @Override
    protected Wire wire(Wire w, GraphEdit wall) {

        return wall.cable(w, new PushButton("x").click((r)->r.parent(Windo.class).remove()));

//        PathSurface p = new PathSurface(2);
//        p.set(0, start.cx(), start.cy());
//        p.set(1, end.cx(), end.cy());
//        wall.addRaw(p);
    }
}
