package spacegraph.space2d.widget.windo;

import jcog.event.Off;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.port.Wire;

abstract public class Link {

    public final Wire id;

    public Link(Wire wire) {
        super();
        this.id = wire;
    }

    public Link on(Off r) {
        id.offs.add(r);
        return this;
    }

    public Link on(Surface hostage) {
        return on(hostage::remove);
    }

    public final void remove(GraphEdit g) {
        g.removeWire(id); //id.a, id.b);
    }



}
