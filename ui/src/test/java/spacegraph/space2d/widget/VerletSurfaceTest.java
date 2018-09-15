package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import jcog.event.Off;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.shape.VerletSurface;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.windo.GraphWall;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.util.animate.Animated;
import toxi.geom.QuadtreeIndex;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.VerletSpring2D;
import toxi.physics2d.behaviors.AttractionBehavior2D;
import toxi.physics2d.behaviors.GravityBehavior2D;

import java.util.Random;

class VerletSurfaceTest {

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
            physics.setBounds(RectFloat2D.X0Y0WH(0, 0, W, W));
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
            VerletSurface.render(physics, gl);
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

//        @Override
//        protected void starting() {
//            super.starting();
//        }

        {
            size(1200, 1200);
            physics.pos(bounds);
//            physics.physics.bounds(bounds);

            Surface a = add(new PushButton("x")).pos(100, 100, 200, 200);
            Surface b = add(new PushButton("y")).pos(300, 300, 400, 400);

            chain(a, b, new PushButton("xy").click((r)->r.parent(Windo.class).detach()));


        }

        public static void main(String[] args) {

            SpaceGraph.window(new VerletLinkTest(), 1200, 800);
        }

    }



}