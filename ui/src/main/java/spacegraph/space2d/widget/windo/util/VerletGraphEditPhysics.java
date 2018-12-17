package spacegraph.space2d.widget.windo.util;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.port.util.Wire;
import spacegraph.space2d.widget.shape.VerletSurface;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.space2d.widget.windo.Link;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.video.Draw;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.behavior.AttractionBehavior2D;
import toxi.physics2d.spring.VerletSpring2D;

import java.util.List;

public class VerletGraphEditPhysics extends GraphEditPhysics {

    protected final VerletSurface physics = new VerletSurface();

    final ConcurrentFastIteratingHashMap<Windo, WindowData> w = new ConcurrentFastIteratingHashMap<>(new WindowData[0]);


    private static class WindowData {

        final Windo window;
        final Vec2D center;
        final AttractionBehavior2D repel;

        private WindowData(Windo window) {
            this.window = window;
            this.center = new Vec2D();
            this.repel = new AttractionBehavior2D(center, 1, 0);
        }

        public void update() {
            center.set(window.cx(), window.cy());
            repel.setRadius(window.radius() * 2);
            repel.setStrength(-(float) (Math.sqrt(window.bounds.area()) * 0.1f));
        }
    }

    @Override protected Surface starting(GraphEdit<?> parent) {
        physics.physics.setDrag(0.25f);
        physics.debugRender.set(false);
        physics.pos(parent.bounds);
        physics.start(parent);
        return physics;
    }

    @Override
    public void stop() {
        physics.stop();
    }

    @Override
    public void add(Windo x) {
        WindowData w = new WindowData(x);
        this.w.put(x,w);
        physics.physics.addBehavior(w.repel);

    }

    public void update() {
        for (WindowData w : this.w.valueArray()) {
            w.update();
        }
    }

    @Override
    public void remove(Windo x) {
        WindowData removed = this.w.remove(x);
        physics.physics.removeBehavior(removed.repel);
    }


    @Override
    public Link link(Wire w, Surface a, Surface b) {
        return new DefaultLink(w, a, b);
    }

    class DefaultLink extends Link {

        public DefaultLink(@Nullable Wire w, Surface a, Surface b) {
            super(w);
            VerletParticle2D ap = physics.bind(a, VerletSurface.VerletSurfaceBinding.NearestSurfaceEdge);
            VerletParticle2D bp = physics.bind(b, VerletSurface.VerletSurfaceBinding.NearestSurfaceEdge);

            int extraJoints = 3;
            int chainLen = 2 + 1 + (extraJoints*2); //should be an odd number

            Pair<List<VerletParticle2D>, List<VerletSpring2D>> chain = physics.addParticleChain(ap, bp,
                    chainLen, 0f /* some minimal # */, 0.5f);

            List<VerletSpring2D> springs = chain.getTwo();
            offs.add(()->{
                //destroy the chain springs on destruction
                springs.forEach(physics.physics::removeSpringAndItsParticles);
            });

            final List<VerletParticle2D> points = chain.getOne();
//        VerletParticle2D first = points.get(0);
//        VerletParticle2D last = points.get(points.size() - 1);
            VerletParticle2D mid = points.get(points.size() / 2);


//        if (first!=mid) {
//            mid.addBehaviorGlobal(new AttractionBehavior2D<>(mid, 300, -1));
//        }


            bind(graph.add(new PushButton("x", () -> remove(graph)), ff ->
                            new Windo(new MetaFrame(ff))).size(20, 20),
                    mid, false, VerletSurface.VerletSurfaceBinding.Center, graph);


            bind(graph.add(new PushButton(".."), ff ->
                            new Windo(ff)).size(5, 5),
                    chain.getOne().get(1), false, VerletSurface.VerletSurfaceBinding.Center, graph);
            bind(graph.add(new PushButton(".."), ff ->
                            new Windo(ff)).size(5, 5),
                    chain.getOne().get(chainLen-2), false, VerletSurface.VerletSurfaceBinding.Center, graph);

            /** link rendering */
            Surface r = renderer(a, b, graph, chain);
            hold(r);

            graph.raw.add(r);
        }

        public void bind(Surface gripWindow, VerletParticle2D particle, boolean surfaceOverrides, VerletSurface.VerletSurfaceBinding where, GraphEdit g) {



            physics.bind(gripWindow, particle, surfaceOverrides, where);
//        }
//        else {
//            gripWindow = null;
//        }

            hold(gripWindow);
        }

        private Surface renderer(Surface a, Surface b, GraphEdit<?> g, Pair<List<VerletParticle2D>, List<VerletSpring2D>> chain) {
            return new DefaultLink.MySurface(a, b, g, chain);
        }

        private class MySurface extends Surface {

            private final Surface a;
            private final Surface b;
            private final GraphEdit<?> g;
            private final Pair<List<VerletParticle2D>, List<VerletSpring2D>> chain;

            public MySurface(Surface a, Surface b, GraphEdit<?> g, Pair<List<VerletParticle2D>, List<VerletSpring2D>> chain) {
                this.a = a;
                this.b = b;
                this.g = g;
                this.chain = chain;
            }

            @Override
            public boolean visible() {
                if (a.parent==null || b.parent==null) {
                    DefaultLink.this.remove(g);
                    remove(); //maybe overkill
                    return false;
                }

                SurfaceBase p = parent;
                if (p instanceof Surface)
                    pos(((Surface)p).bounds); //inherit bounds

                return super.visible();
            }

            @Override protected void paint(GL2 gl, SurfaceRender surfaceRender) {
//                for (VerletParticle2D p : chain.getOne()) {
//                    float t = 2 * p.mass();
//                    Draw.rect(p.x - t / 2, p.y - t / 2, t, t, gl);
//                }

                int window = 100 * 1000 * 1000;
                long renderStart = surfaceRender.renderStartNS;

                Wire id = DefaultLink.this.id;
                float aa = id.activity(true, renderStart, window);
                float bb = id.activity(false, renderStart, window);

                float base = Math.min(a.radius(), b.radius());
                float baseA = base * Util.lerp( aa, 0.25f, 0.75f);
                float baseB = base * Util.lerp(bb, 0.25f, 0.75f);
                Draw.colorHash(gl, id.typeHash(true),  0.25f + 0.45f * aa);
                for (VerletSpring2D s : chain.getTwo()) {
                    VerletParticle2D a = s.a, b = s.b;
                    Draw.halfTriEdge2D(a.x, a.y, b.x, b.y, baseA, gl); //Draw.line(a.x, a.y, b.x, b.y, gl);
                }
                Draw.colorHash(gl, id.typeHash(false), 0.25f + 0.45f * bb);
                for (VerletSpring2D s : chain.getTwo()) {
                    VerletParticle2D a = s.a, b = s.b;
                    Draw.halfTriEdge2D(b.x, b.y, a.x, a.y, baseB, gl); //Draw.line(a.x, a.y, b.x, b.y, gl);
                }

            }
        }
    }

}
