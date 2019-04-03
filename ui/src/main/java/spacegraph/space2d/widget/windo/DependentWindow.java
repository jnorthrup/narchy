package spacegraph.space2d.widget.windo;

import jcog.event.Off;
import jcog.event.Offs;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Container;
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

        this.on = new Offs(()->{
            g.physics.remove(this);

            //remove any associated links, recursively
            if (content instanceof Container) {
                ((Container) content).forEachRecursively(g::removeComponent);
            } else {
                g.removeComponent(content);
            }
        });

    }

    @Override
    protected void stopping() {
        on.off();
        on = null;
        super.stopping();
    }
}
