package spacegraph.space3d.widget;

import jcog.data.list.FasterList;
import spacegraph.space3d.AbstractSpatial;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.Dynamics3D;
import spacegraph.space3d.phys.constraint.TypedConstraint;

import java.util.List;
import java.util.function.Consumer;

/**
 * TODO make inherit from an AbstractSpatial not SimpleSpatial, and
 * SimpleSpatial also subclass from that
 */
public abstract class CompoundSpatial<X> extends AbstractSpatial<X> {

    private final List<Collidable> bodies = new FasterList();
    private final List<Spatial> spatials = new FasterList();
    private final List<TypedConstraint> constraints = new FasterList();

    protected CompoundSpatial(X x) {
        super(x);
    }

    @Override
    public final void update(Dynamics3D world) {

        if (bodies.isEmpty() && spatials.isEmpty()) { 
            create(world);
        } else {
            next(world);
        }

        for (Spatial s : spatials)
            s.update(world);

    }

    private void next(Dynamics3D world) {

    }

    public TypedConstraint add(TypedConstraint c) {
        constraints.add(c);
        return c;
    }

    public Spatial add(Spatial s) {
        spatials.add(s);
        return s;
    }

    public void remove(Spatial s) {
        spatials.remove(s);
    }


    void add(Collidable c) {
        bodies.add(c);
    }

    public void remove(Collidable c) {
        bodies.remove(c);
    }

    protected void create(Dynamics3D world) {

    }

    @Override
    public void forEachBody(Consumer<Collidable> c) {
        bodies.forEach(c);

        if (!spatials.isEmpty())
            spatials.forEach(s -> s.forEachBody(c));
    }

    @Override
    public List<TypedConstraint> constraints() {
        return constraints;
    }
}
