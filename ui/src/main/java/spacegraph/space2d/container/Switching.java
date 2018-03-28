package spacegraph.space2d.container;

import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;


/** TODO extend UnitContainer */
public class Switching extends Container {

    private Surface current;
    private Supplier<Surface>[] states;
    protected volatile int switched = -1;

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
    public Switching state(int next) {
        synchronized(this) {
            if (switched == next)
                return this;

            Surface prevSurface = this.current;

            Surface nextSurface = (current = (states[switched = next].get()));

            if (prevSurface != null)
                prevSurface.stop();

            if (parent!=null) {
                nextSurface.start(this);
            }
        }

        layout();
        return this;
    }

    @Override
    public void start(@Nullable SurfaceBase parent) {
        synchronized (this) {
            super.start(parent);
            assert(current.parent==null);
            current.start(this);
        }
        layout();
    }

    @Override
    public void stop() {
        synchronized (this) {
            current.stop();
            //current = new EmptySurface();
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
        if (current.parent!=null) //if ready
            o.accept(current);
    }
    @Override
    public boolean whileEach(Predicate<Surface> o) {
        if (current.parent!=null)
            return o.test(current);
        else
            return true;
    }

    @Override
    public final boolean whileEachReverse(Predicate<Surface> o) {
        return whileEach(o);
    }
}
