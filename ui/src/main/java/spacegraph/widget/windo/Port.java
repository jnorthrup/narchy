package spacegraph.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.data.graph.NodeGraph;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.api.block.procedure.primitive.IntObjectProcedure;
import spacegraph.Surface;
import spacegraph.SurfaceBase;
import spacegraph.input.Finger;
import spacegraph.input.Wiring;
import spacegraph.render.Draw;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** base class for a port implementation
 * @see http://rawbots.wikia.com/wiki/Category:Visual_Programming_Operands
 * */
public class Port extends Widget implements Wiring.Wireable {

    transient volatile protected Wiring beingWiredOut = null;
    transient volatile protected Wiring beingWiredIn = null;
    private boolean enabled = true;

    /** input handler */
    private In in = null;

    /** prototype (example) builder.  stipulates a protocol as specified by an example instance */
    private Supplier specifyHow = null;

    /** prototype (example) acceptor. accepts a protocol (on connect / re-connect) */
    private Consumer obeyHow = null;

    private IntObjectProcedure<Port> updater = null;

    private transient NodeGraph.Node<Surface, Wire> node;



    public Port() {
        super();
    }

    public Port(In i) {
        this();
        on(i);
    }

    /** for convenience */
    public Port(Consumer<?> i) {
        this();
        on(i);
    }


    public <X> Port on(Consumer<X> i) {
        return on((Wire w,X x)->i.accept(x));
    }

    public <X> Port specify(Supplier<X> proto) {
        this.specifyHow = proto;
        return this;
    }

    public <X> Port obey(Consumer<X> withRecievedProto) {
        this.obeyHow = withRecievedProto;
        return this;
    }

    /** set the input handler */
    public <X> Port on(@Nullable In<X> i) {
        this.in = i; return this;
    }

    public Port update(@Nullable Runnable update) {
        this.updater = (i,p)->update.run();
        return this;
    }

    public Port update(@Nullable Consumer<Port> update) {
        this.updater = (i,p)->update.accept(p);
        return this;
    }

    public Port update(@Nullable IntObjectProcedure<Port> update) {
        this.updater = update;
        return this;
    }

//    public Wire link(Port target) {
//        PhyWall.PhyWindow pw = parent(PhyWall.PhyWindow.class);
//        if (pw == null)
//            throw new RuntimeException("port not materialized");
//        return pw.link(this,target);
//    }

    public boolean enabled() {
        return enabled;
    }

    public boolean connected(Port other) {
        if (other.specifyHow !=null) {

            if (specifyHow!=null) {
                //both specify a protocol, so test that the spec matches
                return specifyHow.get().equals(other.specifyHow.get());
            }

            if (obeyHow!=null) {
                //teach how to obey the protocol
                obeyHow.accept(other.specifyHow.get());
            }
        }

        return true;
    }

    @FunctionalInterface
    public interface In<T> {

        /** TODO more informative backpressure-determining state
         *  TODO pluggable receive procedure:
         *          local buffers (ex: QueueLock), synch, threadpool, etc
         * */

        /** test before typed wire connection */
        default boolean accept(T proto) {
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

        if (beingWiredOut !=null) {
            gl.glColor4f(0.5f, 1, 0, 0.35f);
            Draw.rect(gl, bounds);
        }
        if (beingWiredIn !=null) {
            gl.glColor4f(0, 0.5f, 1, 0.35f);
            Draw.rect(gl, bounds);
        }


    }


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
        if (preOrPost && !acceptWiring(w)) {
            this.beingWiredIn = null;
            return false;
        }
        this.beingWiredIn = preOrPost ? w : null;
        return true;
    }


    @Override
    public void onWireOut(@Nullable Wiring w, boolean preOrPost) {
        this.beingWiredOut = preOrPost ? w : null;
        if (!preOrPost) {
            onWired(w);
        }
    }

    /** wiring complete */
    protected void onWired(Wiring w) {

    }


    @Override
    public void prePaint(int dtMS) {
        IntObjectProcedure<Port> u = this.updater;
        if (u !=null)
            u.value(dtMS, this);

        super.prePaint(dtMS);
    }

    public final void out(Object x) {
        out(this, x);
    }

    @Override
    public void start(@Nullable SurfaceBase parent) {
        synchronized(this) {
            super.start(parent);
            this.node = parent(PhyWall.class).links.addNode(this);
            IntObjectProcedure<Port> u = this.updater;
            if (u !=null)
                u.value(0, this);
        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            parent(PhyWall.class).links.removeNode(this);
            node = null;
            super.stop();
        }
    }

    protected final void out(Port sender, Object x) {
        //TODO optional transfer function
        if (enabled) {
            node.edges(true, true).forEach(t -> t.id.in(sender, x));
        }
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
