package spacegraph.space2d.shape;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.event.Off;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.util.animate.Animated;
import spacegraph.video.Draw;
import toxi.geom.PointQuadtree;
import toxi.geom.Rect;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.behaviors.AttractionBehavior2D;
import toxi.physics2d.behaviors.GravityBehavior2D;

/**
 * softbody verlet shapes
 */
public class SoftSurface {

    public static class AttractTest2D extends Surface implements Animated {

        final int W = 800;

        private Off update;

        public static void main(String[] args) {
            SpaceGraph.window(new AttractTest2D(), 1200, 800);
        }

        int NUM_PARTICLES = 1500;
        float timeScale = 0.5f;

        VerletPhysics2D physics;

        private Vec2D mousePos = new Vec2D();

        private AttractionBehavior2D mouseAttractor;

        @Override
        protected void starting() {
            physics = new VerletPhysics2D(null, 2, 0);
            physics.setDrag(0.05f);
            physics.setWorldBounds(new Rect(0, 0, W, W));
            physics.addBehavior(new GravityBehavior2D(new Vec2D(0, 0.1f)));
            physics.setIndex(
                    new PointQuadtree(null, 0, 0, W + 1, W + 1)
                    //new RTreeQuadTree()
            );

            update = root().animate(this);
        }

        @Override
        protected void stopping() {
            update.off();
            update = null;
        }

        private void addParticle() {
            VerletParticle2D p = new VerletParticle2D(Vec2D.randomVector().scale(5)
                    .addSelf(W * 0.5f, 0));
            p.setWeight((float) (0.5f + Math.random() * 4f));
            physics.addParticle(p);
            // normally add a negative attraction force field around the new particle
            //but some will be attract
            float str = Math.random() < 0.05f ? +2.2f : -1.2f;
            physics.addBehavior(new AttractionBehavior2D(p, 30, str, 0.01f));
        }


        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {

            for (VerletParticle2D p : physics.particles) {
                float t = 2 * p.getWeight();
                Draw.colorGrays(gl, 0.3f + 0.7f * Util.tanhFast(p.getSpeed()), 0.7f);
                Draw.rect(gl, p.x - t / 2, p.y - t / 2, t, t);
            }
            // Quadtree tree = (Quadtree) physics.getIndex();
            // noFill();
            // stroke(255, 50);
            // tree.prewalk(new QuadtreeVisitor() {
            //
            // public void visitNode(Quadtree node) {
            // gfx.rect(node);
            // }
            // });
//            fill(255);
//            text("fps: " + frameRate, 20, 20);
//            text("count: " + physics.particles.size(), 20, 40);
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
            if (physics.particles.size() < NUM_PARTICLES) {
                addParticle();
            }
            physics.update(dt * timeScale);
            return true;
        }
    }
}
