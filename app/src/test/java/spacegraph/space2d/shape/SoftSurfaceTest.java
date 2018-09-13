package spacegraph.space2d.shape;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.event.Off;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.util.animate.Animated;
import spacegraph.video.Draw;
import toxi.geom.PointQuadtree;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.behaviors.AttractionBehavior2D;
import toxi.physics2d.behaviors.GravityBehavior2D;

import java.util.Random;

class SoftSurfaceTest {

    public static class AttractTest2D extends Surface implements Animated {

        final int W = 800;

        private Off update;

        int NUM_PARTICLES = 1500;
        float timeScale = 1f;

        VerletPhysics2D physics;

        private Vec2D mousePos = new Vec2D();
        final Random rng = new XoRoShiRo128PlusRandom(1);
        private AttractionBehavior2D mouseAttractor;

        @Override
        protected void starting() {

            physics = new VerletPhysics2D(null, 3, 0);
            physics.setDrag(0.02f);
            physics.setWorldBounds(RectFloat2D.X0Y0WH(0, 0, W, W));
            physics.setIndex(
                    new PointQuadtree(null, -1, -1, W + 1, W + 1)
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

        private void addParticle() {
            VerletParticle2D p = new VerletParticle2D(Vec2D.randomVector().scale(5).addSelf(W * 0.5f, 0));
            p.mass((float) (0.5f + Math.random() * 4f));

            float str = Math.random() < 0.05f ? +2.2f : -1.2f;

            AttractionBehavior2D forceField = new AttractionBehavior2D(p, 30, str, 0.01f, rng);
            physics.addBehavior(forceField);

            physics.addParticle(p);
        }


        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {
            renderVerlets(physics, gl);
        }


        @Override
        public Surface finger(Finger finger) {
            if (finger != null) {
                float mouseX = finger.pos.x, mouseY = finger.pos.y;
                mousePos.set(mouseX, mouseY);


                if (finger.pressing(0)) {
                    synchronized (physics) {
                        if (mouseAttractor == null) {
                            mouseAttractor = new AttractionBehavior2D(mousePos, 400, 332f);
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
            physics = new VerletPhysics2D(null, 2, 0);
            physics.setDrag(0.05f);

            physics.setIndex(
                    new PointQuadtree(-1, -1, w+1, h+1)
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
            renderVerlets(physics, gl);
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

    static void renderVerlets(VerletPhysics2D physics, GL2 gl) {
        for (VerletParticle2D p : physics.particles) {
            float t = 2 * p.getMass();
            Draw.colorGrays(gl, 0.3f + 0.7f * Util.tanhFast(p.getSpeed()), 0.7f);
            Draw.rect(gl, p.x - t / 2, p.y - t / 2, t, t);
        }
    }

}