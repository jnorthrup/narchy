package spacegraph.space2d.widget.windo.util;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.port.Wire;
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

    @Override
    protected Surface starting(GraphEdit<?> parent) {
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
        this.w.put(x, w);
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
    public Link link(Wire w) {
        return new VerletVisibleLink(w);
    }

    class VerletVisibleLink extends GraphEdit.VisibleLink {

        public VerletVisibleLink(@Nullable Wire w) {
            super(w);
            Surface a = w.a;
            Surface b = w.b;
            VerletParticle2D ap = physics.bind(a, VerletSurface.VerletSurfaceBinding.NearestSurfaceEdge);
            VerletParticle2D bp = physics.bind(b, VerletSurface.VerletSurfaceBinding.NearestSurfaceEdge);

            int extraJoints = 3;
            int chainLen = 2 + 1 + (extraJoints * 2); //should be an odd number

            Pair<List<VerletParticle2D>, List<VerletSpring2D>> chain = physics.addParticleChain(ap, bp,
                    chainLen, 0f /* some minimal # */, 0.5f);

            List<VerletSpring2D> springs = chain.getTwo();
            on(() -> {
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
                    chain.getOne().get(chainLen - 2), false, VerletSurface.VerletSurfaceBinding.Center, graph);

            /** link rendering */
            Surface r = renderer(chain);
            on(r);

            graph.addRaw(r);
        }

        void bind(Surface gripWindow, VerletParticle2D particle, boolean surfaceOverrides, VerletSurface.VerletSurfaceBinding where, GraphEdit g) {
            physics.bind(gripWindow, particle, surfaceOverrides, where);
            on(gripWindow);
        }

        private Surface renderer(Pair<List<VerletParticle2D>, List<VerletSpring2D>> chain) {
            return new VerletVisibleLinkSurface(chain);
        }

        private class VerletVisibleLinkSurface extends VisibleLinkSurface {

            private final Pair<List<VerletParticle2D>, List<VerletSpring2D>> chain;

            public VerletVisibleLinkSurface(Pair<List<VerletParticle2D>, List<VerletSpring2D>> chain) {
                this.chain = chain;
            }

            @Override protected void paintLink(GL2 gl, SurfaceRender surfaceRender) {
                int window = 100 * 1000 * 1000;
                long renderStart = surfaceRender.restartNS;

                Wire id = VerletVisibleLink.this.id;
                float aa = id.activity(true, renderStart, window);
                float bb = id.activity(false, renderStart, window);

                float base = Math.min(a().radius(), b().radius());
                float baseA = base * Util.lerp(aa, 0.25f, 0.75f);
                float baseB = base * Util.lerp(bb, 0.25f, 0.75f);
                Draw.colorHash(gl, id.typeHash(true), 0.25f + 0.45f * aa);
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
