package spacegraph.space2d.dyn2d;

import spacegraph.space2d.phys.dynamics.Dynamics2D;

/**
 * Interface pre testovacie scenare
 *
 * @author Marek Benovic
 */
public interface ICase {
    /**
     * Inicializator. Do sveta predanom v parametri inicializuje objekty.
     *
     * @param w Svet
     */
    void init(Dynamics2D w);
}
