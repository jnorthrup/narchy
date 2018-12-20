package spacegraph.space2d.widget.windo.util;

import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.event.Off;
import jcog.math.v2;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.block.procedure.primitive.ObjectLongProcedure;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.phys.collision.AABB;
import spacegraph.space2d.phys.collision.shapes.CircleShape;
import spacegraph.space2d.phys.collision.shapes.EdgeShape;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.collision.shapes.Shape;
import spacegraph.space2d.phys.common.Settings;
import spacegraph.space2d.phys.dynamics.*;
import spacegraph.space2d.phys.dynamics.joints.*;
import spacegraph.space2d.phys.fracture.PolygonFixture;
import spacegraph.space2d.phys.particle.ParticleColor;
import spacegraph.space2d.phys.particle.ParticleSystem;
import spacegraph.space2d.widget.meta.WeakSurface;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.space2d.widget.windo.Link;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.video.Draw;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.ObjLongConsumer;

/**
 * TODO
 */
public class Box2DGraphEditPhysics extends GraphEditPhysics {

    final Dynamics2D physics;

    final ConcurrentFastIteratingHashMap<Windo, WindowData> w = new ConcurrentFastIteratingHashMap<>(new WindowData[0]);
    private Off loop;
    private int velIter = 4;
    private int posIter = 4;
    float timeScale = 1f;
    static final float minDimension = 0.5f;
    static final float scaling = 10f;

    private static class WindowData {
        final Windo window;
        final Body2D body;
        private final PolygonShape shape;


        private WindowData(Windo window, Body2D body) {
            this.window = window;
            this.body = body;
            this.shape = PolygonShape.box(Math.max(minDimension, window.w() / 2), Math.max(minDimension, window.h() / 2));
            body.setFixedRotation(true);
            body.addFixture(shape, 1f);
            body.setData(window);
        }

        void pre(Dynamics2D physics) {
            RectFloat r = window.bounds;
//            if (r != physBounds) {
//
//                if (!Util.equals(r.w, physBounds.w, SHAPE_SIZE_EPSILON) ||
//                        !Util.equals(r.h, physBounds.h, SHAPE_SIZE_EPSILON)) {
            body.updateFixtures((f) -> f.setShape(
                    shape.setAsBox(r.w / 2 / scaling, r.h / 2 / scaling)

            ));

            v2 target = new v2(r.cx() / scaling, r.cy() / scaling);

            if (body.setTransform(target, 0, Spatialization.EPSILONf))
                body.setAwake(true);

        }

        void post(Dynamics2D physics) {

            float w = window.w(), h = window.h();

            v2 p = body.pos;
            RectFloat r = RectFloat.XYWH(p.x * scaling, p.y * scaling, w, h);
            window.pos(r);
            //if (!r.equals(physBounds, Spatialization.EPSILONf)) {

            //pos(physBounds = r);
            //}
        }

    }


    public Box2DGraphEditPhysics() {
        this.physics = new Dynamics2D();
        physics.setSubStepping(true);
        physics.setWarmStarting(true);
        physics.setContinuousPhysics(true);
        physics.setAllowSleep(true);
    }

    @Override
    public void add(Windo w) {
        WindowData ww = new WindowData(w, new Body2D(BodyType.DYNAMIC, physics));
        this.w.put(w, ww);
        physics.addBody(ww.body);
    }

    @Override
    public void remove(Windo w) {
        WindowData ww = this.w.remove(w);
        physics.removeBody(ww.body);
    }

    @Override
    public Surface starting(GraphEdit g) {
        loop = g.root().animate(this::update);
        return
                new Dyn2DRenderer(true, true, true);
                //new EmptySurface();
    }

    protected boolean update(float dt) {
        w.forEachValue(ww -> ww.pre(physics));
        physics.step(dt * timeScale, velIter, posIter);
        w.forEachValue(ww -> ww.post(physics));
        return true;
    }

    @Override
    public void stop() {
        loop.off();
        loop = null;
    }

    @Override
    public Link link(Wire w) {
        if((w.a instanceof WeakSurface) ^ (w.b instanceof WeakSurface)) {
            //one is a dependent of the other
            return new GlueLink(w);
        } else {
            return new SnakeLink(w);
        }
    }

    private WindowData phy(Windo x) {
        return w.get(x);
    }

    abstract class Box2DLink extends GraphEdit.VisibleLink {

        public Box2DLink(Wire wire) {
            super(wire);
        }

        @Nullable
        protected Dynamics2D world() {
            Body2D b = sourceBody();
            if (b!=null)
                return b.W;
            Body2D c = targetBody();
            if (c!=null)
                return c.W;
            return null;
        }

        protected Body2D body(Surface x) {
            return phy(x.parent(Windo.class)).body;
        }

        public Body2D sourceBody() {
            Surface x = a();
            return body(x);
        }

        public Body2D targetBody() {
            return body(b());
        }


        protected float targetRadius() {
            return targetBounds().extents().length();
        }

        protected float sourceRadius() {
            return sourceBounds().extents().length();
        }

        protected AABB sourceBounds() {
            return physicsBounds(sourceBody());
        }
        protected AABB targetBounds() {
            return physicsBounds(this.targetBody());
        }


        protected AABB physicsBounds(Body2D x) {
            Fixture f = x.fixtures();
            if (f != null) {
                return f.getAABB(0);
            } else {
                return new AABB(); //empty
            }

        }

    }

    /** lightweight elastic attachment/binder */
    class GlueLink extends Box2DLink {

        private final Joint joint;

        final float margin = 0.05f;

        public GlueLink(Wire wire) {
            super(wire);

            Dynamics2D w = world();
            if (w==null) {
                throw new TODO();
                //wire.remove();
            }

            RopeJointDef jd = new RopeJointDef(sourceBody(), targetBody());
            //WeldJointDef jd = new WeldJointDef();
            jd.maxLength = targetLength();
            jd.collideConnected = true;

//            jd.bodyA = sourceBody();
//            jd.bodyB = targetBody();


            //joint = new WeldJoint(w, jd);
            joint = new RopeJoint(w.pool, jd) {
                @Override
                public void initVelocityConstraints(SolverData data) {
                    setPositionFactor(0.01f);
                    this.targetLength = targetLength();
                    super.initVelocityConstraints(data);
                }
            };

            w.addJoint(joint);

            on(()->{
                Dynamics2D ww = world();
                if (ww!=null)
                    ww.removeJoint(joint);
            });
        }

        public float targetLength() {
            //TODO sourceBody().fixtures.shape.computeDistanceToOut()
            return (sourceRadius() + targetRadius()) * (1f + margin);
        }
    }

    /** represents a cable that transmits data from port to port */
    class SnakeLink extends Box2DLink {

        private final Snake snake;

        public SnakeLink(Wire wire) {
            super(wire);

            this.snake = new Snake(sourceBody(), targetBody(), 7);
            on(snake::remove);


//            Surface r = new Box2DVisibleLinkSurface();
//            hold(r);
//            graph.addRaw(r);

        }

//        private class Box2DVisibleLinkSurface extends VisibleLinkSurface {
//            @Override
//            protected void paintLink(GL2 gl, SurfaceRender surfaceRender) {
//                Surface a = a();
//                Surface b = b();
//                gl.glColor4f(1, 1, 1, 0.5f);
//                Draw.halfTriEdge2D(a.x(), a.y(), b.x(), b.y(), a.radius(), gl);
//                gl.glColor4f(1, 1, 1, 0.5f);
//                Draw.halfTriEdge2D(b.x(), b.y(), a.x(), a.y(), b.radius(), gl);
//            }
//        }
    }

    static class Snake {

        private final List<Body2D> bodies;
        private final List<Body2D> attachments;
        final List<Joint> joints;
        //        private final Surface source;
//        private final Surface target;
        private final Body2D sourceBody, targetBody;
        private final int n;

        private transient float elementLength, elementThickness;

        protected v2 sourceCenterWorld() {
            return sourceBody.pos;
        }
        protected v2 targetCenterWorld() {
            return targetBody.pos;
        }
        protected float distance() {
            return sourceBody.pos.distance(targetBody.pos);
        }

        private void updateGeometry() {
            elementLength = (distance() / n) * 0.75f;

            float sourceRadius = sourceRadius(); //((Surface)sourceBody.data()).radius();
            float targetRadius = targetRadius();  //((Surface)targetBody.data()).radius();
            elementThickness = Math.max(Settings.EPSILONsqrt, Math.min(sourceRadius, targetRadius) / 8f);
                    //TODO get from surfaces Math.min( sourceBody.fixtures.shape.radius, targetBody.fixtures.shape.radius );
        }
        protected float targetRadius() {
            return targetBody.fixtures.getAABB(0).extents().length();
        }

        protected float sourceRadius() {
            return sourceBody.fixtures.getAABB(0).extents().length();
        }

        public Snake(Body2D source, Body2D target, int num) {

//            this.source = source;
//            this.target = target;
            this.sourceBody = source;
            this.targetBody = target;

            this.n = num;

            bodies = new FasterList(num);
            joints = new FasterList(num);
            attachments = new FasterList(0);

            Dynamics2D w = sourceBody.W;

            updateGeometry();

            FixtureDef segment = new FixtureDef(PolygonShape.box(1, 1), 0.01f, 0f);
            segment.restitution = 0f;
            segment.filter.maskBits = 0;

            FixtureDef segmentCollidable = new FixtureDef(PolygonShape.box(1, 1), 0.01f, 0f);
            segmentCollidable.restitution = 0f;

            Body2D from = null;

            v2 center = sourceCenterWorld().add(targetCenterWorld()).scale(0.5f);

            int mid = num/2;
            for (int i = 0; i < num; ++i) {


                if (from == null) {
                    from = sourceBody;
                } else {


                    Body2D to = null;
                    if (i == num - 1) {
                        to = targetBody;
                    } else {

                        int finalI = i;
                        to = new SnakeElementBody(center, w, finalI);

                        to.addFixture(i == mid ? segmentCollidable : segment);

                        bodies.add(to);
                        to.setGravityScale(0);
                        to.setLinearDamping(0);
                        to.postUpdate();
                    }

                    RevoluteJointDef jd = new RevoluteJointDef();
                    jd.collideConnected = false;
                    jd.bodyA = from;
                    jd.bodyB = to;
                    jd.referenceAngle = 0;
                    jd.enableMotor = false;
                    //jd.motorSpeed = 100;

                    RevoluteJoint jj = new MyRevoluteJoint(w, jd, 0.05f);
                    joints.add(jj);


                    from = to;
                }


            }

            w.invoke(() -> {
                bodies.forEach(b -> w.addBody(b));
                joints.forEach(w::addJoint);
            });
        }




        /**
         * attach a body to center of one of the segments
         */
        public void attach(Body2D b, int segment) {
            Dynamics2D world = world();
            world.invoke(() -> {
                RevoluteJoint w = (RevoluteJoint) b.W.addJoint(new RevoluteJointDef(bodies.get(segment), b));
                attachments.add(b);
                joints.add(w);
            });
        }

        private Dynamics2D world() {
            return bodies.get(0).W;
        }

        public void remove() {

            Dynamics2D world = world();
            world.invoke(() -> {


                attachments.forEach(Body2D::remove);
                attachments.clear();

                bodies.forEach(Body2D::remove);
                bodies.clear();
            });
        }

        private static class MyRevoluteJoint extends RevoluteJoint {


            MyRevoluteJoint(Dynamics2D w, RevoluteJointDef jd, float power) {
                super(w, jd);
                this.positionFactor = power;
            }

        }

        private class SnakeElementBody extends Body2D {

            private final int finalI;
            float eleLen, eleThick;

            public SnakeElementBody(v2 center, Dynamics2D w, int finalI) {
                super(new BodyDef(BodyType.DYNAMIC, center), w);
                this.finalI = finalI;
                eleLen = Float.NaN;
                eleThick = Float.NaN;
            }

            @Override
            public void postUpdate() {


                if (finalI == 1 /* head */ && (sourceBody.isActive() || targetBody.isActive()) ) {
                    updateGeometry();
                }

                if (!Util.equals(eleLen,elementLength,Settings.EPSILONsqrt) || !Util.equals(eleThick,elementThickness,Settings.EPSILONsqrt)) {
                    eleLen = elementLength;
                    eleThick = elementThickness;

                    updateFixtures(this::updateFixtures);
                }

            }

            private void updateFixtures(Fixture f /* the only one */) {
                ((PolygonShape) f.shape).setAsBox(eleLen, eleThick);

                RevoluteJoint rj = (RevoluteJoint) ((Snake.this.joints).get(finalI - 1));
                if (rj!=null) {
                    if (finalI != 0) {
                        rj.getLocalAnchorB().set(+eleLen, 0);
                    } else {
                        rj.getLocalAnchorB().set(0, 0);
                    }
                }

                RevoluteJoint rk = (RevoluteJoint) ((Snake.this.joints).get(finalI));
                if (rk!=null) {
                    if (finalI != n - 1) {
                        rk.getLocalAnchorA().set(-eleLen, 0);
                    } else {
                        rk.getLocalAnchorA().set(0, 0);
                    }
                }
            }

        }
    }


//    @Deprecated public class PhyWindow extends Windo {
//        //public final Body2D body;
//        //private final PolygonShape shape;
//
//
//        PhyWindow(RectFloat2D initialBounds, boolean collides) {
//            super();
//            pos(initialBounds);
//
//
////            this.shape =
////
////                    PolygonShape.box(initialBounds.w / 2, initialBounds.h / 2);
////
////            FixtureDef fd = new FixtureDef(shape, 1f, 0.75f);
////            if (!collides) {
////                fd.filter.maskBits = 0;
////            }
////
////            fd.setRestitution(0.1f);
////
////
////            W.addBody(this.body = new WallBody(initialBounds.cx(), initialBounds.cy()), fd);
////            body.setLinearDamping(linearDampening);
////            if (!collides) {
////                body.setGravityScale(0f);
////            }
//        }
//
////        void setCollidable(boolean c) {
////            W.invoke(() -> {
////                body.fixtures.filter.maskBits = (c ? 0xffff : 0);
////                body.setGravityScale(c ? 1f : 0f);
////                body.setAwake(true);
////            });
////        }
//
//        @Override
//        public boolean fingerable(DragEdit d) {
//            if (d == DragEdit.MOVE)
//                return false;
//
//
//            return super.fingerable(d);
//        }
//
//        public void remove() {
//            synchronized (links) {
//                links.removeNode(this);
//            }
////            W.removeBody(this.body);
//            //Dyn2DSurface.this.remove(this);
//            throw new TODO();
//        }
//
//
//        public Pair<PhyWindow, Wire> sprout(Surface target, float scale) {
//            return sprout(target, scale, 1f);
//        }
//
//        /**
//         * convenience method for essentially growing a separate window
//         * of a proportional size with some content (ex: a port),
//         * and linking it to this window via a constraint.
//         */
//        Pair<PhyWindow, Wire> sprout(Surface target, float scale, float targetAspect) {
//            PhyWindow sprouted = spawn(target, scale, targetAspect);
//
//            return pair(sprouted, link(target));
//        }
//
////        /**
////         * spawns and attaches a new component to the boundary of this
////         */
////        public PhyWindow grow(Surface target, float scale, float targetAspect, v2 normal) {
////
////            PhyWindow x = spawn(target, scale, targetAspect);
////
////            W.invoke(() -> {
////                v2 myWeldLocal, theirWeldLocal;
////                RayCastInput input = new RayCastInput();
////                RayCastOutput output = new RayCastOutput();
////                {
////                    input.p2.set(0, 0);
////                    float r = radius() * 2;
////                    input.p1.set(0 + normal.x * r, 0 + normal.y * r);
////                    input.maxFraction = 1.0f;
////
////                    boolean hit = body.fixtures.raycast(output, input, 0);
////                    assert (hit);
////                    v2 hitPoint = (input.p2.sub(input.p1)).scaled(output.fraction).added(input.p1);
////                    myWeldLocal = hitPoint;
////                }
////                {
////                    input.p2.set(0, 0);
////                    float r = x.radius() * 2;
////                    input.p1.set(0 - normal.x * r, 0 - normal.y * r);
////                    input.maxFraction = 1.0f;
////
////                    boolean hit = x.body.fixtures.raycast(output, input, 0);
////                    assert (hit);
////                    v2 hitPoint = (input.p2.sub(input.p1)).scaled(output.fraction).added(input.p1);
////                    theirWeldLocal = hitPoint;
////                }
////
////                WeldJoint j = weld(x, myWeldLocal, theirWeldLocal);
////
////            });
////            return x;
////        }
//
////        private WeldJoint weld(PhyWindow x, v2 myLocal, v2 theirLocal) {
////            WeldJointDef jd = new WeldJointDef();
////            jd.bodyA = this.body;
////            jd.bodyB = x.body;
////            jd.localAnchorA.set(myLocal.scaled(0.5f));
////            jd.localAnchorB.set(theirLocal.scaled(0.5f));
////            jd.referenceAngle = ((v2) myLocal).angle(theirLocal);
////            jd.collideConnected = false;
////            jd.dampingRatio = 0.5f;
////            jd.frequencyHz = 0.25f;
////
////            WeldJoint j = new WeldJoint(W.pool, jd);
////            W.addJoint(j);
////            return j;
////        }
//
//        PhyWindow spawn(Surface target, float scale, float targetAspect) {
//            float W = w();
//            float H = h();
//            float sprouterRadius = radius();
//            float w = W * scale;
//            float h = H * scale;
//
//
//            RectFloat2D sproutSize = RectFloat2D.XYWH(0, 0, w, h);
//
//
//            float minRadius = sprouterRadius + sproutSize.radius();
//
//            float a = (float) (Math.random() * 2 * (float) Math.PI);
//            float dx = cx() + (float) (minRadius * Math.cos(a));
//            float dy = cy() + (float) (minRadius * Math.sin(a));
//
//            return put(target, sproutSize.move(dx, dy, Spatialization.EPSILONf));
//        }
//
//
//        /**
//         * assumes the PhyWindow wraps *THE* source
//         */
//        Wire link(Surface target) {
//            assert (children().length == 1);
//            return link(get(0), target);
//        }
//
//
//
//        /**
//         * convenience method for creating a basic undirected link joint.
//         * no endpoint is necessarily an owner of the other so
//         * it should not matter who is the callee.
//         * <p>
//         * duplicate links are prevented.
//         */
//        public Wire link(Surface source, Surface target) {
//            return link(new Wire(source, target));
//        }
//
//        /**
//         * undirected link
//         */
//        Wire link(Wire wire) {
//
//            Surface aa = wire.a;
//            Surface bb = wire.b;
//
//            synchronized (links) {
//
//                NodeGraph.MutableNode<Surface, Wire> A = links.addNode(aa);
//
//                Iterable<FromTo<Node<spacegraph.space2d.Surface, spacegraph.space2d.widget.windo.Wire>, Wire>> edges = A.edges(false, true);
//                if (edges != null) {
//
//                    for (FromTo<Node<spacegraph.space2d.Surface, spacegraph.space2d.widget.windo.Wire>, Wire> e : edges) {
//                        Wire ee = e.id();
//                        if (wire.equals(ee))
//                            return ee;
//                    }
//                }
//
//                if (!wire.connect()) {
//                    return null;
//                }
//
//                NodeGraph.MutableNode<Surface, Wire> B = links.addNode(bb);
//                links.addEdge(A, wire, B);
//
//
////                W.invoke(() -> {
////
////
////                    {
////
////
////                        Snake s = snake(wire, () -> unlink(aa, bb));
////
////                    }
////
////
////                });
//            }
//
//            return wire;
//
//        }
//
//
//        void sproutBranch(String label, float scale, float childScale, Iterable<Surface> children) {
//            CheckBox toggle = new CheckBox(label);
//            Pair<PhyWindow, Wire> toggleWindo = sprout(toggle, scale);
////            List<PhyWindow> built = new FasterList(0);
////            toggle.on((cb, enabled) -> W.invoke(() -> {
////
////                if (enabled) {
////                    for (Surface x : children) {
////                        built.add(toggleWindo.getOne().sprout(x, childScale).getOne());
////                    }
////                } else {
////
////                    built.forEach(PhyWindow::remove);
////                    built.clear();
////                }
////
////            }));
//        }
//
//        public void sproutBranch(String label, float scale, float childScale, Supplier<Surface[]> children) {
//            sproutBranch(label, scale, childScale, ArrayIterator.iterable(children.get()));
//        }
//
//        @Override
//        public boolean tangible() {
//            return true;
//        }
//
//    }

    private class Dyn2DRenderer extends Surface {
        final boolean drawJoints, drawBodies, drawParticles;

        public Dyn2DRenderer(boolean drawJoints, boolean drawBodies, boolean drawParticles) {
            this.drawJoints = drawJoints;
            this.drawBodies = drawBodies;
            this.drawParticles = drawParticles;
        }

        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {

            Dynamics2D w = physics;

            if (drawJoints) {
                long now = System.currentTimeMillis();
                w.joints(j -> drawJoint(j, gl, now));
            }

            if (drawBodies) {
                w.bodies(b -> drawBody(b, gl));
            }

            if (drawParticles) {
                drawParticleSystem(gl, w.particles);
            }

        }

        private void drawParticleSystem(GL2 gl, ParticleSystem system) {

            int particleCount = system.getParticleCount();
            if (particleCount != 0) {
                float particleRadius = system.getParticleRadius();
                v2[] positionBuffer = system.getParticlePositionBuffer();
                ParticleColor[] colorBuffer = null;
                if (system.m_colorBuffer.data != null) {
                    colorBuffer = system.getParticleColorBuffer();
                }


                Draw.particles(gl, positionBuffer, particleRadius, 6, colorBuffer, particleCount);

            }
        }

        private void drawJoint(Joint joint, GL2 g, long now) {
            Object data = joint.data();
            if (data instanceof ObjectLongProcedure) {
                ((ObjLongConsumer) data).accept(g, now);
            } else {

                Draw.colorHash(g, joint.getClass().hashCode(), 0.5f);
                g.glLineWidth(10f);
            }
            v2 v1 = new v2(), v2 = new v2();
//            switch (joint.getType()) {
//                default:
                    joint.getAnchorA(v1);
                    joint.getAnchorB(v2);
//                    break;
//            }
            Draw.line( v1.x * scaling, v1.y * scaling, v2.x * scaling, v2.y * scaling, g);

        }

        private void drawBody(Body2D body, GL2 gl) {


            //        if (body.data() instanceof PhyWindow.WallBody) {
            //            return;
            //        }
            if (body instanceof Consumer) {
                ((Consumer) body).accept(gl);
                return;
            }



            boolean awake = body.isAwake();

            gl.glColor4f(0.5f, 0.5f, 0.5f, awake ? 0.75f : 0.65f);

            for (Fixture f = body.fixtures; f != null; f = f.next) {
                PolygonFixture pg = f.polygon;
                if (pg != null) {

                } else {
                    Shape shape = f.shape();


                    switch (shape.m_type) {
                        case POLYGON:

                            Draw.poly(body, gl, scaling, (PolygonShape) shape);
                            break;
                        case CIRCLE:

                            CircleShape circle = (CircleShape) shape;
                            float r = circle.skinRadius;
                            v2 v = new v2();
                            body.getWorldPointToOut(circle.center, v);
                            v.scale(scaling);

                            Draw.circle(gl, v, true, r * scaling, 9);
                            break;
                        case EDGE:
                            EdgeShape edge = (EdgeShape) shape;
                            v2 p1 = edge.m_vertex1;
                            v2 p2 = edge.m_vertex2;
                            gl.glLineWidth(4f);
                            Draw.line( p1.x * scaling, p1.y * scaling, p2.x * scaling, p2.y * scaling, gl);
                            break;
                    }
                }
            }


        }

        ; //TODO
    }


}

