package spacegraph.space3d.widget;

import spacegraph.space3d.AbstractSpace;
import spacegraph.space3d.SpaceGraph3D;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.transform.EdgeDirected3D;
import spacegraph.space3d.transform.Flatten;
import spacegraph.util.Active;

import java.util.List;
import java.util.function.Consumer;


/**
 * thread-safe visualization of a set of spatials, and
 * calls to their per-frame rendering animation
 */
public abstract class DynamicListSpace<X> extends AbstractSpace<X> {

    private SpaceGraph3D<X> space;
    List<Spatial<X>> active = List.of();


    @Override
    public void start(SpaceGraph3D<X> space) {
        this.space = space;
    }


    @Override
    public void stop() {
        synchronized (this) {
            clear();

            super.stop();
        }
    }

    @Override
    public void forEach(Consumer<? super Spatial<X>> each) {
        active.forEach(each::accept);
    }

    @Override
    public void update(SpaceGraph3D<X> s, long dtMS) {

        synchronized (this) {
            List<? extends Spatial<X>> prev = this.active;

            prev.forEach(Active::deactivate);

            List next = get();


            this.active = next;


            prev.forEach(x -> {
                if (!x.preactive)
                    x.order = -1;
            });

            active.forEach(x -> x.update(s.dyn));

            super.update(s, dtMS);
        }



    }

    abstract protected List<? extends Spatial<X>> get();


    /**
     * displays in a window with default force-directed options
     */
    @Deprecated
    public SpaceGraph3D show(int w, int h, boolean flat) {


        AbstractSpace ss = flat ? with(new Flatten(0.25f, 0.25f)) : this;
        SpaceGraph3D<X> s = new SpaceGraph3D<>(ss);

        EdgeDirected3D fd = new EdgeDirected3D();
        s.dyn.addBroadConstraint(fd);
        fd.condense.set(fd.condense.get() * 8);


        s.camPos(0, 0, 90).display.show(w, h);

        return s;

    }

    public void clear() {
        synchronized (this) {
            active.forEach(space::remove);
            active.clear();
        }
    }

}



















































