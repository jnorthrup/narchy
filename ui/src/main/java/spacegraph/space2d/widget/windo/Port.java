package spacegraph.space2d.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.data.graph.Node;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.api.block.procedure.primitive.IntObjectProcedure;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.Wiring;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.video.Draw;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** base class for a port implementation
 * @see http:
 * */
public class Port extends Widget implements Wiring.Wireable {

    private transient volatile Wiring beingWiredOut = null;
    private transient volatile Wiring beingWiredIn = null;
    private boolean enabled = true;

    /** input handler */
    private In in = null;

    /** prototype (example) builder.  stipulates a protocol as specified by an example instance */
    private Supplier specifyHow = null;

    /** prototype (example) acceptor. accepts a protocol (on connect / re-connect) */
    private Consumer obeyHow = null;

    private IntObjectProcedure<Port> updater = null;

    private transient Node<spacegraph.space2d.Surface,spacegraph.space2d.widget.windo.Wire> node;



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








    public boolean enabled() {
        return enabled;
    }

    public boolean connected(Port other) {
        if (other.specifyHow !=null) {

            if (specifyHow!=null) {
                
                return specifyHow.get().equals(other.specifyHow.get());
            }

            if (obeyHow!=null) {
                
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




















    @Override
    protected void paintWidget(GL2 gl, RectFloat2D bounds) {

        if (beingWiredOut !=null) {
            gl.glColor4f(0.5f, 1, 0, 0.35f);
            Draw.rect(bounds, gl);
        }
        if (beingWiredIn !=null) {
            gl.glColor4f(0, 0.5f, 1, 0.35f);
            Draw.rect(bounds, gl);
        }


    }


    @Override
    public Surface finger(Finger finger) {



        if (finger!=null /*&& buttons!=null*/) {
            Surface x = super.finger(finger);
            if (x==null || x==this) {
                if (finger.tryFingering(new LinkingWiring(this)))
                    return this;
            }

            return x;
        }

        return super.finger(finger);







    }


    private boolean acceptWiring(Wiring w) {
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
    void onWired(Wiring w) {

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
    public boolean start(@Nullable SurfaceBase parent) {

        if (super.start(parent)) {
            this.node = parent(GraphWall.class).links.addNode(this);
            IntObjectProcedure<Port> u = this.updater;
            if (u !=null)
                u.value(0, this);
            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        GraphWall p = parent(GraphWall.class);
        if (super.stop()) {
            if (p!=null)
                p.links.removeNode(this);
            node = null;
            return true;
        }
        return false;
    }
    /** TODO Supplier-called version of this */
    private void out(Port sender, Object x) {
        
        if (enabled) {
            Node<spacegraph.space2d.Surface,spacegraph.space2d.widget.windo.Wire> n = this.node;
            if (n!=null)
                n.edges(true, true).forEach(t -> { if (t!=sender) { t.id().in(sender, x); } } );
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
