package spacegraph.space2d.widget.windo.util;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.graph.EditGraph2D;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.space2d.container.graph.Link;

public abstract class GraphEditPhysics {

    protected EditGraph2D<?> graph = null;

    transient public Surface surface = new EmptySurface();

    abstract public void add(Surface w);

    abstract public void remove(Surface w);

    public final Surface start(EditGraph2D parent) {
        this.graph = parent;
        return starting(graph);
    }
    abstract protected Surface starting(EditGraph2D<?> graph);

    abstract public void stop();

    public abstract Link link(Wire w);

    public abstract void invokeLater(Runnable o);

    public void update(EditGraph2D g, float dt) {

    }

}
