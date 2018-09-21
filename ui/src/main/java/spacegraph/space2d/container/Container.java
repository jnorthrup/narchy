package spacegraph.space2d.container;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
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
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            layout();
            return true;
        }
        return false;
    }

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

                    if ((c instanceof Container && !((Container)c).clipBounds) || (
                            fx >= c.left() && fx <= c.right() && fy >= c.top() && fy <= c.bottom())) {


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

        return tangible() ? this : null;
    }

    protected abstract int childrenCount();

    protected boolean tangible() {
        return false;
    }

    @Override
    public boolean key(KeyEvent e, boolean pressed) {
        if (visible() && !super.key(e, pressed)) {
            return whileEach(c -> c.key(e, pressed));
        }
        return false;
    }

    @Override
    public boolean key(v2 hitPoint, char charCode, boolean pressed) {
        if (visible() && !super.key(hitPoint, charCode, pressed)) {
            return whileEach(c -> c.key(hitPoint, charCode, pressed));
        }
        return false;
    }

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
               ((Container)z).forEachRecursively(o);
           else
               o.accept(z);
        });

    }

    protected abstract boolean whileEach(Predicate<Surface> o);

    protected abstract boolean whileEachReverse(Predicate<Surface> o);


}
