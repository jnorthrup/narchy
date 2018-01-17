package spacegraph.layout;

import org.jetbrains.annotations.Nullable;
import spacegraph.EmptySurface;
import spacegraph.Surface;

import java.util.function.Consumer;
import java.util.function.Supplier;


public class Switching extends Layout {

    private Surface current;
    private Supplier<Surface>[] states;
    protected int switched = -1;

    public Switching(Supplier<Surface>... states) {
        super();

        current = new EmptySurface();

        if (states.length > 0) {
            states(states);
        }
    }

    /** sets the available states */
    public Switching states(Supplier<Surface>... states) {
        synchronized(this) {
            switched = -1;
            this.states = states;
            state(0);
            return this;
        }
    }



    /** selects the active state */
    public void state(int next) {
        synchronized(this) {
            if (switched ==next)
                return;

            if (current!=null) {
                current.stop();
            }

            current = (states[switched = next].get());
            layout();
        }
    }

    @Override
    public void start(@Nullable Surface parent) {
        synchronized (this) {
            super.start(parent);
            current.start(this);
            layout();
        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            current.stop();
            current = new EmptySurface();
            super.stop();
        }
    }

    @Override
    public void doLayout(int dtMS) {
        current.pos(bounds);
    }

    @Override
    public int childrenCount() {
        return 1;
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        o.accept(current);
    }

}
