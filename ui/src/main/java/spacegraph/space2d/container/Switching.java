package spacegraph.space2d.container;

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

    private int switched = -1;
    private Surface current;
    private Supplier<Surface>[] states;

    public Switching(Supplier<Surface>... states) {
        super();

        current = new EmptySurface();

        if (states.length > 0) {
            states(states);
        }
        set(0);
    }

    /**
     * sets the available states
     */
    private Switching states(Supplier<Surface>... states) {

        switched = -1;
        this.states = states;

        return this;
    }


    /**
     * selects the active state
     */
    public Switching set(int next) {

        if (switched == next)
            return this;

        Surface prevSurface = this.current;

        Surface nextSurface = (current = (states[switched = next].get()));
        if (prevSurface != nextSurface) {

            if (prevSurface != null)
                prevSurface.stop();


            if (parent != null) {
                nextSurface.start(this);
            }

            layout();
        }


        return this;
    }

    @Override
    public boolean start(@Nullable SurfaceBase parent) {
        if (super.start(parent)) {

                if (!current.start(this))
                    throw new RuntimeException();


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
        o.accept(current);
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        return o.test(current);
    }

    @Override
    public final boolean whileEachReverse(Predicate<Surface> o) {
        return whileEach(o);
    }
}
