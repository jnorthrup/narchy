package spacegraph.space2d.container;

import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.data.atomic.MetalAtomicIntegerFieldUpdater;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.Surfacelike;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * branch node.  layout is an asynchronous lazy update.
 * TODO illustrate the various update loops and their interactions
 */
abstract public class ContainerSurface extends Surface {

    final static MetalAtomicIntegerFieldUpdater<ContainerSurface> MUSTLAYOUT =
            new MetalAtomicIntegerFieldUpdater<>(ContainerSurface.class, "mustLayout");

    private final int mustLayout = 0;

    @Override
    public final boolean start(Surfacelike parent) {
        if (super.start(parent)) {
            layout();
            return true;
        }
        return false;
    }

    public final void layout() {


        MUSTLAYOUT.set(this, 1);
        //MUSTLAYOUT.lazySet(this, 1);
    }


    /** TODO just accept the ReRender instance, not this dtS which it can get as needed */
    @Deprecated abstract protected void doLayout(float dtS);

    @Override
    public void print(PrintStream out, int indent) {
        super.print(out, indent);

        forEach(c -> {
            out.print(Texts.repeat("  ", indent + 1));
            c.print(out, indent + 1);
        });
    }

    public final <S extends Surface> S pos(v2 p) {
        pos(p.x, p.y);
        return (S) this;
    }

    @Override
    public final <S extends Surface> S pos(RectFloat r) {
        if (posChanged(r))
            layout();
        return (S) this;
    }

    /**
     * first sub-layer
     */
    @Deprecated
    protected void paintIt(GL2 gl, ReSurface r) {

    }

    @Override
    protected final void render(ReSurface r) {
        if (preRender(r)) {
            render(r, MUSTLAYOUT.compareAndSet(this, 1, 0));
            show();
        } else {
            hide();
        }
    }

    /* TODO abstract */ protected final void render(ReSurface r, boolean layout) {
        //TODO all of these called methods can be merged into one method that does these in whatever order the impl chooses
        //

        if (layout) {
            forEach(c -> c.start(this));
            doLayout(r.dtS());
        }

        renderContainer(r);

        renderContent(r);
    }

    @Deprecated private void renderContainer(ReSurface r) {
        r.on(this::paintIt); //TODO if transparent ("non-opaque" in Swing terminology) this doesnt need rendered
    }

    @Deprecated protected void renderContent(ReSurface r) {
        //TODO forEachWith
        forEach(c -> c.tryRender(r));
    }

    /** post-visibility render guard */
    protected boolean preRender(ReSurface r) {
        return true;
    }

    public Surface finger(Finger finger) {

        if (showing() && childrenCount() > 0 && (!clipBounds || finger.intersects(bounds))) {
            Surface[] found = new Surface[1];
            whileEachReverse(c -> {

                Surface s = c.finger(finger);
                if (s != null) {
                    found[0] = s;
                    return false;
                }

                return true;

            });
            return found[0];
        }

        return null;
    }

    protected abstract int childrenCount();


    @Override
    protected void starting() {
        forEach(s -> s.start(this));
        layout();
    }

    @Override
    protected void stopping() {
        forEach(Surface::stop);
    }

    abstract public void forEach(Consumer<Surface> o);

    public void forEachRecursively(Consumer<Surface> o) {

        o.accept(this);

        forEach(z -> {
            if (z instanceof ContainerSurface)
                ((ContainerSurface) z).forEachRecursively(o);
            else
                o.accept(z);
        });

    }

    public abstract boolean whileEach(Predicate<Surface> o);

    public abstract boolean whileEachReverse(Predicate<Surface> o);

    /** default implementation */
    public <X extends Surface> X first(Class<? extends X> zoomedClass) {
        final Surface[] found = new Surface[]{null};
        whileEach(s -> {
            if (zoomedClass.isInstance(s)) {
                found[0] = s;
                return false;
            }
            return true; //keep going
        });
        return (X) found[0];
    }

    public boolean layoutPending() {
        return this.mustLayout>0;
    }


//    public final void forEachReverse(Consumer<Surface> each) {
//        whileEachReverse((s) -> {
//            each.accept(s);
//            return true;
//        });
//    }

}
