package spacegraph.space2d.container;

import org.jetbrains.annotations.Nullable;

public interface GridModel<X> {
    int cellsX();

    int cellsY();

    /**
     * return null to remove the content of a displayed cell
     */
    @Nullable X get(int x, int y);

    default void start(ScrollGrid<X> x) {
    }

    default void stop(ScrollGrid<X> x) {
    }
}
