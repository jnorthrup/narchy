package nars.rover.robot;


import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.util.Bodies;
import nars.rover.util.Explosion;
import nars.util.concept.random.XORShiftRandom;
import spacegraph.space2d.phys.common.Color3f;
import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.dynamics.Body;
import spacegraph.space2d.phys.dynamics.BodyType;
import spacegraph.space2d.phys.dynamics.contacts.Contact;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Turret extends Robotic {

    final static Random rng = new XORShiftRandom();

    final float fireProbability = 0.005f;

    public Turret(String id) {
        super(id);
    }

    @Override
    public RoboticMaterial getMaterial() {

        return new RoboticMaterial(this) {

            @Override public void before(Body b, JoglAbstractDraw d, float time) {
                super.before(b, d, time);

                if (!explosions.isEmpty()) {
                    Iterator<BulletData> ii = explosions.iterator();
                    while (ii.hasNext()) {
                        BulletData bd = ii.next();
                        if (bd.explosionTTL-- <= 0)
                            ii.remove();


                        d.drawSolidCircle(bd.getCenter(), bd.explosionTTL/8 +  rng.nextFloat() * 4, new Vec2(),
                                new Color3f(1 - rng.nextFloat()/3f,
                                            0.8f - rng.nextFloat()/3f,
                                            0f));
                    }
                }
            }
        };
    }

    @Override
    protected Body newTorso() {

        return sim.create(new Vec2(), Bodies.rectangle(new Vec2(4, 1)), BodyType.DYNAMIC);
    }



    @Override
    public void step(int i) {
        super.step(i);


        for (Body b : removedBullets) {
            bullets.remove(b);
            sim.remove(b);

            final BulletData bd = (BulletData) b.getUserData();
            bd.explode();
            explosions.add(bd);
        }
        removedBullets.clear();

        if (Math.random() < fireProbability) {
            fireBullet();
        }
    }

    final int maxBullets = 16;
    final Deque<Body> bullets = new ArrayDeque(maxBullets);
    final Deque<Body> removedBullets = new ArrayDeque(maxBullets);
    final Collection<BulletData> explosions = new ConcurrentLinkedQueue();

    public void fireBullet(/*float ttl*/) {









        final float speed = 100f;


        if (bullets.size() >= maxBullets) {
            sim.remove( bullets.removeFirst() );
        }


        Vec2 start = torso.getWorldPoint(new Vec2(6.5f, 0));
        Body b = sim.create(start, Bodies.rectangle(0.4f, 0.6f), BodyType.DYNAMIC);
        b.m_mass= 0.05f;

        float angle = torso.getAngle();
        Vec2 rayDir = new Vec2( (float)Math.cos(angle), (float)Math.sin(angle) );
        rayDir.mulLocal(speed);


        
        b.setUserData(new BulletData(b, 0));
        bullets.add(b);

        b.applyForce(rayDir, new Vec2(0,0));
























    }

    public class BulletData implements Collidable {
        private final float diesAt;
        private final Body bullet;
        public int explosionTTL;


        public BulletData(Body b, float diesAt) {
            this.bullet = b;
            this.diesAt = diesAt;
        }

        public void explode() {
            
            float force = 175f;
            Explosion.explodeBlastRadius(bullet.getWorld(), bullet.getWorldCenter(), 160f,force);
            explosionTTL = (int)force/2;
        }

        public Vec2 getCenter() { return bullet.getWorldCenter(); }

        @Override public void onCollision(Contact c) {
            
            removedBullets.add(bullet);
        }
    }

}
