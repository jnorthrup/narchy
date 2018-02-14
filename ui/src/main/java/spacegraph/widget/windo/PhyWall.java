package spacegraph.widget.windo;

import jcog.event.On;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;
import spacegraph.*;
import spacegraph.input.Fingering;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;
import spacegraph.phys.util.Animated;
import spacegraph.test.WidgetTest;
import spacegraph.widget.button.PushButton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * wall which organizes its sub-surfaces according to 2D phys dynamics
 */
public class PhyWall extends Wall implements Animated {
    final Dynamics2D W;


    private On on;

    public PhyWall() {
        super();
        W = new Dynamics2D(new v2(0, 0));
        W.setParticleRadius(0.2f);
        W.setParticleDensity(1.0f);

        W.setAllowSleep(true);
        W.setSubStepping(true);
    }

    @Override
    public void start(SurfaceBase parent) {
        synchronized (this) {
            super.start(parent);
            on = ((Ortho) root()).onUpdate(this);
        }
    }


    @Override
    public boolean animate(float dt) {

        W.step(dt, 8, 8);


        return true;
    }

    @Override
    public void stop() {
        synchronized (this) {
            on.off();
            on = null;
            super.stop();
        }
    }


    final AtomicInteger i = new AtomicInteger(0);


    public SpatialWindo addWindo(Surface content, RectFloat2D initialBounds) {
        SpatialWindo s = new SpatialWindo("w" + i.getAndIncrement(), initialBounds);
//        objects.put(s.spatial.id, s.spatial);
        add(s);
        s.children(new Scale(content, 1f - Windo.resizeBorder));
        return s;
    }

    final Map<String, SpatialWindo> spatials = new ConcurrentHashMap<>();

    class SpatialWindo extends Windo {
        private final Body body;
        private final PolygonShape shape;
//        public final SimpleSpatial<String> spatial;

        SpatialWindo(String id, RectFloat2D initialBounds) {

            pos(initialBounds);

            this.shape = PolygonShape.box(initialBounds.w, initialBounds.h);

            FixtureDef fd = new FixtureDef(shape, 1f, 0.2f);
            fd.setRestitution(0.4f);

            BodyDef bd = new BodyDef();
            bd.type = BodyType.DYNAMIC;
            //bd.position.set(0,0);
            Body body = new MyBody(bd);
            W.addBody(body);
            body.createFixture(fd);

            this.body = body;

            spatials.put(id, this);

//            this.spatial = new Cuboid(id, w(), h()) {
//
//                @Override
//                public Dynamic newBody(boolean collidesWithOthersLikeThis) {
//                    return new FlatDynamic(mass(), shape, transform, (short) +1, (short) -1);
//                }
//
//                @Override
//                public void update(Dynamics world) {
//
//                    boolean newly = body == null; //HACK
//
//                    super.update(world);
//
//                    if (newly) //HACK
//                        commitPhysics();
//
//                    updated = true;
//                }
//
//            };
        }

//        boolean updated = false;

//        @Override
//        protected void paintWidget(GL2 gl, RectFloat2D bounds) {
//            if (updated && busy.compareAndSet(false, true)) {
//                SimpleBoxShape bs = (SimpleBoxShape) spatial.shape;
//                float w = bs.x();
//                float h = bs.y();
//                float d = bs.z();
//
//                Transform transform = spatial.transform;
//                float px = transform.x;
//                float py = transform.y;
//                transform.x = Util.clamp(px, -spaceBoundsXY / 2 + w / 2, spaceBoundsXY / 2 - w / 2);
//                transform.y = Util.clamp(py, -spaceBoundsXY / 2 + h / 2, spaceBoundsXY / 2 - h / 2);
//                //transform.z = Util.clamp(transform.z, -spaceBoundsZ/2+d/2, spaceBoundsZ/2-d/2);
//
//                Dynamic body = spatial.body;
//                if (!Util.equals(px, transform.x, Surface.EPSILON) || !Util.equals(py, transform.y, Surface.EPSILON)) {
//                    //body.linearVelocity.zero(); //HACK emulates infinite absorption on collision with outer bounds
//                    body.angularVelocity.zero();
//                    body.totalTorque.zero();
//
//                    body.linearVelocity.scale(-0.5f); //bounce but diminish
//                    body.totalForce.scale(-0.5f);
//                    //body.clearForces();
//                }
//                if (hover) {
//                    body.linearVelocity.zero();
//                    body.angularVelocity.zero();
//                    body.clearForces();
//                }
//
//                float x = transform.x;
//                float y = transform.y;
//                //float z = transform.z;
//
//                SpatialWindo.this.pos(x - w / 2, y - h / 2, x + w / 2, y + h / 2);
//                updated = false;
//                busy.set(false);
//            }
//
//            super.paintIt(gl);
//        }

//            spatial.transform.x = cx();
//            spatial.transform.y = cy();
//            float H = h();
//            float W = w();
//            float D = Math.max(W, H);
//            spatial.scale(W, H, D);
//            //(D/ spaceBoundsXY) * spaceBoundsZ * 0.5f);
//
//            if (spatial.body != null) {
//                spatial.body.setDamping(0.8f, 0.9f);
//                spatial.body.setFriction(0.5f);
//                spatial.body.setRestitution(0.5f);
//                float density = 0.1f;
//                spatial.body.setMass(W * H * D * density);
//
//            }


//        @Override
//        public Surface pos(RectFloat2D r) {
//            RectFloat2D b = this.bounds;
//            super.pos(r);
//            if (bounds != b) { //if different
//                layout();
//                commitPhysics();
//            }
//            return null;
//        }

        @Override
        protected Fingering fingering(DragEdit mode) {
            Fingering f = super.fingering(mode);
            if (f != null) {
//                spatial.body.clearForces();
//                spatial.body.angularVelocity.zero();
//                spatial.body.linearVelocity.zero();
            }
            return f;
        }


        private class MyBody extends Body {

            RectFloat2D physBounds = null;


            public MyBody(BodyDef bd) {
                super(bd, PhyWall.this.W);
                setFixedRotation(true);
            }


            @Override
            public void preUpdate() {


                RectFloat2D r = bounds;
                if (physBounds == null || bounds!=physBounds) {

                    //boolean change = false;
                    if (physBounds == null || (r.w != physBounds.w) || (r.h != physBounds.h)) {
                        shape.setAsBox(r.w/2, r.h/2);
                        physBounds = bounds;
                        setAwake(true);
                    }

//                    Tuple2f p = getWorldCenter();
//                    float px = p.x;
//                    float py = p.y;
                    //setLinearVelocity(new v2(r.x - px, r.y - py));
                    if (setTransform(new v2(r.cx(), r.cy()), 0))
                        setAwake(true);
//                    if (m_sweep.c.setIfChanged(r.cx(), r.cy(), Settings.EPSILON)) {
//                        //setLinearVelocity(new v2(0,0));
//                        setAwake(true);
//                    }

                }


            }

            @Override
            public void postUpdate() {

                Tuple2f p =
                    getWorldCenter();
                    //getPosition();

                float w = w(), h = h(); //HACK re-use the known width/height assumes that the physics engine cant change the shape's size

                pos(physBounds = RectFloat2D.XYWH(p.x, p.y, w, h));

            }
        }
    }

    public static void main(String[] args) {
        PhyWall d = new PhyWall();

        SpaceGraph.window(d, 800, 800);

        //d.children.add(new GridTex(16).pos(0,0,1000,1000));

        {
            d.addWindo(WidgetTest.widgetDemo(), RectFloat2D.XYWH(-50, 0, 150, 120));
            d.addWindo(WidgetTest.widgetDemo(), RectFloat2D.XYWH(+100, 0, 100, 100));
//            Windo.Port p = w.addPort("X");
        }

//        d.addWindo(grid(new PushButton("x"), new PushButton("y"))).pos(10, 10, 50, 50);

        for (int i = 0; i < 8; i++) {
            float rx = (float) (Math.random() * 1000f / 2);
            float ry = (float) (Math.random() * 1000f / 2);
            float rw = 55 + 150 * (float) Math.random();
            float rh = 50 + 150 * (float) Math.random();
            d.addWindo(new PushButton("w" + i), RectFloat2D.XYWH(rx, ry, rw, rh));
        }

        //d.newWindo(grid(new PushButton("x"), new PushButton("y"))).pos(-100, -100, 0, 0);


    }


//    class Boundary extends SimpleSpatial<String> {
//
//        //final Dynamic b;
//        float cx, cy, cz;
//
//        public Boundary(float x1, float y1, float z1, float x2, float y2, float z2) {
//            super(UUID.randomUUID().toString() /* HACK */);
//            scale(x2 - x1, y2 - y1, z2 - z1);
//            this.cx = (x1 + x2) / 2;
//            this.cy = (y1 + y2) / 2;
//            this.cz = (z1 + z2) / 2;
//
//
////            b = dyn.newBody(0,
////                    new SimpleBoxShape(,
////                    new Transform(,
////                    +1, -1);
////            b.setData(this);
//        }
//
//        @Override
//        public void update(Dynamics world) {
//            move(cx, cy, cz);
//            super.update(world);
//            System.out.println(transform + " " + shape);
//        }
//
//        @Override
//        public float mass() {
//            return 10000;
//        }
//
//        //        @Override
////        public void forEachBody(Consumer<Collidable> c) {
////            c.accept(b);
////        }
////
////        @Nullable
////        @Override
////        public List<TypedConstraint> constraints() {
////            return null;
////        }
////
////        @Override
////        public void renderAbsolute(GL2 gl, long timeMS) {
////
////        }
////
////        @Override
////        public void renderRelative(GL2 gl, Collidable body) {
////
////        }
////
////        @Override
////        public float radius() {
////            return 0;
////        }
//    }
}
