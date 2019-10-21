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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * branch node.  layout is an asynchronous lazy update.
 * TODO illustrate the various update loops and their interactions
 */
public abstract class ContainerSurface extends Surface {

    static final MetalAtomicIntegerFieldUpdater<ContainerSurface> MUSTLAYOUT =
            new MetalAtomicIntegerFieldUpdater<>(ContainerSurface.class, "mustLayout");

    private volatile int mustLayout = 0;

    @Override
    public final boolean start(Surfacelike parent) {
        if (super.start(parent)) {
            layout();
            return true;
        }
        return false;
    }


    /** TODO just accept the ReRender instance, not this dtS which it can get as needed */
    @Deprecated
    protected abstract void doLayout(float dtS);

    @Override
    public void print(PrintStream out, int indent) {
        super.print(out, indent);

        forEach(new Consumer<Surface>() {
            @Override
            public void accept(Surface c) {
                out.print(Texts.repeat("  ", indent + 1));
                c.print(out, indent + 1);
            }
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
        if (canRender(r)) {
            render(r, MUSTLAYOUT.compareAndSet(this, 1, 0));
            showing(true);
            //show();
        } else {
            //hide();
            showing(false);
        }
    }

    /* TODO abstract */ protected final void render(ReSurface r, boolean layout) {
        //TODO all of these called methods can be merged into one method that does these in whatever order the impl chooses
        //

        if (layout) {
            forEachOrphan(new Consumer<Surface>() {
                @Override
                public void accept(Surface c) {
                    c.start(ContainerSurface.this);
                }
            });
            doLayout(r.dtS());
        }

        //TODO if transparent ("non-opaque" in Swing terminology) this doesnt need rendered
        paintIt(r.gl, r);

        renderContent(r);
    }

    @Override
    public final void showing(boolean s) {
        boolean wasShown = this.showing;
        if (wasShown!=s) {
            showing = s;
            if (!s)
                forEach(new Consumer<Surface>() {
                    @Override
                    public void accept(Surface c) {
                        c.showing(false);
                    }
                });
        }
    }

    @Deprecated protected void renderContent(ReSurface r) {
        forEachWith(Surface::renderIfVisible, r);
    }

    /** post-visibility render guard */
    protected boolean canRender(ReSurface r) {
        return true;
    }

    public Surface finger(Finger finger) {

        if (showing() && childrenCount() > 0 && (!clipBounds || finger.intersects(bounds))) {
            Surface[] found = new Surface[1];
            whileEachReverse(new Predicate<Surface>() {
                @Override
                public boolean test(Surface c) {

                    Surface s = c.finger(finger);
                    if (s != null) {
                        found[0] = s;
                        return false;
                    }

                    return true;

                }
            });
            return found[0];
        }

        return null;
    }

    public abstract int childrenCount();


    @Override
    protected void starting() {
        forEachOrphan(new Consumer<Surface>() {
            @Override
            public void accept(Surface s) {
                s.start(ContainerSurface.this);
            }
        });
        layout();
    }


    @Override
    protected void stopping() {
        forEach(Surface::stop);
    }

    /** TODO forEachWith */
    public abstract void forEach(Consumer<Surface> o);

    public <X> void forEachWith(BiConsumer<Surface,X> o, X x) {
        forEach(new Consumer<Surface>() {
            @Override
            public void accept(Surface c) {
                o.accept(c, x);
            }
        });
    }

    public final void forEachOrphan(Consumer<Surface> S) {
        forEachWith(new BiConsumer<Surface, Consumer<Surface>>() {
            @Override
            public void accept(Surface c, Consumer<Surface> s) {
                if (c.parent == null)
                    s.accept(c);
            }
        }, S);
    }

    public void forEachRecursively(Consumer<Surface> O) {

        O.accept(this);

        forEachWith(new BiConsumer<Surface, Consumer<Surface>>() {
            @Override
            public void accept(Surface z, Consumer<Surface> o) {
                if (z instanceof ContainerSurface)
                    ((ContainerSurface) z).forEachRecursively(o);
                else
                    o.accept(z);
            }
        }, O);

    }

    public abstract boolean whileEach(Predicate<Surface> o);

    /** TODO make whileNullReverse(UnaryOperator<Surface> o) */
    public abstract boolean whileEachReverse(Predicate<Surface> o);

    /** default implementation */
    public <X extends Surface> X first(Class<? extends X> zoomedClass) {
        Surface[] found = {null};
        whileEach(new Predicate<Surface>() {
            @Override
            public boolean test(Surface s) {
                if (zoomedClass.isInstance(s)) {
                    found[0] = s;
                    return false;
                }
                return true; //keep going
            }
        });
        return (X) found[0];
    }

    public final void layout() {
        //MUSTLAYOUT.set(this, 1);
        MUSTLAYOUT.lazySet(this, 1);
    }

    public final boolean layoutPending() {
        return MUSTLAYOUT.getOpaque(this)>0;
    }


}
