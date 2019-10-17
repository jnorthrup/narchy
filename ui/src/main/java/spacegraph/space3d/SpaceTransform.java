package spacegraph.space3d;

/**
 * Created by me on 6/21/16.
 */
@FunctionalInterface
public interface SpaceTransform<X> {

    void update(Iterable<Spatial<X>> g, float dt);

}
