package spacegraph.layout;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;

import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 7/20/16.
 */
abstract public class Container extends Surface {

    //final AtomicBoolean mustLayout = new AtomicBoolean(true);
    boolean mustLayout = true;

    protected boolean clipTouchBounds = true;


    @Override
    public final void layout() {
        mustLayout = true;
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
    public final Surface pos(RectFloat2D r) {
        if (posChanged(r))
            layout();
        return this;
    }

    protected void paintAbove(GL2 gl) {

    }

    protected void paintBelow(GL2 gl) {

    }

    /**
     * paints the component above the background drawn ahead of this
     */
    protected void paintIt(GL2 gl) {

    }

    @Override
    protected final void paint(GL2 gl, int dtMS) {

        //TODO maybe in a separate update thread
        if (mustLayout) {
            doLayout(dtMS);
            mustLayout = false;
        }

        prePaint(dtMS);

        paintBelow(gl);

        paintIt(gl);

        forEach(c -> c.render(gl, dtMS)); //render children, if any

        paintAbove(gl);
    }

    protected void prePaint(int dtMS) {

    }


    @Override
    public Surface onTouch(Finger finger, short[] buttons) {
        Surface x = super.onTouch(finger, buttons);
        if (x != null)
            return x;

        //2. test children reaction

        if (childrenCount() > 0) {

            // Draw forward, propagate touch events backwards
            if (finger == null) {
                forEach(c -> c.onTouch(finger, null));
                return null;
            } else {

                //HACK
                final Surface[] found = {null};
                float fx = finger.hit.x;
                float fy = finger.hit.y;
                forEach(c -> {

                    if (found[0] != null) //TODO use whileEach() with a predicate for fast terminate
                        return;

                    //TODO factor in the scale if different from 1

                    //                if (/*csx != csx || */csx <= 0 || /*csy != csy ||*/ csy <= 0)
                    //                    return;

                    //project to child's space

                    //subHit.sub(tx, ty);

                    //                float hx = relativeHit.x, hy = relativeHit.y;

                    if (!clipTouchBounds || (
                            fx >= c.left() && fx <= c.right() && fy >= c.top() && fy <= c.bottom())) {


                        Surface s = c.onTouch(finger, buttons);
                        if (s != null) {
                            if (found[0] == null || found[0].bounds.cost() > s.bounds.cost())
                                found[0] = s; //FIFO
                        }
                    }

                });

                if ((found[0]) != null)
                    return found[0];
            }
        }

        return tangible() ? this : null;
    }

    abstract public int childrenCount();

    public boolean tangible() {
        return false;
    }

    @Override
    public boolean onKey(KeyEvent e, boolean pressed) {
        if (!super.onKey(e, pressed)) {
            whileEach(c -> c.onKey(e, pressed));
        }
        return false;
    }

    @Override
    public boolean onKey(v2 hitPoint, char charCode, boolean pressed) {
        if (!super.onKey(hitPoint, charCode, pressed)) {
            forEach(c -> c.onKey(hitPoint, charCode, pressed));
        }
        return false;
    }

    @Override
    public void stop() {
        synchronized (this) {
            forEach(Surface::stop);
            super.stop();
        }
    }

    abstract public void forEach(Consumer<Surface> o);

    abstract public boolean whileEach(Predicate<Surface> o);

    /**
     * identity compare
     */
    static boolean equals(List x, Object[] y) {
        int s = x.size();
        if (s != y.length) return false;
        for (int i = 0; i < s; i++) {
            if (x.get(i) != y[i])
                return false;
        }
        return true;
    }

    /**
     * identity compare
     */
    static boolean equals(List x, List y) {
        int s = x.size();
        if (s != y.size()) return false;
        for (int i = 0; i < s; i++) {
            if (x.get(i) != y.get(i))
                return false;
        }
        return true;
    }

}
