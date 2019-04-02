package spacegraph.space2d.container;

import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;

import java.util.function.Consumer;
import java.util.function.Predicate;

abstract public class EmptyContainer extends Container {

    @Override
    public int childrenCount() {
        return 0;
    }

    @Override
    public final void forEach(Consumer<Surface> o) {

    }

    @Override
    public Surface finger(Finger finger) {
        return null;
    }

    @Override
    public final boolean whileEach(Predicate<Surface> o) {
        return true;
    }

    @Override
    public final boolean whileEachReverse(Predicate<Surface> o) {
        return true;
    }
}
