package spacegraph.space2d.widget.windo;

import jcog.event.Offs;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.port.util.Wire;
import spacegraph.space2d.widget.shape.VerletSurface;
import toxi.physics2d.VerletParticle2D;

public class Link {

    public final Wire id;

    public final Offs offs = new Offs();

    Link(Wire wire) {
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

    public void bind(Surface gripWindow, VerletParticle2D mid, boolean surfaceOverrides, VerletSurface.VerletSurfaceBinding where, GraphEdit g) {


        g.physics.bind(gripWindow, mid, surfaceOverrides, where);
//        }
//        else {
//            gripWindow = null;
//        }

        hold(gripWindow);
    }
}
