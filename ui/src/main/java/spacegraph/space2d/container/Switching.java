package spacegraph.space2d.container;

import jcog.exe.Loop;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * TODO extend UnitContainer
 */
public class Switching extends Container {

    protected volatile int switched = -1;
    private Surface current;
    private Supplier<Surface>[] states;

    public Switching(Supplier<Surface>... states) {
        super();

        current = new EmptySurface();

        if (states.length > 0) {
            states(states);
        }
    }

    /**
     * sets the available states
     */
    public Switching states(Supplier<Surface>... states) {

        switched = -1;
        this.states = states;
        state(0);

        return this;
    }


    /**
     * selects the active state
     */
    public Switching state(int next) {
        Loop.invokeLater(() -> {
            if (switched == next)
                return;

            Surface prevSurface = this.current;

            Surface nextSurface = (current = (states[switched = next].get()));

            if (prevSurface != null)
                prevSurface.stop();

            if (parent != null) {
                nextSurface.start(this);
            }

            layout();
        });
        return this;
    }

    @Override
    public boolean start(@Nullable SurfaceBase parent) {
        if (super.start(parent)) {
            assert (current.parent == null);
            current.start(this);
            layout();
            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            current.stop();
            return true;
        }
        return false;
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
        if (current.parent != null) //if ready
            o.accept(current);
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        if (current.parent != null)
            return o.test(current);
        else
            return true;
    }

    @Override
    public final boolean whileEachReverse(Predicate<Surface> o) {
        return whileEach(o);
    }
}
