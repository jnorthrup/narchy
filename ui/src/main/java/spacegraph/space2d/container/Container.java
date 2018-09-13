package spacegraph.space2d.container;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.util.math.v2;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 7/20/16.
 */
abstract public class Container extends Surface {

    private volatile boolean mustLayout = true;




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
    public final <S extends Surface> S pos(RectFloat2D r) {
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

        int dtMS = r.dtMS;
        if (!prePaint(r)) {
            showing = false;
            return;
        } else {
            showing = true;
        }

        
        if (mustLayout) {
            mustLayout = false;
            doLayout(dtMS);
        }

        paintBelow(gl, r);

        paintIt(gl);

        renderContents(gl, r);

        paintAbove(gl, r);
    }

    public final void renderContents(GL2 gl, SurfaceRender r) {
        forEach(c -> c.render(gl, r));
    }

    protected boolean prePaint(SurfaceRender r) {
        prePaint(r.dtMS);
        return true;
    }

    @Deprecated public void prePaint(int dtMS) {

    }


    @Override
    public Surface tryTouch(Finger finger) {

        if (!showing())
            return null;

        if (childrenCount() > 0) { 

            
            if (finger == null) {
                forEach(c -> c.tryTouch(null));
                return null;
            } else {

                Surface[] found = new Surface[1];

                
                float fx = finger.pos.x;
                float fy = finger.pos.y;

                
                whileEachReverse(c -> {




                    

                    
                    

                    

                    

                    

                    if (!c.showing())
                        return true; 

                    if ((c instanceof Container && !((Container)c).clipBounds) || (
                            fx >= c.left() && fx <= c.right() && fy >= c.top() && fy <= c.bottom())) {


                        Surface s = c.tryTouch(finger);
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

        return tangible() ? this : null;
    }

    protected abstract int childrenCount();

    protected boolean tangible() {
        return false;
    }

    @Override
    public boolean tryKey(KeyEvent e, boolean pressed) {
        if (visible() && !super.tryKey(e, pressed)) {
            return whileEach(c -> c.tryKey(e, pressed));
        }
        return false;
    }

    @Override
    public boolean tryKey(v2 hitPoint, char charCode, boolean pressed) {
        if (visible() && !super.tryKey(hitPoint, charCode, pressed)) {
            return whileEach(c -> c.tryKey(hitPoint, charCode, pressed));
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            forEach(Surface::stop);
            return true;
        }
        return false;
    }

    abstract public void forEach(Consumer<Surface> o);

    protected abstract boolean whileEach(Predicate<Surface> o);

    protected abstract boolean whileEachReverse(Predicate<Surface> o);


}
