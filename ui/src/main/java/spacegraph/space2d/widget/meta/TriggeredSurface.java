package spacegraph.space2d.widget.meta;

import jcog.event.Off;
import spacegraph.space2d.Surface;

import java.util.function.Consumer;
import java.util.function.Function;


/** abstract update triggered */
public class TriggeredSurface<X extends Surface> extends AbstractCachedSurface<X> {

    private final Function<Runnable, Off> trigger;
    private final Consumer<X> update;
    private final transient Off on = null;

    public TriggeredSurface(X surface, Function<Runnable,Off> trigger, Runnable update) {
        this(surface, trigger, (x)->update.run());
    }

    public TriggeredSurface(X surface, Function<Runnable,Off> trigger, Consumer<X> update) {
        super(surface);
        this.trigger = trigger;
        this.update = update;
    }

    @Override
    public Off whenOff() {
        return trigger.apply(this::update);
    }

    protected final void update() {
        update.accept(the);
    }

}
