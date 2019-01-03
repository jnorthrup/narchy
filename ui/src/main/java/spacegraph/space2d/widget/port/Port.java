package spacegraph.space2d.widget.port;

import com.jogamp.opengl.GL2;
import jcog.data.graph.Node;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.block.procedure.primitive.IntObjectProcedure;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.SurfaceRoot;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.port.util.Wiring;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.video.Draw;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * base class for a port implementation
 *
 * @see http:
 */
public class Port<X> extends Widget implements Wiring.Wireable {

    private transient volatile Wiring beingWiredOut = null;
    private transient volatile Wiring beingWiredIn = null;
    private boolean enabled = true;

    /**
     * input handler
     */
    public In<? super X> in = null;

    /**
     * prototype (example) builder.  stipulates a protocol as specified by an example instance
     */
    private Supplier specifyHow = null;

    /**
     * prototype (example) acceptor. accepts a protocol (on connect / re-connect)
     */
    private Consumer obeyHow = null;

    private IntObjectProcedure<Port<X>> updater = null;

    private transient Node<spacegraph.space2d.Surface, Wire> node;

    static final private int WIRING_BUTTON = 2;


    public Port() {
        super();
    }


    public Port(In<? super X> i) {
        this();
        on(i);
    }

    /**
     * for convenience
     */
    public Port(Consumer<? super X> i) {
        this();
        on(i);
    }


    public Port<X> on(Consumer<? super X> i) {
        return on((Wire w, X x) -> i.accept(x));
    }

    public Port<X> specify(Supplier<X> proto) {
        this.specifyHow = proto;
        return this;
    }

//    public Port<X> obey(Consumer<? super X> withRecievedProto) {
//        this.obeyHow = withRecievedProto;
//        return this;
//    }

    /**
     * set the input handler
     */
    public Port<X> on(@Nullable In<? super X> i) {
        this.in = i;
        return this;
    }

    public Port<X> update(@Nullable Runnable update) {
        this.updater = (i, p) -> update.run();
        return this;
    }

    public Port<X> update(@Nullable Consumer<Port<X>> update) {
        this.updater = (i, p) -> update.accept(p);
        return this;
    }

    public Port<X> update(@Nullable IntObjectProcedure<Port<X>> update) {
        this.updater = update;
        return this;
    }


    public boolean enabled() {
        return enabled;
    }

    final boolean connectable(Port other) {
        if (other.specifyHow != null) {

            if (specifyHow != null) {

                return specifyHow.get().equals(other.specifyHow.get());
            }

            if (obeyHow != null) {

                obeyHow.accept(other.specifyHow.get());
            }
        }

        return true;
    }

    /* override in subclasses to implement behavior to be executed after wire connection has been established in the graph. */
    void connected(Port a) {

    }

    @FunctionalInterface
    public interface In<T> {

        /**
         * TODO more informative backpressure-determining state
         * TODO pluggable receive procedure:
         * local buffers (ex: QueueLock), synch, threadpool, etc
         */

//        /** test before typed wire connection */
//        default boolean canAccept(T proto) {
//            return true;
//        }

        void accept(Wire from, T t);


    }


    @Override
    protected void paintWidget(RectFloat bounds, GL2 gl) {

        if (beingWiredOut != null) {
            gl.glColor4f(0.5f, 1, 0, 0.35f);
        } else if (beingWiredIn != null) {
            gl.glColor4f(0, 0.5f, 1, 0.35f);
        } else {
            gl.glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
        }

        Draw.rect(bounds, gl);
    }


    @Override
    public Surface finger(Finger finger) {

        Surface x = super.finger(finger);
        if (x == null || x == this) {
            if (finger.pressedNow(WIRING_BUTTON)) {
                if (finger.tryFingering(new Wiring(WIRING_BUTTON, this)))
                    return this;

            } else {
                return null;
            }
        }

        return x;

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

    /**
     * wiring complete
     */
    protected void onWired(Wiring w) {

    }


    @Override
    public boolean prePaint(SurfaceRender r) {
        if (super.prePaint(r)) {
            IntObjectProcedure<Port<X>> u = this.updater;
            if (u != null)
                u.value(r.dtMS, this);

            return true;
        }
        return false;
    }

    public boolean out(X x) {
        return out(this, x);
    }

    @Override
    protected void starting() {
        super.starting();

        this.node = parent(GraphEdit.class).links.addNode(this);
        IntObjectProcedure<Port<X>> u = this.updater;
        if (u != null)
            u.value(0, this);

    }

    @Override
    public boolean stop() {
        GraphEdit p = parent(GraphEdit.class);
        if (super.stop()) {
            node = null;
            if (p != null)
                p.links.removeNode(this);
            return true;
        }
        return false;
    }


    /**
     * TODO Supplier-called version of this
     */
    protected final boolean out(Port<?> sender, X x) {
        if (enabled) {
            Node<spacegraph.space2d.Surface, Wire> n = this.node;
            if (n != null) {
                n.edges(true, true).forEach(t -> {
                    Wire wire = t.id();
                    Port recv = ((Port) wire.other(Port.this));
                    if (recv!=sender) //1-level cycle block
                        wire.send(this, recv, x);
                });
                return true;
            }
        }
        return false;
    }

    public boolean recv(Wire from, X s) {
        if (!enabled) {
            return false;
        } else {
            In<? super X> in = this.in;
            if (in != null) {
                try {
                    in.accept(from, s);
                    return true;
                } catch (Throwable t) {
                    SurfaceRoot r = root();
                    if (r != null)
                        r.error(this, 1f, t);
                    else
                        t.printStackTrace(); //TODO HACK
                    return false;
                }
            }
            return true;
        }

    }

    public void enable(boolean b) {
        this.enabled = b;
    }

    public boolean active() {
        return enabled && node != null && node.edgeCount(true, true) > 0;
    }

}
