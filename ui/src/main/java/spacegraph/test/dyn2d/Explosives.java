package spacegraph.test.dyn2d;

import com.jogamp.opengl.GL2;
import jcog.exe.Loop;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.contacts.Contact;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;
import spacegraph.render.Draw;
import spacegraph.widget.windo.PhyWall;

import java.util.function.Consumer;

import static spacegraph.test.dyn2d.TensorGlow.rng;
import static spacegraph.test.dyn2d.TensorGlow.staticBox;

public class Explosives {

    /**
     * TODO bullet hard TTL in case it goes off to infinity
     */
    static class Gun {

        protected final Body2D barrel;
        float barrelLength;
        float barrelThick;

        /**
         * in radians; more inaccuracy = more randomized direction spraying
         */
        float inaccuracy = 0.03f;


        long lastFire;
        long minFirePeriodMS = 200;

        public Gun(float barrelLength, Dynamics2D world) {
            this.barrelLength = barrelLength;
            this.barrelThick = barrelLength * 0.2f;
            this.lastFire = world.realtimeMS;
            barrel = world.addBody(new BodyDef(BodyType.DYNAMIC),
                    new FixtureDef(PolygonShape.box(barrelLength / 2, barrelThick / 2), 0.1f, 0f));

        }


        public void fire() {


            if (barrel.W.realtimeMS - lastFire < minFirePeriodMS) {
                return; //too soon
            } else {
                this.lastFire = barrel.W.realtimeMS;
            }

            float bulletLength = barrelThick * 2f;
//                Tuple2f barrelTip = //barrel.getWorldPoint(new v2(barrel., 0));
//                        barrel.getWorldVector(new v2(barrelLength+bulletLength+0.01f, 0));

            float heading = barrel.angle() + 2 * (rng.nextFloat() - 0.5f) * inaccuracy;
            v2 direction = new v2((float) Math.cos(heading), (float) Math.sin(heading));

            float power = barrelThick;

            float bulletThick = barrelThick / 2 * 0.25f;

            PolygonShape pos = PolygonShape.box(bulletLength / 2, bulletThick);
            Body2D projectile = new Projectile(barrel.W);

            projectile.setBullet(true);

            FixtureDef ff = new FixtureDef(pos, 0.01f, 0f);
            ff.restitution = 0.9f;

            barrel.W.addBody(projectile, ff);




            projectile.setTransform(barrel.pos.add(direction.scaled((barrelLength / 2f) + bulletLength)), heading);
            //projectile.set((Rot)barrel);
            //projectile.applyLinearImpulse(direction.scaled(power), new v2(), true);

            {
                //propulsion
                projectile.applyForceToCenter(direction.scaled(power));

                //recoil
                barrel.applyForceToCenter(direction.scaled(-1));
            }

            //      projectile.setLinearVelocity(direction.scaled(power*70f));

        }

    }

    public static class Projectile extends Body2D implements Consumer<GL2> {
        long start, end;

        public Projectile(Dynamics2D w) {
            super(BodyType.DYNAMIC, w);

            start = System.currentTimeMillis();
            long ttl = 2 * 1000;
            end = start + ttl;
        }

        @Override
        public boolean preUpdate() {
            if (W.realtimeMS > end) {
                return false;
            }
            return true;
        }

        @Override
        protected void onRemoval() {
            W.invokeLater(() -> {
                int blasts = 1;
                float bulletRadius = 0.2f; //fixtures.shape().radius * 2f; //<- not working
                float blastScatter = bulletRadius * (blasts - 1);
                float blastRadius = bulletRadius;
                for (int i = 0; i < blasts; i++) {
                    W.addBody(new Fireball(W, pos.add(
                            new v2((float) rng.nextGaussian() * blastScatter, (float) rng.nextGaussian() * blastScatter)),
                            blastRadius));
                }
            });
        }

        @Override
        public void accept(GL2 gl) {
            Fixture f = fixtures;
            if (f != null) {
                gl.glColor3f(0.75f, 0.75f, 0.75f);
                Draw.poly(this, gl, (PolygonShape) f.shape);
            }
        }
    }

    /**
     * expanding shockwave, visible
     */
    public static class Fireball extends Body2D implements Consumer<GL2> {

        private final float maxRad;
        private final CircleShape shape;
        private float rad;

        public Fireball(Dynamics2D w, Tuple2f center, float maxRad) {
            super(new BodyDef(
                    BodyType.KINEMATIC
                    //BodyType.DYNAMIC
            ), w);

            this.maxRad = maxRad;

            shape = new CircleShape();
            rad = shape.radius = 0.05f; //initial

            w.addBody(this,
                    new FixtureDef(shape, 0.001f, 0.1f));
            this.setTransform(center, 0);
        }

        @Override
        public boolean preUpdate() {
            if (rad < maxRad) {
                Fireball.this.rad *= 1.2f;
                //W.invokeLater(() -> {
                    updateFixtures((f) -> {
                        shape.radius = rad;
                        f.setShape(shape);
                    });
                //});
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void accept(GL2 gl) {

            CircleShape circle = shape;
            float r = circle.radius;
            v2 v = new v2();
            getWorldPointToOut(circle.center, v);
            //Point p = getPoint(v);
            //int wr = (int) (r * zoom);
            //g.fillOval(p.x - wr, p.y - wr, wr * 2, wr * 2);
            Draw.colorUnipolarHue(gl, rng.nextFloat(), 0.1f, 0.3f, 0.8f);
            Draw.circle(gl, v, true, r, 9);

        }
    }


    public static void main(String[] args) {
        PhyWall p = PhyWall.window(1200, 1000);

        Dynamics2D w = p.W;
        w.setContactListener(new ExplosionContacts());

        //w.setGravity(new v2(0, -2.8f));

        staticBox(w, -8, -4, 8, 4);


        Gun g = new Gun(1f, w);
        Loop.of(() -> g.fire()).runFPS(10f);


    }

    public static class ExplosionContacts implements ContactListener {

        protected void explode(Body2D b, Body2D hit) {




            b.remove();

        }

        @Override
        public boolean beginContact(Contact contact) {
            Body2D a = contact.aFixture.body;
            Body2D b = contact.bFixture.body;
            //TODO use proper collision group filtering
            if (a instanceof Projectile) { // && !(b instanceof Projectile)) { //HACK TODO use an explosive callback tag
                if (b instanceof Fireball || b instanceof Projectile) {
                    //TODO contribute to existing explosion
                    return false;
                }
                explode(a, b);
            }
            if (b instanceof Projectile) { // && !(a instanceof Projectile)) {
                if (a instanceof Fireball || a instanceof Projectile) {
                    //TODO contribute to existing explosion
                    return false;
                }
                explode(b, a);
            }
            return true;
        }

        @Override
        public void endContact(Contact contact) {

        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {

        }

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {

        }
    }
}
