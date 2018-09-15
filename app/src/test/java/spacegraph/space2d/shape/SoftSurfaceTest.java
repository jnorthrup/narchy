package spacegraph.space2d.shape;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.windo.GraphWall;
import spacegraph.util.animate.Animated;
import spacegraph.video.Draw;
import toxi.geom.QuadtreeIndex;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.VerletSpring2D;
import toxi.physics2d.behaviors.AttractionBehavior2D;
import toxi.physics2d.behaviors.GravityBehavior2D;
import toxi.physics2d.constraints.ParticleConstraint2D;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

class SoftSurfaceTest {

    public static class AttractTest2D extends Surface implements Animated {

        final int W = 800;

        private Off update;

        int NUM_PARTICLES = 500;
        float timeScale = 1f;

        VerletPhysics2D physics;

        private Vec2D mousePos = new Vec2D();
        final Random rng = new XoRoShiRo128PlusRandom(1);
        private AttractionBehavior2D mouseAttractor;
        private VerletParticle2D lastParticle;

        @Override
        protected void starting() {

            physics = new VerletPhysics2D(null, 3, 0);
            physics.setDrag(0.02f);
            physics.setWorldBounds(RectFloat2D.X0Y0WH(0, 0, W, W));
            physics.setIndex(
                    new QuadtreeIndex(null, -1, -1, W + 1, W + 1)
                    //new RTreeQuadTree()
            );

            physics.addBehavior(new GravityBehavior2D(new Vec2D(0, 0.1f)));

            update = root().animate(this);
        }

        @Override
        protected void stopping() {
            update.off();
            update = null;
        }

        private synchronized void addParticle() {
            VerletParticle2D p = new VerletParticle2D(Vec2D.randomVector().scale(5).addSelf(W * 0.5f, 0));
            p.mass((float) (0.5f + Math.random() * 4f));




            physics.addParticle(p);

            if (lastParticle!=null && Math.random() < 0.8f) {
                physics.addSpring(new VerletSpring2D(lastParticle, p, W/100f, 0.5f));
            } else {
                float str = Math.random() < 0.05f ? +2.2f : -1.2f;

                AttractionBehavior2D forceField = new AttractionBehavior2D(p, 30, str, 0.01f, rng);
                physics.addBehavior(forceField);
            }

            lastParticle = p;
        }


        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {
            SoftSurfaceTest.render(physics, gl);
        }


        @Override
        public Surface finger(Finger finger) {
            if (finger != null) {
                float mouseX = finger.pos.x, mouseY = finger.pos.y;
                mousePos.set(mouseX, mouseY);


                if (finger.pressing(0)) {
                    synchronized (physics) {
                        if (mouseAttractor == null) {
                            mouseAttractor = new AttractionBehavior2D(mousePos, 400, 32f);
                            physics.addBehavior(mouseAttractor);
                        }
                    }
                } else {
                    synchronized (physics) {
                        if (mouseAttractor != null) {
                            physics.removeBehavior(mouseAttractor);
                            mouseAttractor = null;
                        }
                    }
                }

            }

            return this;
        }


        @Override
        public boolean animate(float dt) {
            if (physics.particles.size() < NUM_PARTICLES)
                addParticle();
            physics.update(dt * timeScale);
            return true;
        }

        public static void main(String[] args) {
            SpaceGraph.window(new AttractTest2D(), 1200, 800);
        }

    }

    static class VerletSurface extends Surface implements Animated {

        private Off update;

        float timeScale = 1f;

        VerletPhysics2D physics;

        private boolean animateWhenInvisible = false;

        /** constrained to the surface's rectangular bounds */
        private boolean bounded = true;

        public VerletSurface(float w, float h) {
            this(RectFloat2D.X0Y0WH(0,0 , w, h));
        }

        public VerletSurface(RectFloat2D bounds) {
            super();


            pos(bounds);

            physics = new VerletPhysics2D(null, 2, 0);
            physics.setDrag(0.05f);

            physics.setIndex(
                    new QuadtreeIndex(bounds.x-1, bounds.y-1, bounds.w+1, bounds.h+1)
                    //new RTreeQuadTree()
            );

//            physics.addBehavior(new GravityBehavior2D(new Vec2D(0, 0.1f)));
        }

        @Override
        protected void starting() {
            update = root().animate(this);
        }

        @Override
        protected void stopping() {
            update.off();
            update = null;
        }

        @Override
        public boolean animate(float dt) {
            physics.bounds(bounds);
            if (animateWhenInvisible || showing()) {
                if (bounded)
                    physics.setWorldBounds(bounds);
                else
                    physics.setWorldBounds(null);

                physics.update(dt * timeScale);
            }
            return true;
        }

        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {
            SoftSurfaceTest.render(physics, gl);
        }

        public VerletParticle2D addParticleBind(Surface a) {
            VerletParticle2D ap = new VerletParticle2D(a.cx(), a.cy());
            bind(a, ap);

            physics.addParticle(ap);
            return ap;
        }

        public ParticleConstraint2D bind(Surface a, VerletParticle2D ap) {
            WeakReference<Surface> aa = new WeakReference<>(a);
            return ap.addConstraint(p -> {
                Surface aaa = aa.get();
                if (aaa == null) {
                    physics.removeParticle(p);
                } else {
                    p.set(aaa.cx(), aaa.cy());
                }
            });
        }

        public final Pair<List<VerletParticle2D>, List<VerletSpring2D>> addParticleChain(VerletParticle2D x, VerletParticle2D y, int num, float strength) {
            return addParticleChain(x, y, num, Float.NaN, strength);
        }

        public Pair<List<VerletParticle2D>, List<VerletSpring2D>> addParticleChain(VerletParticle2D a, VerletParticle2D b, int num, float chainLength, float strength) {
            assert(num > 0);
            assert(a!=b);

            if (chainLength != chainLength) {
                //auto
                chainLength = a.distanceTo(b);
            }
            float linkLength = chainLength/(num+1);
            VerletParticle2D prev = a;
            FasterList pp = new FasterList(num);
            FasterList ss = new FasterList(num+1);
            for (int i = 0; i < num; i++) {
                float p = ((float) i+1) / (num + 1);
                VerletParticle2D next =
                        new VerletParticle2D(
                                Util.lerp(p, a.x, b.x),
                                Util.lerp(p, a.y, b.y)
                        );
                pp.add(next);
                physics.addParticle(next);
                VerletSpring2D s = new VerletSpring2D(prev, next, linkLength, strength);
                ss.add(s);
                physics.addSpring(s);
                prev = next;
            }
            {
                VerletSpring2D s = new VerletSpring2D(prev, b, linkLength, strength);
                ss.add(s);
                physics.addSpring(s);
            }

            return pair(pp,ss);
        }
    }
    public static class EmbeddedVerletTest {

        public static void main(String[] args) {
            VerletSurface v = new VerletSurface(800, 800) {

                private Vec2D mousePos = new Vec2D();

                final Random rng = new XoRoShiRo128PlusRandom(1);

                private void addParticle(float x, float y) {
                    VerletParticle2D p = new VerletParticle2D(x, y);
                    float r = (float) (1f + Math.random() * 2f);
                    p.mass(r*r);

                    float str = -1;

                    AttractionBehavior2D forceField = new AttractionBehavior2D(p,
                            (float) Math.sqrt(2*r*r)*4, str, 0.01f, rng);
                    physics.addBehavior(forceField);

                    physics.addParticle(p);
                }

                @Override
                public Surface finger(Finger finger) {
                    if (finger != null) {
                        float mouseX = finger.pos.x, mouseY = finger.pos.y;
                        mousePos.set(mouseX, mouseY);


                        if (finger.pressing(0)) {
                            addParticle(mouseX, mouseY);
                        } else {

                        }

                    }

                    return this;
                }

            };

            SpaceGraph.window(v, 1200, 800);
        }

    }

    public static class VerletLinkTest extends GraphWall {

        {
            size(1200, 1200);
            Surface a = add(new PushButton("x")).pos(100, 100, 200, 200);
            Surface b = add(new PushButton("y")).pos(300, 300, 400, 400);

            VerletSurface v = new VerletSurface(bounds);
            addRaw(v);

            VerletParticle2D ap = v.addParticleBind(a);
            VerletParticle2D bp = v.addParticleBind(b);
            int chainLen = 3;
            Pair<List<VerletParticle2D>, List<VerletSpring2D>> chain = v.addParticleChain(ap, bp, chainLen, 0.01f);

            VerletParticle2D mid = chain.getOne().get(chainLen / 2);

            Surface ab = add(new PushButton("xy")).pos(RectFloat2D.XYWH(mid.x, mid.y, 50, 50));
            v.bind(ab, mid);

        }

        public static void main(String[] args) {

            VerletSurface v = new VerletSurface(800, 800) {

                private Vec2D mousePos = new Vec2D();

                final Random rng = new XoRoShiRo128PlusRandom(1);

                private void addParticle(float x, float y) {
                    VerletParticle2D p = new VerletParticle2D(x, y);
                    float r = (float) (1f + Math.random() * 2f);
                    p.mass(r*r);

                    float str = -1;

                    AttractionBehavior2D forceField = new AttractionBehavior2D(p,
                            (float) Math.sqrt(2*r*r)*4, str, 0.01f, rng);
                    physics.addBehavior(forceField);

                    physics.addParticle(p);
                }

                @Override
                public Surface finger(Finger finger) {
                    if (finger != null) {
                        float mouseX = finger.pos.x, mouseY = finger.pos.y;
                        mousePos.set(mouseX, mouseY);


                        if (finger.pressing(0)) {
                            addParticle(mouseX, mouseY);
                        } else {

                        }

                    }

                    return this;
                }

            };

            SpaceGraph.window(new VerletLinkTest(), 1200, 800);
        }

    }

    static void render(VerletPhysics2D physics, GL2 gl) {
        for (VerletParticle2D p : physics.particles) {
            float t = 2 * p.getMass();
            Draw.colorGrays(gl, 0.3f + 0.7f * Util.tanhFast(p.getSpeed()), 0.7f);
            Draw.rect(gl, p.x - t / 2, p.y - t / 2, t, t);
        }
        gl.glLineWidth(2);
        gl.glColor3f(0, 0.5f, 0);
        for (VerletSpring2D s : physics.springs) {
            VerletParticle2D a = s.a, b = s.b;
            Draw.line(gl, a.x, a.y, b.x, b.y);
        }
    }

}