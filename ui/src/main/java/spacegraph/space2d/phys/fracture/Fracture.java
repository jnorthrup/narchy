package spacegraph.space2d.phys.fracture;

import jcog.list.FasterList;
import spacegraph.space2d.phys.callbacks.ContactImpulse;
import spacegraph.space2d.phys.collision.WorldManifold;
import spacegraph.space2d.phys.collision.shapes.CircleShape;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.collision.shapes.Shape;
import spacegraph.space2d.phys.common.Transform;
import spacegraph.space2d.phys.dynamics.*;
import spacegraph.space2d.phys.dynamics.contacts.Contact;
import spacegraph.space2d.phys.fracture.fragmentation.Smasher;
import spacegraph.space2d.phys.fracture.util.MyList;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.List;
import java.util.Objects;

import static spacegraph.space2d.phys.fracture.Material.CIRCLEVERTICES;

/**
 * Objekt, ktory reprezentuje rozbitie jedneho telesa.
 *
 * @author Marek Benovic
 */
public final class Fracture {
    /**
     * Normal Impulse, ktory vyvolal frakturu
     */
    public final float normalImpulse;

    private final Fixture f1; 
    private final Fixture f2; 
    private final Body2D b1; 
    private final Body2D b2; 
    private final Material m; 
    private final Tuple2f point; 
    private final Contact contact; 

    /**
     * Vytvori Frakturu. Ta este nieje aplikovana na svet.
     */
    private Fracture(Fixture f1, Fixture f2, Material m, Contact contact, float normalImpulse, Tuple2f point) {
        this.f1 = f1;
        this.f2 = f2;
        this.b1 = f1.body;
        this.b2 = f2.body;
        this.m = m;
        this.point = point;
        this.contact = contact;
        this.normalImpulse = normalImpulse;
    }

    /**
     * Rozbije objekt. Upravi objekt world tak, ze vymaze triesteny objekt
     * a nahradi ho fragmentami na zaklade nastaveneho materialu a clenskych
     * premennych.
     *
     * @param dt casova dlzka framu
     */
    public void smash(Smasher smasher, float dt) {
        Shape s = f1.shape;
        if (s == null)
            return;

        if (contact == null) { 
            b1.setType(BodyType.DYNAMIC);
            return;
        }

        Dynamics2D w = b1.W;
        Polygon p = f1.polygon;

        if (p == null) {
            switch (s.m_type) {
                case POLYGON:
                    PolygonShape ps = (PolygonShape) s;
                    Tuple2f[] vertices = ps.vertex;
                    int n = ps.vertices;
                    p = new Polygon(n);
                    for (int i = 0; i < n; ++i) {
                        p.add(vertices[n - i - 1]);
                    }
                    break;
                case CIRCLE:
                    CircleShape cs = (CircleShape) s;
                    p = new Polygon(CIRCLEVERTICES);
                    float radius = cs.radius;

                    double u = Math.PI * 2 / CIRCLEVERTICES;
                    radius = (float) Math.sqrt(u / Math.sin(u)) * radius; 

                    Tuple2f center = cs.center;
                    for (int i = 0; i < CIRCLEVERTICES; ++i) {
                        double j = u * i; 
                        float sin = (float) Math.sin(j);
                        float cos = (float) Math.cos(j);
                        Tuple2f v = new v2(sin, cos).scaled(radius).added(center);
                        p.add(v);
                    }
                    break;
                default:
                    throw new RuntimeException("Dany typ tvaru nepodporuje stiepenie");
            }
        }

        float mConst = f1.material.m_rigidity / normalImpulse; 

        boolean fixA = f1 == contact.aFixture; 
        float oldAngularVelocity = fixA ? contact.m_angularVelocity_bodyA : contact.m_angularVelocity_bodyB;
        Tuple2f oldLinearVelocity = fixA ? contact.m_linearVelocity_bodyA : contact.m_linearVelocity_bodyB;
        b1.setAngularVelocity((b1.velAngular - oldAngularVelocity) * mConst + oldAngularVelocity);
        b1.setLinearVelocity(b1.vel.sub(oldLinearVelocity).scaled(mConst).added(oldLinearVelocity));
        if (!w.isFractured(f2) && b2.type == BodyType.DYNAMIC && !b2.m_fractureTransformUpdate) { 
            oldAngularVelocity = !fixA ? contact.m_angularVelocity_bodyA : contact.m_angularVelocity_bodyB;
            oldLinearVelocity = !fixA ? contact.m_linearVelocity_bodyA : contact.m_linearVelocity_bodyB;
            b2.setAngularVelocity((b2.velAngular - oldAngularVelocity) * mConst + oldAngularVelocity);
            b2.setLinearVelocity(b2.vel.sub(oldLinearVelocity).scaled(mConst).added(oldLinearVelocity));
            b2.setTransform(
                    b2.transformPrev.pos.add(b2.vel.scale(dt)),
                    b2.transformPrev.angle()
            ); 
            b2.m_fractureTransformUpdate = true;
        }

        Tuple2f localPoint = Transform.mulTrans(b1, point);
        Tuple2f b1Vec = b1.getLinearVelocityFromWorldPoint(point);
        Tuple2f b2Vec = b2.getLinearVelocityFromWorldPoint(point);
        Tuple2f localVelocity = b2Vec.subbed(b1Vec);

        localVelocity.scaled(dt);

        Polygon[] fragment = m.split(smasher, p, localPoint, localVelocity, normalImpulse); 
        if (fragment.length <= 1) { 
            return;
        }

        
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(b1.pos); 
        bodyDef.angle = b1.angle(); 
        bodyDef.fixedRotation = b1.isFixedRotation();
        bodyDef.angularDamping = b1.m_angularDamping;
        bodyDef.allowSleep = b1.isSleepingAllowed();

        FixtureDef fd = new FixtureDef();
        fd.friction = f1.friction; 
        fd.restitution = f1.restitution; 
        fd.isSensor = f1.isSensor;
        fd.density = f1.density;

        
        List<Fixture> fixtures = new FasterList<>();
        if (f1.polygon != null) {
            for (Fixture f = b1.fixtures; f != null; f = f.next) {
                if (f.polygon == f1.polygon) {
                    fixtures.add(f);
                }
            }
        } else {
            fixtures.add(f1);
        }

        for (Fixture f : fixtures) {
            b1.removeFixture(f);
        }

        if (b1.fixtureCount == 0) {
            w.removeBody(b1);
        }

        
        MyList<Body2D> newbodies = new MyList<>();
        for (Polygon pg : fragment) { 
            if (pg.isCorrect()) {
                if (pg instanceof Fragment) {
                    Polygon[] convex = pg.convexDecomposition();
                    bodyDef.type = BodyType.DYNAMIC;
                    for (Polygon pgx : convex) {
                        Body2D f_body = w.addBody(bodyDef);
                        pgx.flip();
                        PolygonShape ps = new PolygonShape();
                        ps.set(pgx.getArray(), pgx.size());
                        fd.shape = ps;
                        fd.polygon = null;
                        fd.material = f1.material;
                                

                        f_body.addFixture(fd);
                        f_body.setAngularVelocity(b1.velAngular);
                        f_body.setLinearVelocity(b1.getLinearVelocityFromLocalPoint(f_body.getLocalCenter()));
                        newbodies.add(f_body);
                    }

                } else {
                    fd.material =
                            f1.material;
                    bodyDef.type = b1.getType();
                    Body2D f_body = w.addBody(bodyDef);
                    PolygonFixture pf = new PolygonFixture(pg);

                    f_body.addFixture(pf, fd);
                    f_body.setLinearVelocity(b1.getLinearVelocityFromLocalPoint(f_body.getLocalCenter()));
                    f_body.setAngularVelocity(b1.velAngular);
                    newbodies.add(f_body);
                }
            }
        }

        
        FractureListener fl = w.getContactManager().m_fractureListener;
        if (fl != null) {
            fl.action(m, normalImpulse, newbodies);
        }
    }

    /**
     * Detekuje, ci dany kontakt vytvara frakturu
     *
     * @param contact
     * @param impulse
     * @param w
     */
    public static void init(Contact contact, ContactImpulse impulse, Dynamics2D w) {
        Fixture f1 = contact.aFixture;
        Fixture f2 = contact.bFixture;
        float[] impulses = impulse.normalImpulses;
        for (int i = 0; i < impulse.count; ++i) {
            float iml = impulses[i];
            fractureCheck(f1, f2, iml, w, contact, i);
            fractureCheck(f2, f1, iml, w, contact, i);
        }
    }








    private static void fractureCheck(final Fixture f1, final Fixture f2, final float iml, Dynamics2D w, Contact contact, int i) {





            Material m = f1.material;
            if (m!=null && m.m_rigidity < iml) {
                f1.body.m_fractureTransformUpdate = f2.body.m_fractureTransformUpdate = false;
                if (f1.body.m_massArea > Material.MINMASSDESCTRUCTION) {
                    WorldManifold wm = new WorldManifold();
                    contact.getWorldManifold(wm); 
                    w.addFracture(new Fracture(f1, f2, m, contact, iml, new v2(wm.points[i])));
                } else if (f1.body.type != BodyType.DYNAMIC) {
                    w.addFracture(new Fracture(f1, f2, m, null, 0, null));
                }
            }


    }

    private static boolean equals(Fixture f1, Fixture f2) {
        PolygonFixture p1 = f1.polygon;
        PolygonFixture p2 = f2.polygon;
        if (p1 != null && p2 != null) {
            return p1 == p2;
        } else {
            return f1 == f2;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Fracture) {
            Fracture f = (Fracture) obj;
            return equals(f.f1, f1);
        } else if (obj instanceof Fixture) {
            Fixture f = (Fixture) obj;
            return equals(f, f1);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.requireNonNullElse(f1.polygon, f1).hashCode();
    }
}
