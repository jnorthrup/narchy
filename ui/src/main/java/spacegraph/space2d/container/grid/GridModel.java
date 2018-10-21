package spacegraph.space2d.container.grid;

import org.jetbrains.annotations.Nullable;

public interface GridModel<X> {
    int cellsX();

    int cellsY();

    /**
     * return null to remove the content of a displayed cell
     */
    @Nullable X get(int x, int y);

    default void start(DynGrid<X> x) {
    }

    default void stop(DynGrid<X> x) {
    }
}
