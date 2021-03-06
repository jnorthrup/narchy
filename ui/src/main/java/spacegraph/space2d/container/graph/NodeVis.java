package spacegraph.space2d.container.graph;

import com.jogamp.opengl.GL2;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.data.pool.Pool;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.util.MutableRectFloat;
import spacegraph.video.Draw;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class NodeVis<X> extends Windo {

    public transient volatile X id; //TODO WeakReference

    /**
     * optional priority component
     */
    public float pri;

    /**
     * current layout movement instance
     */
    public /*volatile*/ transient MutableRectFloat mover = null;

    /**
     * outgoing edges
     */
    public final ConcurrentFastIteratingHashMap<X, EdgeVis<X>> outs = new ConcurrentFastIteratingHashMap(new EdgeVis[0]);

    private float r;
    private float g;
    private float b;
    private float a;

    /** general re-purposeable serial integer */
    public transient int i;

    void start(X id) {
        this.i = Integer.MIN_VALUE;
        this.id = id;
        pri = 0.5f;
        r = g = b = 0.5f;
    }

    void end(Pool<EdgeVis<X>> edgePool) {
        hide();
        removeOuts(edgePool);
        this.mover = null;
        this.id = null;
        this.i = Integer.MIN_VALUE;
    }



    //        @Override
//        public boolean stop() {
//            if (super.stop()) {
//                return true;
//            }
//            return false;
//        }

    void paintEdges(GL2 gl) {
        outs.forEachValue(new Consumer<EdgeVis<X>>() {
            @Override
            public void accept(EdgeVis<X> x) {
                x.draw(NodeVis.this, gl);
            }
        });
    }

    @Override
    protected void paintIt(GL2 gl, ReSurface r) {
        gl.glColor4f(this.r, g, b, a);
        Draw.rect(bounds, gl);
    }

    public void color(float r, float g, float b) {
        color(r,g,b, 1.0F);
    }

    public static boolean pinned() {

        return false;
    }

    private void removeOuts(Pool<EdgeVis<X>> pool) {
        outs.clear(pool::put);
    }

//        private boolean removeOut(EdgeVis<X> x, Pool<EdgeVis<X>> pool) {
//            EdgeVis<X> y = outs.remove(x.to.id);
//            if (y != null) {
//                pool.put(y);
//                return true;
//            }
//            return false;
//        }

    /**
     * adds or gets existing edge
     */
    EdgeVis<X> out(NodeVis<X> target, Pool<EdgeVis<X>> pool) {

        X tid = target.id;
        if (tid == null)
            return null;

        EdgeVis<X> y = outs.compute(tid, new BiFunction<X, EdgeVis<X>, EdgeVis<X>>() {
            @Override
            public EdgeVis<X> apply(X tt, EdgeVis<X> yy) {
                EdgeVis<X> yy1 = yy;
                if (yy1 == null) {
                    yy1 = pool.get();
                    yy1.to = target;
                }
                return yy1;
            }
        });
        y.invalid = false;
        return y;
    }

    public void update() {
        //remove dead edges, or edges to NodeVis's which have been recycled after removal
        outs.removeIf(new BiPredicate<X, EdgeVis<X>>() {
            @Override
            public boolean test(X x, EdgeVis<X> e) {
                if (e.invalid) return true;
                NodeVis<X> ee = e.to;
                return ee == null || !x.equals(ee.id);
            }
        });
    }

    void invalidateEdges() {
        outs.forEachValue(new Consumer<EdgeVis<X>>() {
            @Override
            public void accept(EdgeVis<X> e) {
                e.invalid = true;
            }
        });
    }

    public void color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
}
