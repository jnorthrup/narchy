package spacegraph.space2d.widget.windo.util;

import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.Util;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.GraphEdit2D;
import spacegraph.space2d.container.graph.Link;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.space2d.widget.shape.VerletSurface;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.video.Draw;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.behavior.AttractionBehavior2D;
import toxi.physics2d.spring.VerletSpring2D;

import java.util.List;

public class VerletGraphEditPhysics extends GraphEditPhysics {

    protected final VerletSurface physics = new VerletSurface();

    final ConcurrentFastIteratingHashMap<Surface, PhySurface> w =
        new ConcurrentFastIteratingHashMap<>(new PhySurface[0]);


    public static final class PhySurface {

        public final Surface surface;
        public final Vec2D center;
        public final AttractionBehavior2D repel;

        private PhySurface(Surface surface) {
            this.surface = surface;
            this.center = new Vec2D();
            this.repel = new AttractionBehavior2D(center, 1, 0);
        }

        public void update() {
            center.set(surface.cx(), surface.cy());
            repel.setRadius(surface.radius() * 2);
            repel.setStrength(-(float) (Math.sqrt(surface.bounds.area()) * 0.1f));
        }
    }

    @Override
    protected void starting(GraphEdit2D parent) {
        physics.physics.setDrag(0.25f);
        physics.debugRender.set(false);
        physics.pos(parent.bounds);
        physics.start(parent);
        below = physics;
    }

    @Override
    public final void invokeLater(Runnable o) {
        //physics.invoke(o);
        throw new TODO();
    }

    @Override
    public void stop() {
        physics.stop();
    }

    @Override
    public PhySurface add(Surface x) {
        return this.w.computeIfAbsent(x, (ww->{
            PhySurface wd = new PhySurface(ww);
            physics.physics.addBehavior(wd.repel);
            return wd;
        }));
    }

    public void update() {
        for (PhySurface w : this.w.valueArray()) {
            w.update();
        }
    }

    @Override
    public void remove(Surface x) {
        PhySurface removed = this.w.remove(x);
        physics.physics.removeBehavior(removed.repel);
    }


    @Override
    public Link link(Wire w) {
        return new VerletVisibleLink(w);
    }

    class VerletVisibleLink extends GraphEdit2D.VisibleLink {

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
                VerletPhysics2D verletPhysics2D = physics.physics;
                for (VerletSpring2D spring : springs) {
                    verletPhysics2D.removeSpringAndItsParticles(spring);
                }
            });

            final List<VerletParticle2D> points = chain.getOne();
//        VerletParticle2D first = points.get(0);
//        VerletParticle2D last = points.get(points.size() - 1);
            VerletParticle2D mid = points.get(points.size() / 2);


//        if (first!=mid) {
//            mid.addBehaviorGlobal(new AttractionBehavior2D<>(mid, 300, -1));
//        }


            bind(graph.add(new PushButton("x", () -> remove(graph)), ff ->
                            new Windo(new MetaFrame(ff))).resize(20, 20),
                    mid, false, VerletSurface.VerletSurfaceBinding.Center, graph);


            bind(graph.add(new PushButton(".."), Windo::new).resize(5, 5),
                    chain.getOne().get(1), false, VerletSurface.VerletSurfaceBinding.Center, graph);
            bind(graph.add(new PushButton(".."), Windo::new).resize(5, 5),
                    chain.getOne().get(chainLen - 2), false, VerletSurface.VerletSurfaceBinding.Center, graph);

            /** link rendering */
            Surface r = renderer(chain);
            on(r);

            graph.addRaw(r);
        }

        void bind(Surface gripWindow, VerletParticle2D particle, boolean surfaceOverrides, VerletSurface.VerletSurfaceBinding where, GraphEdit2D g) {
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

            @Override protected void paintLink(GL2 gl, ReSurface reSurface) {
                int window = 100 * 1000 * 1000;
                long renderStart = reSurface.frameNS;

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
