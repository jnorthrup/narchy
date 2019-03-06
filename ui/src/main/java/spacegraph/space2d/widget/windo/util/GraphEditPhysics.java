package spacegraph.space2d.widget.windo.util;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.space2d.widget.windo.Link;

public abstract class GraphEditPhysics {

    protected GraphEdit<?> graph = null;

    transient public Surface surface = new EmptySurface();

    abstract public void add(Surface w);

    abstract public void remove(Surface w);

    public final Surface start(GraphEdit parent) {
        this.graph = parent;
        return starting(graph);
    }
    abstract protected Surface starting(GraphEdit<?> graph);

    abstract public void stop();

    public abstract Link link(Wire w);

    public abstract void invokeLater(Runnable o);

}
