package spacegraph.space2d.widget.windo;

import jcog.event.Off;
import jcog.event.Offs;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Container;

class DependentWindow extends Windo {


    private final Surface content;
    private Off on;


    public DependentWindow(Surface content) {
        super(content);
        this.content = content;
    }

    @Override
    protected void starting() {
        super.starting();

        GraphEdit graphEdit = parent(GraphEdit.class);

        this.on = new Offs(()->{
            graphEdit.physics.remove(this);

            //remove any associated links, recursively
            if (content instanceof Container) {
                ((Container) content).forEachRecursively(graphEdit::removeComponent);
            } else {
                graphEdit.removeComponent(content);
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
