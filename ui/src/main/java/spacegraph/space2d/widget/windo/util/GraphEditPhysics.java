package spacegraph.space2d.widget.windo.util;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.graph.EditGraph2D;
import spacegraph.space2d.container.graph.Link;
import spacegraph.space2d.widget.port.Wire;

/** model for physics-based management of EditGraph2D spaces */
public abstract class GraphEditPhysics {

    protected EditGraph2D graph = null;

    transient public Surface below = new EmptySurface();
    transient public Surface above = new EmptySurface();


    abstract public Object add(Surface w);

    abstract public void remove(Surface w);

    public final void start(EditGraph2D parent) {
        starting(this.graph = parent);
    }

    /** may construct surfaceBelow and surfaceAbove in implementations */
    abstract protected void starting(EditGraph2D graph);

    abstract public void stop();

    public abstract Link link(Wire w);

    /** queues procedures synchronously in the physics model's private sequential queue */
    public abstract void invokeLater(Runnable o);

    public void update(EditGraph2D g, float dt) {

    }

}
