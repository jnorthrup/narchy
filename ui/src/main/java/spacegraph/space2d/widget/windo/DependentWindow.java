package spacegraph.space2d.widget.windo;

import jcog.event.Off;
import jcog.event.RunThese;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.ContainerSurface;
import spacegraph.space2d.container.graph.EditGraph2D;

public class DependentWindow extends Windo {


    private final Surface content;
    private Off on;


    public DependentWindow(Surface content) {
        super(content);
        this.content = content;
    }

    @Override
    protected void starting() {
        super.starting();

        EditGraph2D g = parentOrSelf(EditGraph2D.class);

        this.on = new RunThese(()->{
            g.physics.remove(this);

            //remove any associated links, recursively
            if (content instanceof ContainerSurface) {
                ((ContainerSurface) content).forEachRecursively(g::removeComponent);
            } else {
                g.removeComponent(content);
            }
        });

    }

    @Override
    protected void stopping() {
        on.close();
        on = null;
        super.stopping();
    }
}
