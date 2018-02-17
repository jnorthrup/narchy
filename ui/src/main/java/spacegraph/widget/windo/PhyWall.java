package spacegraph.widget.windo;

import jcog.Util;
import jcog.event.On;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Transform;
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
    public static final float SHAPE_SIZE_EPSILON = 0.001f;
    final Dynamics2D W;


    private On on;

    public PhyWall() {
        super();
        W = new Dynamics2D(new v2(0, 0));
        W.setParticleRadius(0.2f);
        W.setParticleDensity(1.0f);

        W.setAllowSleep(true);
        //W.setSubStepping(true);
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

        W.step(dt*8, 8, 8);


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


    public SpatialWindo window(Surface content, RectFloat2D initialBounds) {
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

            this.shape = PolygonShape.box(initialBounds.w/2, initialBounds.h/2);

            FixtureDef fd = new FixtureDef(shape, 0.1f, 0f);
            fd.setRestitution(0f);

            BodyDef bd = new BodyDef();
            bd.type = BodyType.DYNAMIC;
            //bd.position.set(0,0);
            Body body = new MyBody(bd);
            W.addBody(body);
            body.addFixture(fd);

            this.body = body;

            spatials.put(id, this);

        }


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
                    if (physBounds == null)
                        physBounds = bounds;
                    if ((r.w != physBounds.w) || (r.h != physBounds.h)) {

                        if (!Util.equals(r.w, physBounds.w, SHAPE_SIZE_EPSILON) ||
                            !Util.equals(r.h, physBounds.h, SHAPE_SIZE_EPSILON)) {
                            updateFixtures((f) -> {
                                //HACK assumes the first is the only one
                                //if (f.m_shape == shape) {
                                    f.setShape(shape.setAsBox(r.w / 2, r.h / 2));
                                //}


                            });
                        }


                    }

//                    Tuple2f p = getWorldCenter();
//                    float px = p.x;
//                    float py = p.y;
                    //setLinearVelocity(new v2(r.x - px, r.y - py));
                    v2 target = new v2(r.cx(), r.cy());
//                    target.sub(p.x, p.y);
//                    target.normalize();
//
//                    float updateSpeed = 10f;
//                    target.scale(updateSpeed);
//
//                    applyLinearImpulse(target, p, true);

                    if (setTransform(target, 0))
                       setAwake(true);
////                    if (m_sweep.c.setIfChanged(r.cx(), r.cy(), Settings.EPSILON)) {
////                        //setLinearVelocity(new v2(0,0));
////                        setAwake(true);
////                    }

                }


            }

            @Override
            public void postUpdate() {


                Transform t = getTransform();
                Tuple2f p = t.p;
                //float rot = t.q.getAngle();

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
            d.window(WidgetTest.widgetDemo(), RectFloat2D.XYWH(-250, 0, 150, 320));
            d.window(WidgetTest.widgetDemo(), RectFloat2D.XYWH(+400, 0, 300, 100));
//            Windo.Port p = w.addPort("X");
        }

//        d.addWindo(grid(new PushButton("x"), new PushButton("y"))).pos(10, 10, 50, 50);

        for (int i = 0; i < 8; i++) {
            float rx = (float) (Math.random() * 1000f / 2);
            float ry = (float) (Math.random() * 1000f / 2);
            float rw = 55 + 150 * (float) Math.random();
            float rh = 50 + 150 * (float) Math.random();
            d.window(new PushButton( String.valueOf((char)('w' + i)) ), RectFloat2D.XYWH(rx, ry, rw, rh));
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
