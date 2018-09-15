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
    protected void wired(Surface start, Surface end, Wire y, GraphWall wall) {

        wall.chain(start, end, new PushButton("x").click((r)->r.parent(Windo.class).detach()));

//        PathSurface p = new PathSurface(2);
//        p.set(0, start.cx(), start.cy());
//        p.set(1, end.cx(), end.cy());
//        wall.addRaw(p);
    }
}
