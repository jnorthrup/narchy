package spacegraph.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.data.graph.ImmutableDirectedEdge;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.input.Wiring;
import spacegraph.render.Draw;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.function.Consumer;

/** base class for a port implementation
 * @see http://rawbots.wikia.com/wiki/Category:Visual_Programming_Operands
 * */
public class Port extends Widget implements Wiring.Wireable {

    protected Wiring wiringOut = null;
    protected Wiring wiringIn = null;
    private boolean enabled = true;

    /** input handler */
    private InPort in = null;

    public Port() {
        super();
    }

    public Port(InPort i) {
        this();
        on(i);
    }

    /** for convenience */
    public Port(Consumer<?> i) {
        this();
        on(i);
    }

    public void on(Consumer i) {
        on((w,x)->i.accept(x));
    }

    /** set the input handler */
    public void on(@Nullable InPort i) {
        this.in = i;
    }

    public Wire link(Port target) {
        PhyWall.PhyWindow pw = parent(PhyWall.PhyWindow.class);
        if (pw == null)
            throw new RuntimeException("port not materialized");
        return pw.link(this,target);
    }

    public boolean enabled() {
        return enabled;
    }

    @FunctionalInterface
    public interface InPort<T> {

        /** TODO more informative backpressure-determining state
         *  TODO pluggable receive procedure:
         *          local buffers (ex: QueueLock), synch, threadpool, etc
         * */

        /** test before typed wire connection */
        default boolean accept(Type t) {
            return true;
        }

        void accept(Wire from, T t);

    }

//            final FingerDragging dragInit = new FingerDragging(0) {
//
//                @Override
//                public void start(Finger f) {
//                    //root().debug(this, 1f, ()->"fingering " + this);
//                }
//
//                @Override
//                protected boolean drag(Finger f) {
//                    SurfaceRoot root = root();
//                    root.debug(this, 1f, ()->"drag " + this);
//                    if (f.tryFingering(this))
//                        return false;
//                    else
//                        return true;
//                }
//            };


    @Override
    protected void paintWidget(GL2 gl, RectFloat2D bounds) {

        if (wiringOut !=null) {
            gl.glColor4f(0, 1, 0, 0.35f);
            Draw.rect(gl, bounds);
        }
        if (wiringIn!=null) {
            gl.glColor4f(0, 0, 1, 0.35f);
            Draw.rect(gl, bounds);
        }


    }

//    @Override
//    public boolean tangible() {
//        return false;
//    }

    @Override
    public Surface onTouch(Finger finger, short[] buttons) {



        if (finger!=null && buttons!=null) {
            Surface x = super.onTouch(finger, buttons);
            if (x==null || x==this) {
                if (finger.tryFingering(new LinkingWiring(this)))
                    return this;
            }

            return x;
        }

        return super.onTouch(finger, buttons);


//                Surface c = super.onTouch(finger, buttons);
//                if (c != null)
//                    return c;


    }


    protected boolean acceptWiring(Wiring w) {
        return true;
    }

    @Override
    public boolean onWireIn(@Nullable Wiring w, boolean preOrPost) {
        if (preOrPost && !acceptWiring(w))
            return false;
        this.wiringIn = preOrPost ? w : null;
        return true;
    }


    @Override
    public void onWireOut(@Nullable Wiring w, boolean preOrPost) {
        this.wiringOut = preOrPost ? w : null;
        if (!preOrPost) {
            onWired(w);
        }
    }

    /** wiring complete */
    protected void onWired(Wiring w) {

    }



    public void out(Object x) {
        out(this, x);
    }

    protected void out(Port sender, Object x) {
        PhyWall.PhyWindow w = parent(PhyWall.PhyWindow.class);
        if (w==null)
            throw new NullPointerException();

        //TODO optional transfer function

        Iterable<ImmutableDirectedEdge<Surface, Wire>> targets = w.edges(this);
        targets.forEach((t)->{
            t.id.in(sender, x);
        });
    }

    public boolean in(Wire from, Object s) {
        if (!enabled || this.in == null) {
            return false;
        } else {
            try {
                this.in.accept(from, s);
                return true;
            } catch (Throwable t) {
                root().error(this, 1f, t);
                return false;
            }
        }
    }

    public void enable(boolean b) {
        this.enabled = b;
    }
}
