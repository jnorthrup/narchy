package spacegraph.space2d.widget.windo;

import jcog.event.Offs;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.port.util.Wire;

abstract public class Link {

    public final Wire id;

    public final Offs offs = new Offs();

    public Link(Wire wire) {
        super();
        this.id = wire;
    }

    public Link hold(Surface hostage) {
        this.offs.add(hostage::remove);
        return this;
    }

    public void remove(GraphEdit g) {


        g.removeWire(id.a, id.b);

        offs.off();


    }



}
