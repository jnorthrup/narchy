package spacegraph.space2d.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.graph.ImmutableDirectedEdge;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.NodeGraph;
import jcog.event.On;
import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.rect.RectFloat2D;
import jcog.util.ArrayIterator;
import org.eclipse.collections.api.block.procedure.primitive.ObjectLongProcedure;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.input.finger.DoubleClicking;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerDragging;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.hud.Ortho;
import spacegraph.space2d.phys.collision.AABB;
import spacegraph.space2d.phys.collision.RayCastInput;
import spacegraph.space2d.phys.collision.RayCastOutput;
import spacegraph.space2d.phys.collision.shapes.CircleShape;
import spacegraph.space2d.phys.collision.shapes.EdgeShape;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.collision.shapes.Shape;
import spacegraph.space2d.phys.common.Transform;
import spacegraph.space2d.phys.dynamics.*;
import spacegraph.space2d.phys.dynamics.joints.*;
import spacegraph.space2d.phys.fracture.PolygonFixture;
import spacegraph.space2d.phys.particle.ParticleColor;
import spacegraph.space2d.phys.particle.ParticleSystem;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.meta.ProtoWidget;
import spacegraph.space2d.widget.meta.WizardFrame;
import spacegraph.space2d.widget.tab.ButtonSet;
import spacegraph.space2d.widget.tab.TabPane;
import spacegraph.space2d.widget.text.Label;
import spacegraph.util.animate.Animated;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;
import spacegraph.video.Draw;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * wall which organizes its sub-surfaces according to 2D phys dynamics
 */
public class Dyn2DSurface extends Wall implements Animated {

    static final float SHAPE_SIZE_EPSILON = 0.0001f;
    final static int MOUSE_JOINT_BUTTON = 0;
    public final Dynamics2D W = new Dynamics2D(new v2(0, 0));
    public final Random rng = new XoRoShiRo128PlusRandom(1);
    /**
     * increase for more physics precision
     */
    final int solverIterations = 8;

    @Override
    public boolean tangible() {
        return true;
    }

    /**
     * TODO use more efficient graph representation
     */
    final MapNodeGraph<Surface, Wire> links = new MapNodeGraph();
    final DoubleClicking doubleClicking = new DoubleClicking(0, this::doubleClick);
    private final float linearDampening = 0.9f;
    FingerDragging jointDrag = new FingerDragging(MOUSE_JOINT_BUTTON) {

        final Body2D ground = W.addBody(new BodyDef(BodyType.STATIC),
                new FixtureDef(PolygonShape.box(0, 0), 0, 0).noCollide());

        private volatile MouseJoint mj;

        @Override
        public boolean start(Finger f) {
            boolean b = super.start(f);
            if (b) {

                Body2D touched2D;
                if (((touched2D = pick(f)) != null)) {
                    MouseJointDef def = new MouseJointDef();

                    def.bodyA = ground;
                    def.bodyB = touched2D;
                    def.collideConnected = true;


                    def.target.set(f.pos);

                    def.maxForce = 500f * touched2D.getMass();
                    def.dampingRatio = 0;

                    mj = (MouseJoint) W.addJoint(new MouseJoint(W.pool, def));
                    return true;
                }

            }
            return false;
        }

        public Body2D pick(Finger ff) {
            v2 p = ff.pos.scale(scaling);
            //v2 p = ff.relativePos(Dyn2DSurface.this).scale(scaling);

            float w = 0;
            float h = 0;


            final Fixture[] found = {null};
            W.queryAABB((Fixture f) -> {
                if (f.body.type != BodyType.STATIC &&
                        f.filter.maskBits != 0 /* filter non-colllidables */ && f.testPoint(p)) {
                    found[0] = f;
                    return false;
                }

                return true;
            }, new AABB(new v2(p.x - w, p.y - h), new v2(p.x + w, p.y + h), false));

//            //TODO use queryAABB
//            for (Body2D b = W.bodies(); b != null; b = b.next) {
//
//                if (b.type==BodyType.STATIC) continue; //dont grab statics
//
//                for (Fixture f = b.fixtures(); f != null; f = f.next) {
//                    if (f.filter.maskBits != 0 /* filter non-colllidables */ && f.testPoint(p)) {
//                        return b;
//                    }
//                }
//            }
            return found[0] != null ? found[0].body : null;
        }

        @Override
        public void stop(Finger finger) {
            super.stop(finger);
            if (mj != null) {
                W.removeJoint(mj);
                mj = null;
            }
        }

        @Override
        protected boolean drag(Finger f) {
            if (mj != null) {
                v2 p = f.pos.scale(scaling);
                //v2 p = f.relativePos(Dyn2DSurface.this).scale(scaling);
                /*if (clickedPoint != null)*/

//                v2 clickedPoint = f.hitOnDown[MOUSE_JOINT_BUTTON];
//                p.x -= clickedPoint.x;
//                p.y -= clickedPoint.y;
                mj.setTarget(p);
            }
//                center.x = startCenter.x - p.x / zoom;
//                center.y = startCenter.y + p.y / zoom;
//            } else {
//                if (mj != null) {
//                    mj.setTarget(mousePosition);
//                }
//            }
            return true;

        }

    };
    private On on;
    private float scaling = 1f;

    public Dyn2DSurface() {
        super();

        W.setParticleRadius(0.2f);
        W.setParticleDensity(1.0f);

        W.setWarmStarting(true);
        W.setAllowSleep(true);
        W.setContinuousPhysics(true);
        //W.setSubStepping(true);
    }

    /**
     * create a static box around the content, which moves along with the surface's bounds
     */
    public Dyn2DSurface enclose() {
        new StaticBox(this::bounds);
        return this;
    }

    public RectFloat2D bounds() {
        return bounds;
    }

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            on = ((Ortho) root()).animate(this);
            return true;
        }
        return false;
    }

    @Override
    public boolean animate(float dt) {

        W.step(dt, solverIterations, solverIterations);

        return true;
    }

    @Override
    protected void paintBelow(GL2 gl) {
        super.paintBelow(gl);

        Dynamics2D w = this.W;

        long now = System.currentTimeMillis();

        //gl.glPushMatrix();



        w.joints(j -> drawJoint(j, gl, now));

        w.bodies(b -> drawBody(b, gl));

        drawParticleSystem(gl, w.particles);

        //gl.glPopMatrix();
    }

    private void drawParticleSystem(GL2 gl, ParticleSystem system) {
        //boolean wireframe = (m_debugDraw.getFlags() & DebugDraw.e_wireframeDrawingBit) != 0;
        int particleCount = system.getParticleCount();
        if (particleCount != 0) {
            float particleRadius = system.getParticleRadius();
            Tuple2f[] positionBuffer = system.getParticlePositionBuffer();
            ParticleColor[] colorBuffer = null;
            if (system.m_colorBuffer.data != null) {
                colorBuffer = system.getParticleColorBuffer();
            }
//            if (wireframe) {
//                m_debugDraw.drawParticlesWireframe(positionBuffer, particleRadius, colorBuffer,
//                        particleCount);
//            } else {
            Draw.particles(gl, positionBuffer, particleRadius, 6, colorBuffer, particleCount);
//            }
        }
    }

    private void drawJoint(Joint joint, GL2 g, long now) {
        Object data = joint.data();
        if (data instanceof ObjectLongProcedure) {
            ((ObjectLongProcedure) data).accept(g, now);
        } else {

            Draw.colorHash(g, joint.getClass().hashCode(), 0.5f);
            g.glLineWidth(10f);
        }
        Tuple2f v1 = new v2(), v2 = new v2();
        switch (joint.getType()) {
            default:
                joint.getAnchorA(v1);
                joint.getAnchorB(v2);
                break;
        }
        Draw.line(g, v1.x*scaling, v1.y*scaling, v2.x*scaling, v2.y*scaling);

    }

    private void drawBody(Body2D body, GL2 gl) {
//                    if (body.getType() == BodyType.DYNAMIC) {
//                        g.setColor(Color.LIGHT_GRAY);
//                    } else {
//                        g.setColor(Color.GRAY);
//                    }

        if (body.data() instanceof PhyWindow.WallBody) {
            return; //its rendered already via its Surface
        }
        if (body instanceof Consumer) { //HACK make better custom enderer interface
            ((Consumer) body).accept(gl);
            return;
        }

        //boolean active = body.isActive();
        boolean awake = body.isAwake();
        gl.glColor4f(0.5f, 0.5f, 0.5f, awake ? 0.75f : 0.65f);

        //Tuple2f v = new v2();
        //List<PolygonFixture> generalPolygons = new FasterList<>();
        for (Fixture f = body.fixtures; f != null; f = f.next) {
            PolygonFixture pg = f.polygon;
            if (pg != null) {
                //generalPolygons.add(pg);
            } else {
                Shape shape = f.shape();
                switch (shape.m_type) {
                    case POLYGON:
                        Draw.poly(body, gl, scaling, (PolygonShape) shape);
                        break;
                    case CIRCLE:

                        CircleShape circle = (CircleShape) shape;
                        float r = circle.radius;
                        v2 v = new v2();
                        body.getWorldPointToOut(circle.center, v);
                        v.scale(scaling);
                        //Point p = getPoint(v);
                        //int wr = (int) (r * zoom);
                        //g.fillOval(p.x - wr, p.y - wr, wr * 2, wr * 2);
                        Draw.circle(gl, v, true, r*scaling, 9);
                        break;
                    case EDGE:
                        EdgeShape edge = (EdgeShape) shape;
                        Tuple2f p1 = edge.m_vertex1;
                        Tuple2f p2 = edge.m_vertex2;
                        gl.glLineWidth(4f);
                        Draw.line(gl, p1.x*scaling, p1.y*scaling, p2.x*scaling, p2.y*scaling);
                        break;
                }
            }
        }

//                    if (generalPolygons.size() != 0) {
//                        PolygonFixture[] polygonArray = generalPolygons.toArray(new PolygonFixture[generalPolygons.size()]);
//                        for (PolygonFixture poly : polygonArray) {
//                            int n = poly.size();
//                            int x[] = new int[n];
//                            int y[] = new int[n];
//                            for (int i = 0; i < n; ++i) {
//                                body.getWorldPointToOut(poly.get(i), v);
//                                Point p = getPoint(v);
//                                x[i] = p.x;
//                                y[i] = p.y;
//                            }
//                            g.fillPolygon(x, y, n);
//                        }
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            on.off();
            on = null;
            return true;
        }
        return false;
    }

    public float rngPolar(float scale) {
        return //2f*(rng.nextFloat()*scale-0.5f);
                (float) rng.nextGaussian() * scale;
    }

    public float rngNormal(float scale) {
        return rng.nextFloat() * scale;
    }

    /**
     * spawns in view center at the given size
     */
    public PhyWindow put(Surface content, float w, float h) {
        Ortho view = (Ortho) root();
        return put(content, RectFloat2D.XYWH(view.x(), view.y(), w, h));
    }

    public PhyWindow put(Surface content, RectFloat2D initialBounds) {
        return put(content, initialBounds, true);
    }

    public PhyWindow put(Surface content, RectFloat2D initialBounds, boolean collides) {
        PhyWindow s = new PhyWindow(initialBounds, collides);

        add(s);

        s.add(content);

        return s;
    }

    protected Snake snake(Wire wire, Runnable onRemove) {
        Surface source = wire.a;
        Surface target = wire.b;

        assert (source != target);

        float sa = source.bounds.area();
        float ta = target.bounds.area();
        float areaDiff = Math.abs(sa - ta) / (sa + ta);

        int segments = Util.lerp(areaDiff, 8, 6); //heuristic estimate: larger area difference = shorter snake

        float EXPAND_SCALE_FACTOR = 4;

        PushButton deleteButton = new PushButton("x");
        Surface menu = new TabPane(ButtonSet.Mode.Multi, Map.of("o", () -> new Gridding(
                new Label(source.toString()),
                new Label(target.toString()),
                deleteButton
        )), (l) -> new CheckBox(l) {
            @Override
            protected String label(String text, boolean on) {
                return text; //override just display the 'o'
            }

            @Override
            public ToggleButton set(boolean expanded) {

                super.set(expanded);

                synchronized (wire) {

                    PhyWindow w = parent(PhyWindow.class);
                    if (w == null)
                        return this;
                    float cx = w.cx();
                    float cy = w.cy();
                    float ww, hh;
                    if (expanded) {
                        //grow
                        ww = w.w() * EXPAND_SCALE_FACTOR;
                        hh = w.h() * EXPAND_SCALE_FACTOR;
                    } else {
                        //shrink
                        ww = w.w() / EXPAND_SCALE_FACTOR;
                        hh = w.h() / EXPAND_SCALE_FACTOR;
                    }
                    w.pos(cx - ww / 2, cy - hh / 2, cx + ww / 2, cy + hh / 2);
                }

                return this;
            }
        });

        PhyWindow menuBody = put(menu,
                RectFloat2D.mid(source.bounds, target.bounds, 0.1f));

        float mw = menuBody.radius();

        Snake s = new Snake(source, target, segments, 1.618f * 2 * mw, mw) {

            @Override
            public void remove() {
                onRemove.run();
                super.remove();
            }
        };


        s.attach(menuBody.body, segments / 2 - 1);

        deleteButton.click(s::remove);

        int jj = 0;
        for (Joint j : s.joints) {

            float p = ((float) jj) / (segments - 1);

            //custom joint renderer: color coded indicate activity and type of data
            j.setData((ObjectLongProcedure<GL2>) (g, now) -> {

                int TIME_DECAY_MS = 250;
                boolean side = p < 0.5f;
                float activity =
                        wire.activity(side, now, TIME_DECAY_MS);
                //Util.lerp(p, wire.activity(false, now, TIME_DECAY_MS), wire.activity(true, now, TIME_DECAY_MS));


                int th = wire.typeHash(side);
                if (th == 0) {
                    g.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
                } else {
                    Draw.colorHash(g, th, 0.9f, 0.5f + 0.5f * activity, 0.5f + 0.4f * activity);
                }

                g.glLineWidth(10f + activity * 10f);

//            Draw.line(g, w.a.cx(), w.a.cy(), w.b.cx(), w.b.cy());
//            return;
            });
            jj++;
        }

        return s;
    }

    protected RopeJoint rope(Surface source, Surface target) {

        RopeJointDef jd = new RopeJointDef(source.parent(PhyWindow.class).body, target.parent(PhyWindow.class).body);

        jd.collideConnected = true;
        jd.maxLength = Float.NaN; //should be effectively ignored by the implementation below

        RopeJoint ropeJoint = new RopeJoint(Dyn2DSurface.this.W.pool, jd) {

            float lengthScale = 2.05f;

            @Override
            public float targetLength() {
                //maxLength will be proportional to the radii of the two bodies
                //so that if either changes, the rope's behavior changes also

//                        DistanceOutput o = new DistanceOutput();
//
//                        DistanceInput i = new DistanceInput();
//
//                        Fixture fixA = getBodyA().fixtures();
//                        assert(fixA.m_proxyCount==1);
//                        i.proxyA.set(getBodyA().fixtures().shape(), 0);
//                        i.transformB.setIdentity();
//
//                        Fixture fixB = getBodyB().fixtures();
//                        assert(fixB.m_proxyCount==1);
//                        i.proxyB.set(fixB.shape(), 0);
//                        i.transformB.setIdentity();
//
//                        i.useRadii = true;
//
//                        pool.getDistance().distance(o, i);
//                        return o.distance;


//                        v2 normal = new v2();//getBodyA().pos.sub(getBodyB().pos);
//                        Tuple2f midPoint = getBodyA().pos.add(getBodyB().pos).scaled(0.5f);
//                        float abDist = getBodyA().fixtures.computeDistance(midPoint, 0, normal);
//                        float baDist = getBodyB().fixtures.computeDistance(midPoint, 0, normal);
//                        return 2 * (Math.abs(abDist) + Math.abs(baDist)) * lengthScale;

                return ((source.radius() + target.radius()) * lengthScale)
                        //* (0.9f/(1f+m_impulse))
                        ;


                //return 0;
            }
        };


        W.addJoint(ropeJoint);
        return ropeJoint;
    }

    public Iterable<ImmutableDirectedEdge<Surface, Wire>> edges(Surface s) {
        NodeGraph.Node<Surface, Wire> n = links.node(s);
        return n != null ? n.edges(true, true) : Collections.emptyList();
    }

    @Override
    public Surface tryTouch(Finger finger) {

        Surface s = super.tryTouch(finger);
        if (s != null && s != this && !(s instanceof PhyWindow))
            return s; //some other content, like an inner elmeent of a window but not a window itself

        if (doubleClicking.update(finger))
            return this;

        if (finger.tryFingering(jointDrag))
            return this;


        return s != null ? s : this;
        //return s;
    }

    void doubleClick(v2 pos) {
        put(
                new WizardFrame(new ProtoWidget()) {
                    @Override
                    protected void become(Surface next) {
                        super.become(next);

                        PhyWindow pp = parent(PhyWindow.class);
                        if (next instanceof ProtoWidget) {
                            pp.setCollidable(false);
                        } else {
                            pp.setCollidable(true);
                        }

                    }
                },
                RectFloat2D.XYWH(pos.x, pos.y, 1, 1), false);
    }

    public Dyn2DSurface scale(float v) {
        scaling = v;
        return this;
    }

    class StaticBox {

        private final Body2D body;
        private final Fixture bottom;
        private final Fixture top;
        private final Fixture left;
        private final Fixture right;

        public StaticBox(Supplier<RectFloat2D> bounds) {

            float w = 1, h = 1, thick = 0.5f; //temporary for init

            body = W.addBody(new Body2D(new BodyDef(BodyType.STATIC), W) {
                @Override
                public boolean preUpdate() {
                    update(bounds.get());
                    synchronizeFixtures();
                    return true;
                }
            });
            bottom = body.addFixture(
                    new FixtureDef(PolygonShape.box(w / 2 - thick / 2, thick / 2),
                            0, 0)
            );
            top = body.addFixture(
                    new FixtureDef(PolygonShape.box(w / 2 - thick / 2, thick / 2),
                            0, 0)
            );
            left = body.addFixture(
                    new FixtureDef(PolygonShape.box(thick / 2, h / 2 - thick / 2),
                            1, 0)
            );
            right = body.addFixture(
                    new FixtureDef(PolygonShape.box(thick / 2, h / 2 - thick / 2),
                            1, 0)
            );


        }

        protected void update(RectFloat2D bounds) {

            body.updateFixtures(f -> {

                float cx = bounds.cx()/scaling;
                float cy = bounds.cy()/scaling;
                float thick = Math.min(bounds.w, bounds.h) / 16f/scaling;

                float W = bounds.w / scaling;
                float H = bounds.h / scaling;
                ((PolygonShape)top.shape).setAsBox(W, thick, new v2(cx/2, +H), 0);
                ((PolygonShape)right.shape).setAsBox(thick, H, new v2(+W, cy/2), 0);
                ((PolygonShape)bottom.shape).setAsBox(W, thick, new v2(cx, 0), 0);
                ((PolygonShape)left.shape).setAsBox(thick, H, new v2(0, cy), 0);
            });

        }
    }

    public class PhyWindow extends Windo {
        public final Body2D body;
        private final PolygonShape shape;

//        public final SimpleSpatial<String> spatial;

        PhyWindow(RectFloat2D initialBounds, boolean collides) {
            super();
            pos(initialBounds);


            this.shape =
                    //PolygonShape.box(0.1f, 0.1f);
                    PolygonShape.box(initialBounds.w / 2, initialBounds.h / 2);

            FixtureDef fd = new FixtureDef(shape, 1f, 0.75f);
            if (!collides) {
                fd.filter.maskBits = 0; //no collision
            }

            fd.setRestitution(0.1f);


            W.addBody(this.body = new WallBody(initialBounds.cx(), initialBounds.cy()), fd);
            body.setLinearDamping(linearDampening);

            if (!collides) {
                body.setGravityScale(0f);
            }
        }

        public void setCollidable(boolean c) {
            W.invoke(() -> {
                body.fixtures.filter.maskBits = (c ? 0xffff : 0);
                body.setGravityScale(c ? 1f : 0f);
                body.setAwake(true);
            });
        }

        @Override
        public boolean fingerable(DragEdit d) {
            if (d == DragEdit.MOVE)
                return false; //this will be handled by box2d mousejoint

            //TODO handle other dragging interaction with box2d mousejoint
            return super.fingerable(d);
        }

        public void remove() {
            synchronized (links) {
                links.removeNode(this);
            }
            W.removeBody(this.body);
            Dyn2DSurface.this.remove(this);
        }


//        @Override
//        protected Fingering fingering(DragEdit mode) {
//            Fingering f = super.fingering(mode);
//            if (f != null) {
////                spatial.body.clearForces();
////                spatial.body.angularVelocity.zero();
////                spatial.body.linearVelocity.zero();
//            }
//            return f;
//        }


        public Pair<PhyWindow, Wire> sprout(Surface target, float scale) {
            return sprout(target, scale, 1f);
        }

        /**
         * convenience method for essentially growing a separate window
         * of a proportional size with some content (ex: a port),
         * and linking it to this window via a constraint.
         */
        public Pair<PhyWindow, Wire> sprout(Surface target, float scale, float targetAspect) {
            PhyWindow sprouted = spawn(target, scale, targetAspect);

            return pair(sprouted, link(target));
        }

        /**
         * spawns and attaches a new component to the boundary of this
         */
        public PhyWindow grow(Surface target, float scale, float targetAspect, Tuple2f normal) {

            PhyWindow x = spawn(target, scale, targetAspect);


            Tuple2f myWeldLocal, theirWeldLocal;
            RayCastInput input = new RayCastInput();
            RayCastOutput output = new RayCastOutput();
            {
                input.p2.set(0, 0);
                float r = radius() * 2;
                input.p1.set(0 + normal.x * r, 0 + normal.y * r); //looking back to center
                input.maxFraction = 1.0f;

                boolean hit = body.fixtures.raycast(output, input, 0);
                assert (hit);
                Tuple2f hitPoint = (input.p2.sub(input.p1)).scaled(output.fraction).added(input.p1);
                myWeldLocal = hitPoint;//.sub(body.pos);
            }
            {
                input.p2.set(0, 0);
                float r = x.radius() * 2;
                input.p1.set(0 - normal.x * r, 0 - normal.y * r); //looking back to center
                input.maxFraction = 1.0f;

                boolean hit = x.body.fixtures.raycast(output, input, 0);
                assert (hit);
                Tuple2f hitPoint = (input.p2.sub(input.p1)).scaled(output.fraction).added(input.p1);
                theirWeldLocal = hitPoint;
            }

            WeldJoint j = weld(x, myWeldLocal, theirWeldLocal);

            return x;
        }

        private WeldJoint weld(PhyWindow x, Tuple2f myLocal, Tuple2f theirLocal) {
            WeldJointDef jd = new WeldJointDef();
            jd.bodyA = this.body;
            jd.bodyB = x.body;
            jd.localAnchorA.set(myLocal.scaled(0.5f));
            jd.localAnchorB.set(theirLocal.scaled(0.5f));
            jd.referenceAngle = ((v2) myLocal).angle(theirLocal);
            jd.collideConnected = false;
            jd.dampingRatio = 0.5f;
            jd.frequencyHz = 0.25f;

            WeldJoint j = new WeldJoint(W.pool, jd);
            W.addJoint(j);
            return j;
        }

        public PhyWindow spawn(Surface target, float scale, float targetAspect) {
            float W = w();
            float H = h();
            float sprouterRadius = radius();
            float w = W * scale;
            float h = H * scale;

            //TODO apply targetAspect in initial size of the target


            RectFloat2D sproutSize = RectFloat2D.XYWH(0, 0, w, h);

            //min rope distance
            float minRadius = sprouterRadius + sproutSize.radius();

            float a = rng.nextFloat() * 2 * (float) Math.PI;
            float dx = cx() + (float) (minRadius * Math.cos(a));
            float dy = cy() + (float) (minRadius * Math.sin(a));

            return put(target, sproutSize.move(dx, dy, EPSILON));
        }


        /**
         * assumes the PhyWindow wraps *THE* source
         */
        public Wire link(Surface target) {
            assert (children().length == 1);
            return link(get(0), target);
        }

        public Wire unlink(Surface source, Surface target) {
            synchronized (links) {
                Wire wire = new Wire(source, target);
                NodeGraph.Node<Surface, Wire> an = links.node(wire.a);
                if (an != null) {
                    NodeGraph.Node<Surface, Wire> bn = links.node(wire.b);
                    if (bn != null) {
                        boolean removed = links.edgeRemove(new ImmutableDirectedEdge<>(
                                an, bn, wire)
                        );
                        return removed ? wire : null;
                    }
                }
                return null;
            }
        }

        /**
         * convenience method for creating a basic undirected link joint.
         * no endpoint is necessarily an owner of the other so
         * it should not matter who is the callee.
         * <p>
         * duplicate links are prevented.
         */
        public Wire link(Surface source, Surface target) {
            return link(new Wire(source, target));
        }

        /**
         * undirected link
         */
        public Wire link(Wire wire) {

            Surface aa = wire.a;
            Surface bb = wire.b;

            synchronized (links) {

                NodeGraph.MutableNode<Surface, Wire> A = links.addNode(aa);

                Iterable<ImmutableDirectedEdge<Surface, Wire>> edges = A.edges(false, true); //only need to scan the out of the src (triangular half of the graph 'matrix')
                if (edges != null) {
                    //HACK TODO use Set.contains or something
                    for (ImmutableDirectedEdge<Surface, Wire> e : edges) {
                        Wire ee = e.id;
                        if (wire.equals(ee))
                            return ee; //already exists
                    }
                }

                if (!wire.connect()) {
                    return null;
                }

                NodeGraph.MutableNode<Surface, Wire> B = links.addNode(bb);
                links.addEdge(A, wire, B);


                W.invoke(() -> {


//                {
//                    //RAW unidirectional
//                    RopeJoint ropeJoint = rope(aa, bb);
//                    ropeJoint.setData(wire);
//                }

                    {
                        //RAW unidirectional
                        //RopeJoint ropeJoint = rope(aa, bb);
                        //ropeJoint.setData(wire);
                        Snake s = snake(wire, () -> unlink(aa, bb));

                    }

//                    {
//                        //split with control widget at midpoint
//                        float scale = 0.25f;
//                        float wh =
//                                Math.max(Util.mean(aa.w(), bb.w()),
//                                        Util.mean(aa.h(), bb.h())
//                                ) * scale;
//
//                        PushButton controller;
//                        //not sure why this has to be embedded in something like Gridding
//                        addWindow(new Gridding(controller = new PushButton("o")),
//                                RectFloat2D.XYWH(
//                                        (aa.cx() + bb.cx()) / 2,
//                                        (aa.cy() + bb.cy()) / 2,
//                                        wh, wh
//                                ));
//
//                        RopeJoint ropeJointS = rope(aa, controller);
//                        ropeJointS.setData(wire);
//
//                        RopeJoint ropeJointT = rope(controller, bb);
//                        ropeJointT.setData(wire);
//
//
//                        controller.click(() -> {
//                            W.invoke(() -> {
//                                if (unlink(aa, bb) != null) {
//                                    W.removeJoint(ropeJointS);
//                                    W.removeJoint(ropeJointT);
//                                    controller.parent(PhyWindow.class).remove();
//                                }
//                            });
//                        });
//
//                    }
                });
            }

            return wire;

        }


        public void sproutBranch(String label, float scale, float childScale, Iterable<Surface> children) {
            CheckBox toggle = new CheckBox(label);
            Pair<PhyWindow, Wire> toggleWindo = sprout(toggle, scale);
            List<PhyWindow> built = new FasterList(0);
            toggle.on((cb, enabled) -> {
                W.invoke(() -> {

                    if (enabled) {
                        for (Surface x : children) {
                            built.add(toggleWindo.getOne().sprout(x, childScale).getOne());
                        }
                    } else {
                        //TODO remove what is shown
                        built.forEach(PhyWindow::remove);
                        built.clear();
                    }

                });
            });
        }

        public void sproutBranch(String label, float scale, float childScale, Supplier<Surface[]> children) {
            sproutBranch(label, scale, childScale, () -> ArrayIterator.get(children.get()));
        }

        @Override
        public boolean tangible() {
            return true;
        }

        private class WallBody extends Body2D {

            RectFloat2D physBounds = null;

            public WallBody(float cx, float cy) {
                super(new BodyDef(BodyType.DYNAMIC, new v2(cx/scaling, cy/scaling)), Dyn2DSurface.this.W);

                setData(this);

                setFixedRotation(true);
                this.physBounds = bounds;
            }

            @Override
            protected void onRemoval() {
                //body has been destroyed
                PhyWindow.this.remove();
            }

            @Override
            public boolean preUpdate() {

                RectFloat2D r = bounds;
                if (r != physBounds) {

                    if (!Util.equals(r.w, physBounds.w, SHAPE_SIZE_EPSILON) ||
                            !Util.equals(r.h, physBounds.h, SHAPE_SIZE_EPSILON)) {
                        updateFixtures((f) -> {
                            //HACK assumes the first is the only one
                            //if (f.m_shape == shape) {
                            f.setShape(
                                    shape.setAsBox(r.w / 2 / scaling, r.h / 2 / scaling)
                                    //shape.lerpAsBox(r.w / 2, r.h / 2, 0.1f)
                            );
                            //}


                        });
                    }


                    v2 target = new v2(r.cx()/ scaling, r.cy()/ scaling);

                    if (setTransform(target, 0, EPSILON))
                        setAwake(true);
                }

                return true;
            }

            @Override
            public void postUpdate() {


                Transform t = this;
                Tuple2f p = t.pos;
                //float rot = t.q.getAngle();

                float w = w(), h = h(); //HACK re-use the known width/height assumes that the physics engine cant change the shape's size

                RectFloat2D r = RectFloat2D.XYWH(p.x*scaling, p.y*scaling, w, h);
                if (!r.equals(physBounds, EPSILON)) {
                    pos(physBounds = r);
                }

            }
        }
    }
}

//    private void initMouse() {

//                        @Override
//                        public void mouseMoved(MouseEvent e) {
//                            Point p = e.getPoint();
//                            mousePosition = getPoint(p);
//                            if (!running) {
//                                repaint();
//                            }
//                        }
//                    });

//                    addMouseListener(new MouseListener() {
//                        @Override
//                        public void mouseClicked(MouseEvent e) {
//                        }
//
//                        @Override
//                        public void mousePressed(MouseEvent e) {
//                            int x = e.getX();
//                            int y = e.getY();
//                            Point p = new Point(x, y);
//                            switch (e.getButton()) {
//                                case 3:
//                                    startCenter.set(center);
//                                    clickedPoint = p;
//                                    break;
//                                case 1:
//                                    Tuple2f v = getPoint(p);
//                                    /*synchronized(Tests.this)*/
//                                {
//                                    bodyFor:
//                                    for (Body2D b = w.bodies(); b != null; b = b.next) {
//                                        for (Fixture f = b.getFixtureList(); f != null; f = f.next) {
//                                    }
//                                }
//
//                                break;
//                            }
//                        }
//
//                        @Override
//                        public void mouseReleased(MouseEvent e) {
//                            //synchronized (Tests.this) {
//                            switch (e.getButton()) {
//                                case 3:
//                                    clickedPoint = null;
//                                    break;
//                                case 1:
//                                    break;
//                            }
//                            //}
//                        }
//                    });
//        mj = null;
//        destroyMj = false;
//        mjdef = null;
//    }

