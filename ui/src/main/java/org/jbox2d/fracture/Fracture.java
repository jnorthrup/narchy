package org.jbox2d.fracture;

import jcog.list.FasterList;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.collision.WorldManifold;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Transform;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.fracture.fragmentation.Smasher;
import org.jbox2d.fracture.util.MyList;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

import java.util.List;

import static org.jbox2d.fracture.Material.CIRCLEVERTICES;

/**
 * Objekt, ktory reprezentuje rozbitie jedneho telesa.
 *
 * @author Marek Benovic
 */
public class Fracture {
    /**
     * Normal Impulse, ktory vyvolal frakturu
     */
    public final float normalImpulse;

    private final Fixture f1; //primarna fixture, ktora sa rozbija
    private final Fixture f2; //sekundarna fixture
    private final Body2D b1; //telo fixtury f1
    private final Body2D b2; //telo fuxtury f2
    private final Material m; //material triestiaceho sa telesa
    private final Tuple2f point; //kolizny bod vo worldCoordinates
    private final Contact contact; //kontakt, z ktoreho vznika fraktura

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

        if (contact == null) { //riesi sa staticky prvok, ktory ma priliz maly obsah
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
                    p = new Polygon();
                    for (int i = 0; i < ps.vertices; ++i) {
                        p.add(vertices[ps.vertices - i - 1]);
                    }
                    break;
                case CIRCLE:
                    CircleShape cs = (CircleShape) s;
                    p = new Polygon();
                    float radius = cs.radius;

                    double u = Math.PI * 2 / CIRCLEVERTICES;
                    radius = (float) Math.sqrt(u / Math.sin(u)) * radius; //upravim radius tak, aby bola zachovana velkost obsahu

                    Tuple2f center = cs.center;
                    for (int i = 0; i < CIRCLEVERTICES; ++i) {
                        double j = u * i; //uhol
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

        float mConst = f1.material.m_rigidity / normalImpulse; //sila v zavislosti na pevnosti telesa

        boolean fixA = f1 == contact.aFixture; //true, ak f2 je v objekte contact ako m_fixtureA
        float oldAngularVelocity = fixA ? contact.m_angularVelocity_bodyA : contact.m_angularVelocity_bodyB;
        Tuple2f oldLinearVelocity = fixA ? contact.m_linearVelocity_bodyA : contact.m_linearVelocity_bodyB;
        b1.setAngularVelocity((b1.velAngular - oldAngularVelocity) * mConst + oldAngularVelocity);
        b1.setLinearVelocity(b1.vel.sub(oldLinearVelocity).scaled(mConst).added(oldLinearVelocity));
        if (!w.isFractured(f2) && b2.type == BodyType.DYNAMIC && !b2.m_fractureTransformUpdate) { //ak sa druhy objekt nerozbija, tak sa jej nahodia povodne hodnoty (TREBA MODIFIKOVAT POHYB OBJEKTU, KTORY SPOSOBUJE ROZPAD)
            oldAngularVelocity = !fixA ? contact.m_angularVelocity_bodyA : contact.m_angularVelocity_bodyB;
            oldLinearVelocity = !fixA ? contact.m_linearVelocity_bodyA : contact.m_linearVelocity_bodyB;
            b2.setAngularVelocity((b2.velAngular - oldAngularVelocity) * mConst + oldAngularVelocity);
            b2.setLinearVelocity(b2.vel.sub(oldLinearVelocity).scaled(mConst).added(oldLinearVelocity));
            b2.setTransform(
                    b2.transformPrev.pos.add(b2.vel.scale(dt)),
                    b2.transformPrev.angle()
            ); //osetruje jbox2d od posuvania telesa pri rieseni kolizie
            b2.m_fractureTransformUpdate = true;
        }

        Tuple2f localPoint = Transform.mulTrans(b1, point);
        Tuple2f b1Vec = b1.getLinearVelocityFromWorldPoint(point);
        Tuple2f b2Vec = b2.getLinearVelocityFromWorldPoint(point);
        Tuple2f localVelocity = b2Vec.subbed(b1Vec);

        localVelocity.scaled(dt);

        Polygon[] fragment = m.split(smasher, p, localPoint, localVelocity, normalImpulse); //rodeli to
        if (fragment.length <= 1) { //nerozbilo to na ziadne fragmenty
            return;
        }

        //definuje tela fragmentov - tie maju vsetky rovnaku definiciu (preberaju parametre z povodneho objektu)
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(b1.pos); //pozicia
        bodyDef.angle = b1.angle(); // otocenie
        bodyDef.fixedRotation = b1.isFixedRotation();
        bodyDef.angularDamping = b1.m_angularDamping;
        bodyDef.allowSleep = b1.isSleepingAllowed();

        FixtureDef fd = new FixtureDef();
        fd.friction = f1.friction; // trenie
        fd.restitution = f1.restitution; //odrazivost
        fd.isSensor = f1.isSensor;
        fd.density = f1.density;

        //odstrani fragmentacne predmety/cele teleso
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

        //prida fragmenty do simulacie
        MyList<Body2D> newbodies = new MyList<>();
        for (Polygon pg : fragment) { //vytvori tela, prida fixtury, poriesi konvexnu dekompoziciu
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
                                //.m_fragments; //rekurzivne stiepenie

                        f_body.addFixture(fd);
                        f_body.setAngularVelocity(b1.velAngular);
                        f_body.setLinearVelocity(b1.getLinearVelocityFromLocalPoint(f_body.getLocalCenter()));
                        newbodies.add(f_body);
                    }

                } else {
                    fd.material =
                            f1.material;//.m_fragments; //rekurzivne stiepenie
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

        //zavola sa funkcia z fraction listeneru (pokial je nadefinovany)
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

//    /**
//     * Kontrolue, ci je kolizia kriticka, ak je, tak ju prida do hashovacej tabulky
//     * kritickych kolizii. Treba zauvazit aj multimaterialove telesa. Materialy
//     * tvoria spojovy zoznam, pre triestenie sa vsak pouzije len jeden
//     */
//    private static final MyList<Material> materials = new MyList<>();

    private static void fractureCheck(final Fixture f1, final Fixture f2, final float iml, Dynamics2D w, Contact contact, int i) {
//        materials.clear();
//        for (Material m = f1.material; m != null; m = m.m_fragments) {
//            if (materials.contains(m)) {
//                return;
//            }
            Material m = f1.material;
            if (m!=null && m.m_rigidity < iml) {
                f1.body.m_fractureTransformUpdate = f2.body.m_fractureTransformUpdate = false;
                if (f1.body.m_massArea > Material.MINMASSDESCTRUCTION) {
                    WorldManifold wm = new WorldManifold();
                    contact.getWorldManifold(wm); //vola sa iba raz
                    w.addFracture(new Fracture(f1, f2, m, contact, iml, new v2(wm.points[i])));
                } else if (f1.body.type != BodyType.DYNAMIC) {
                    w.addFracture(new Fracture(f1, f2, m, null, 0, null));
                }
            }
//            materials.add(m);
//        }
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
        if (f1.polygon != null) {
            return f1.polygon.hashCode();
        } else {
            return f1.hashCode();
        }
    }
}
