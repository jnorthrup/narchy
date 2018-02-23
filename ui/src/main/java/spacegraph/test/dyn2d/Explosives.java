package spacegraph.test.dyn2d;

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
import spacegraph.widget.windo.PhyWall;

import static spacegraph.test.dyn2d.TensorGlow.staticBox;

public class Explosives {

    /**
     * TODO bullet hard TTL in case it goes off to infinity
     */
    static class Gun {

        private final Body2D barrel;
        float barrelLength = 1.1f;
        float barrelThick = 0.07f;

        public Gun(Dynamics2D world) {

            barrel = world.addBody(new BodyDef(BodyType.DYNAMIC),
                    new FixtureDef(PolygonShape.box(barrelLength / 2, barrelThick / 2), 0.1f, 0f));

        }

        public void fire() {

            barrel.W.invoke(() -> {

                float bulletLength = 0.1f;
//                Tuple2f barrelTip = //barrel.getWorldPoint(new v2(barrel., 0));
//                        barrel.getWorldVector(new v2(barrelLength+bulletLength+0.01f, 0));
                v2 direction = new v2(barrel.c, barrel.s);

                float power = 0.15f;

                PolygonShape pos = PolygonShape.box(bulletLength / 2, barrelThick / 2 * 0.9f);
                Body2D projectile = new Body2D(BodyType.DYNAMIC, barrel.W) {
                    @Override
                    protected void onRemoval() {
                        W.invokeLater(() -> {
                            W.addBody(new Fireball(W, pos, 1f).body);
                        });
                    }
                };
                barrel.W.addBody(projectile, new FixtureDef(pos, 0.1f, 0f));


                projectile.setBullet(true);

                projectile.setTransform(barrel.pos.add(direction.scaled(barrelLength * (0.5f + 0.1f))), barrel.angle());
                //projectile.set((Rot)barrel);
                //projectile.applyLinearImpulse(direction.scaled(power), new v2(), true);
                projectile.applyForceToCenter(direction.scaled(power));

            });
        }
    }

    /**
     * expanding shockwave, visible
     */
    public static class Fireball {

        private final Body2D body;
        private float rad;

        public Fireball(Dynamics2D w, Tuple2f center, float maxRad) {
            CircleShape shape = new CircleShape();
            rad = shape.radius = 0.1f; //initial

            body = new Body2D(new BodyDef(BodyType.KINEMATIC), w) {
                @Override
                public void preUpdate() {
                    super.preUpdate();
                    if (rad < maxRad) {
                        Fireball.this.rad *= 1.1f;
                        w.invokeLater(() -> {
                            updateFixtures((f) -> {
                                shape.radius = rad;
                                f.setShape(shape);
                            });
                        });
                    } else {
                        //end
                        W.invokeLater(()->{
                            remove();
                        });
                    }
                }
            };
            w.addBody(body,
                    new FixtureDef(shape, 0f, 0.1f));
            this.body.setTransform(center, 0);

        }
    }

    public static void main(String[] args) {
        PhyWall p = PhyWall.window(1200, 1000);

        Dynamics2D w = p.W;
        w.setContactListener(new ContactListener() {

            protected void explode(Body2D b) {

                w.invokeLater(() -> {
                    b.remove();
                });
            }

            @Override
            public void beginContact(Contact contact) {

            }

            @Override
            public void endContact(Contact contact) {

            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {
                if (contact.aFixture.body.isBullet()) { //HACK TODO use an explosive callback tag
                    explode(contact.aFixture.body);
                }
                if (contact.bFixture.body.isBullet()) {
                    explode(contact.bFixture.body);
                }
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {

            }
        });

        //w.setGravity(new v2(0, -2.8f));

        staticBox(w, -8, -4, 8, 4);


        Gun g = new Gun(w);
        Loop.of(() -> g.fire()).runFPS(4f);


    }
}
