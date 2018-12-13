package spacegraph.space2d.container;

import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.data.atomic.MetalAtomicIntegerFieldUpdater;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.SurfaceRender;
import jcog.math.v2;

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

    /** first sub-layer */
    protected void paintIt(GL2 gl, SurfaceRender r) {

    }

    /** last sub-layer */
    protected void paintAbove(GL2 gl, SurfaceRender r) {

    }



    @Override
    protected void compile(SurfaceRender r) {
        if (!prePaint(r)) {
            showing = false;
            return;
        } else {
            showing = true;
        }

        if (MUSTLAYOUT.compareAndSet(this, 1, 0)) {
            doLayout(r.dtMS);
        }

        r.on(this::doPaint);

        forEach(c -> c.recompile(r));
    }

    @Override
    protected final void paint(GL2 gl, SurfaceRender r) {

        doPaint(gl, r);

    }


    private final void doPaint(GL2 gl, SurfaceRender r) {

        paintIt(gl, r);

        paintAbove(gl, r);

    }



    protected boolean prePaint(SurfaceRender r) {
        return true;
    }


    @Override
    public Surface finger(Finger finger) {

        if (!showing())
            return null;

        if (childrenCount() > 0) {

            Surface[] found = new Surface[1];

            v2 fp = finger.posOrtho;
            float fx = fp.x, fy = fp.y;

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
