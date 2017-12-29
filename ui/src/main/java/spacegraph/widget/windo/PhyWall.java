package spacegraph.widget.windo;

import com.google.common.collect.Iterables;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.exe.Loop;
import jcog.list.FasterList;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.NotNull;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.Surface;
import spacegraph.input.Fingering;
import spacegraph.layout.Flatten;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.FlatDynamic;
import spacegraph.phys.collision.DefaultCollisionConfiguration;
import spacegraph.phys.collision.DefaultIntersecter;
import spacegraph.phys.collision.broad.DbvtBroadphase;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.SimpleBoxShape;
import spacegraph.space.Cuboid;
import spacegraph.widget.button.PushButton;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * wall which organizes its sub-surfaces according to 2D phys dynamics
 */
public class PhyWall extends Wall {
    final Dynamics dyn;

    float spaceBoundsXY = 2000;
    float spaceBoundsZ = 10;

    final Map<String, Spatial<String>> objects = new ConcurrentHashMap<>();
    private final Loop loop;
    private final Flatten<String> flat;
    final List<Spatial<String>> env = new FasterList();

    final AtomicBoolean busy = new AtomicBoolean();

    public PhyWall() {

        this.dyn = new Dynamics<>(new DefaultIntersecter(
                new DefaultCollisionConfiguration()), new DbvtBroadphase(),
                Iterables.concat(env, objects()));

//        float INF = spaceBoundsXY * 9;
//        //BOTTOM
//        env.add( new Boundary(-INF, -INF, -INF, +INF, +INF, -spaceBoundsZ) );
//        //TOP
//        env.add( new Boundary(-INF, -INF, +spaceBoundsZ, +INF, +INF, INF) );
//        //S
//        env.add( new Boundary(-INF, -INF, -INF, -INF, -spaceBoundsXY, +INF) );
//        //N
//        env.add( new Boundary(-INF, +spaceBoundsXY, -INF, +INF, +INF, +INF) );
//        //W
//        env.add( new Boundary(-INF, -INF, -INF, -spaceBoundsXY, +INF, +INF) );
//        //E
//        env.add( new Boundary(+spaceBoundsXY, -INF, -INF, +INF, +INF, +INF) );

        flat = new Flatten<>(0.3f, 0.8f);

        this.loop = new Loop() {

            float dt = 0.02f;
            int physSubSteps = 4;

            @Override
            public boolean next() {

                if (busy.compareAndSet(false, true)) {
                    dyn.update(dt, physSubSteps);
                    flat.update(objects(), dt);
                    busy.set(false);
                }

                return true;
            }
        };
        loop.runFPS(30f);
    }

    @NotNull
    protected Collection<Spatial<String>> objects() {
        return objects.values();
    }

    final AtomicInteger i = new AtomicInteger(0);

    @Override
    public Windo addWindo() {
        SpatialWindo s = new SpatialWindo("w" + i.getAndIncrement());
        objects.put(s.spatial.id, s.spatial);
        children.add(s);
        return s;
    }

    class SpatialWindo extends Windo {
        public final SimpleSpatial<String> spatial;

        SpatialWindo(String id) {
            this.spatial = new Cuboid(id, w(), h()) {


                @Override
                public Dynamic newBody(boolean collidesWithOthersLikeThis) {
                    return new FlatDynamic(mass(), shape, transform, (short) +1, (short) -1);
                }

                @Override
                public void update(Dynamics world) {

                    boolean newly = body == null; //HACK

                    super.update(world);

                    if (newly) //HACK
                        commitPhysics();

                    updated = true;
                }

            };
        }

        boolean updated = false;

        @Override
        protected void paintIt(GL2 gl) {
            if (updated && busy.compareAndSet(false, true)) {
                SimpleBoxShape bs = (SimpleBoxShape) spatial.shape;
                float w = bs.x();
                float h = bs.y();
                float d = bs.z();

                Transform transform = spatial.transform;
                float px = transform.x;
                float py = transform.y;
                transform.x = Util.clamp(px, -spaceBoundsXY / 2 + w / 2, spaceBoundsXY / 2 - w / 2);
                transform.y = Util.clamp(py, -spaceBoundsXY / 2 + h / 2, spaceBoundsXY / 2 - h / 2);
                //transform.z = Util.clamp(transform.z, -spaceBoundsZ/2+d/2, spaceBoundsZ/2-d/2);

                Dynamic body = spatial.body;
                if (!Util.equals(px, transform.x, Surface.EPSILON) || !Util.equals(py, transform.y, Surface.EPSILON)) {
                    //body.linearVelocity.zero(); //HACK emulates infinite absorption on collision with outer bounds
                    body.angularVelocity.zero();
                    body.totalTorque.zero();

                    body.linearVelocity.scale(-0.5f); //bounce but diminish
                    body.totalForce.scale(-0.5f);
                    //body.clearForces();
                }
                if (hover) {
                    body.linearVelocity.zero();
                    body.angularVelocity.zero();
                    body.clearForces();
                }

                float x = transform.x;
                float y = transform.y;
                //float z = transform.z;

                SpatialWindo.this.pos(x - w / 2, y - h / 2, x + w / 2, y + h / 2);
                updated = false;
                busy.set(false);
            }

            super.paintIt(gl);
        }

        /**
         * commits display/interaction state to physics
         */
        protected void commitPhysics() {
            spatial.transform.x = cx();
            spatial.transform.y = cy();
            float H = h();
            float W = w();
            float D = Math.max(W, H);
            spatial.scale(W, H, D);
            //(D/ spaceBoundsXY) * spaceBoundsZ * 0.5f);

            if (spatial.body != null) {
                spatial.body.setDamping(0.8f, 0.9f);
                spatial.body.setFriction(0.5f);
                spatial.body.setRestitution(0.5f);
                float density = 0.1f;
                spatial.body.setMass(W * H * D * density);

            }
        }

        @Override
        public Surface pos(RectFloat2D r) {
            RectFloat2D b = this.bounds;
            super.pos(r);
            if (bounds != b) { //if different
                layout();
                commitPhysics();
            }
            return null;
        }

        @Override
        protected Fingering fingering(DragEdit mode) {
            Fingering f = super.fingering(mode);
            if (f != null) {
                spatial.body.clearForces();
                spatial.body.angularVelocity.zero();
                spatial.body.linearVelocity.zero();
            }
            return f;
        }
    }

    public static void main(String[] args) {
        PhyWall d = new PhyWall();

        //d.children.add(new GridTex(16).pos(0,0,1000,1000));

        {
            Windo w = d.addWindo(Widget.widgetDemo());
            w.pos(80, 80, 550, 450);

//            Windo.Port p = w.addPort("X");
        }

//        d.addWindo(grid(new PushButton("x"), new PushButton("y"))).pos(10, 10, 50, 50);

        for (int i = 0; i < 100; i++) {
            float rx = (float) (Math.random() * 1000f / 2);
            float ry = (float) (Math.random() * 1000f / 2);
            float rw = 25 + 150 * (float) Math.random();
            float rh = 20 + 150 * (float) Math.random();
            d.addWindo(new PushButton("w" + i)).pos(rx, ry, rx + rw, ry + rh);
        }

        //d.newWindo(grid(new PushButton("x"), new PushButton("y"))).pos(-100, -100, 0, 0);

        SpaceGraph.window(d, 800, 800);

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
