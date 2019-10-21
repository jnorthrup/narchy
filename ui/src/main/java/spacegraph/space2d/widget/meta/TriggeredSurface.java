package spacegraph.space2d.widget.meta;

import jcog.event.Off;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.UnitContainer;

import java.util.function.Consumer;
import java.util.function.Function;


/** abstract update triggered */
public class TriggeredSurface<X extends Surface> extends UnitContainer<X> {

    private final Function<Runnable, Off> trigger;
    private final Consumer<X> update;
    private transient Off on = null;

    public TriggeredSurface(X surface, Function<Runnable,Off> trigger, Runnable update) {
        this(surface, trigger, new Consumer<X>() {
            @Override
            public void accept(X x) {
                update.run();
            }
        });
    }

    public TriggeredSurface(X surface, Function<Runnable,Off> trigger, Consumer<X> update) {
        super(surface);
        this.trigger = trigger;
        this.update = update;
    }

    @Override
    protected void starting() {
        super.starting();
        this.on = trigger.apply(this::update);
    }

    @Override
    protected void stopping() {
        on.close();
        on = null;
        super.stopping();
    }

    protected final void update() {
        update.accept(the);
    }

}
