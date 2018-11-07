package spacegraph.space2d.container;

import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.data.atomic.MetalAtomicIntegerFieldUpdater;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.SurfaceRender;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 7/20/16.
 */
abstract public class Container extends Surface {

    final static MetalAtomicIntegerFieldUpdater<Container> MUSTLAYOUT =
        new MetalAtomicIntegerFieldUpdater<>(Container.class, "mustLayout");

    private volatile int mustLayout = 0;


    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            layout();
            return true;
        }
        return false;
    }

    @Override
    public final void layout() {
        MUSTLAYOUT.lazySet(this, 1);
    }

    abstract protected void doLayout(int dtMS);

    @Override
    public void print(PrintStream out, int indent) {
        super.print(out, indent);

        forEach(c -> {
            out.print(Texts.repeat("  ", indent + 1));
            c.print(out, indent + 1);
        });
    }


    @Override
    public final <S extends Surface> S pos(RectFloat r) {
        if (posChanged(r))
            layout();
        return (S) this;
    }

    protected void paintAbove(GL2 gl, SurfaceRender r) {

    }

    protected void paintBelow(GL2 gl, SurfaceRender r) {

    }

    /**
     * paints the component above the background drawn ahead of this
     */
    protected void paintIt(GL2 gl) {

    }


    @Override
    protected final void paint(GL2 gl, SurfaceRender r) {

        if (!prePaint(r)) {
            showing = false;
            return;
        } else {
            showing = true;
        }


        doPaint(gl, r);
    }


    protected void doPaint(GL2 gl, SurfaceRender r) {
        if (MUSTLAYOUT.compareAndSet(this, 1, 0)) {
            doLayout(r.dtMS);
        }

        paintBelow(gl, r);

        paintIt(gl);

        renderContents(gl, r);

        paintAbove(gl, r);
    }

    public void renderContents(GL2 gl, SurfaceRender r) {
        forEach(c -> {
            if (c.parent==Container.this) {
                c.render(gl, r);
            }
            //else throw new NullPointerException(c + " is unparented child of " + Container.this);
        });
    }

    protected boolean prePaint(SurfaceRender r) {
        return true;
    }


    @Override
    public Surface finger(Finger finger) {

        if (!showing())
            return null;

        if (childrenCount() > 0) {


            if (finger == null) {
                forEach(c -> c.finger(null));
                return null;
            } else {

                Surface[] found = new Surface[1];


                float fx = finger.pos.x;
                float fy = finger.pos.y;


                whileEachReverse(c -> {


                    if (!c.showing())
                        return true;

                    if ((c instanceof Container && !((Container) c).clipBounds) || (
                            c.bounds.contains(fx, fy))) {

                        Surface s = c.finger(finger);
                        if (s != null) {


                            found[0] = s;
                            return false;
                        }
                    }

                    return true;

                });

                if ((found[0]) != null)
                    return found[0];
            }
        }

        return null;
    }

    protected abstract int childrenCount();



    @Override
    public boolean stop() {
        if (super.stop()) {
            forEach(Surface::stop);
            //TODO: clear();
            return true;
        }
        return false;
    }

    abstract public void forEach(Consumer<Surface> o);

    public void forEachRecursively(Consumer<Surface> o) {

        o.accept(this);

        forEach(z -> {
            if (z instanceof Container)
                ((Container) z).forEachRecursively(o);
            else
                o.accept(z);
        });

    }

    protected abstract boolean whileEach(Predicate<Surface> o);

    protected abstract boolean whileEachReverse(Predicate<Surface> o);


}
