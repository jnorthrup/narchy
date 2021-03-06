package spacegraph.space3d.widget;

import spacegraph.space3d.AbstractSpace;
import spacegraph.space3d.SpaceGraph3D;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.transform.EdgeDirected3D;
import spacegraph.space3d.transform.Flatten;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;


/**
 * thread-safe visualization of a set of spatials, and
 * calls to their per-frame rendering animation
 */
public abstract class DynamicListSpace<X> extends AbstractSpace<X> {

    private SpaceGraph3D<X> space;
    List<Spatial<X>> active = Collections.EMPTY_LIST;


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
        for (Spatial<X> xSpatial : active) {
            each.accept(xSpatial);
        }
    }


    @Override
    public void update(SpaceGraph3D<X> s, long dtMS) {

        //synchronized (this) {
            List<? extends Spatial<X>> prev = this.active;

        for (Spatial<X> spatial : prev) {
            spatial.deactivate();
        }

        List next = get();


            this.active = next;


        for (Spatial<X> xSpatial : prev) {
            if (!xSpatial.preactive) {
                xSpatial.order = (short) -1;
            }
        }

        for (Spatial<X> x : active) {
            x.update(s.dyn);
        }

        super.update(s, dtMS);
        //}



    }

    protected abstract List<? extends Spatial<X>> get();


    /**
     * displays in a window with default force-directed options
     */
    @Deprecated
    public SpaceGraph3D show(int w, int h, boolean flat) {


        AbstractSpace ss = flat ? with(new Flatten(0.25f, 0.25f)) : this;
        SpaceGraph3D<X> s = new SpaceGraph3D<>(ss);

        EdgeDirected3D fd = new EdgeDirected3D();
        fd.condense.set(fd.condense.get() * 1.0F);
        s.dyn.addBroadConstraint(fd);


        s.camPos((float) 0, (float) 0, 90.0F);
        s.video.show(w, h);

        return s;

    }

    public void clear() {
        synchronized (this) {
            SpaceGraph3D<X> spatials = space;
            for (Spatial<X> xSpatial : active) {
                spatials.remove(xSpatial);
            }
            active.clear();
        }
    }

}



















































