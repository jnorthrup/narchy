package spacegraph.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.graph.ImmutableDirectedEdge;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.NodeGraph;
import jcog.event.On;
import jcog.list.ArrayIterator;
import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.api.tuple.Pair;
import org.jbox2d.collision.RayCastInput;
import org.jbox2d.collision.RayCastOutput;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Transform;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.*;
import org.jbox2d.fracture.PolygonFixture;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;
import spacegraph.*;
import spacegraph.layout.EmptySurface;
import spacegraph.layout.Gridding;
import spacegraph.layout.Splitting;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;
import spacegraph.phys.util.Animated;
import spacegraph.render.Draw;
import spacegraph.render.SpaceGraphFlat;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.meta.SpaceLogConsole;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * wall which organizes its sub-surfaces according to 2D phys dynamics
 */
public class PhyWall extends Wall implements Animated {

    static final float SHAPE_SIZE_EPSILON = 0.0001f;

    /**
     * increase for more physics precision
     */
    final int solverIterations = 2;

    public final Dynamics2D W;
    private On on;

    /**
     * TODO use more efficient graph representation
     */
    final MapNodeGraph<Surface, Wire> links = new MapNodeGraph();

    private PhyWall() {
        super();

        W = new Dynamics2D(new v2(0, 0));
        W.setParticleRadius(0.2f);
        W.setParticleDensity(1.0f);

        W.setWarmStarting(true);
        W.setAllowSleep(true);
        W.setContinuousPhysics(true);
        //W.setSubStepping(true);
    }

    /**
     * HACK
     * note: adds development frame but this shouldnt be part of the default general-purpose window constructor
     */
    public static PhyWall window(int width, int height) {
        PhyWall s = new PhyWall();
        s.pos(-1, -1, 1, 1);

        SpaceLogConsole log = new SpaceLogConsole();
        //textGUI.text.alpha(0.5f);
        log.visible(false);

        HUDOrtho hud = new HUDOrtho();

        new SpaceGraphFlat(
                new ZoomOrtho(s) {

                    @Override
                    protected boolean maximize() {
                        return true;
                    }

                    @Override
                    public boolean autoresize() {
                        zoom(s);
                        return false;
                    }

                    @Override
                    public void log(@Nullable Object key, float duration, Level level, Supplier<String> message) {
                        if (log.visible())
                            log.log(key, duration, level, message);
                        //else: buffer?
                    }
                },
                hud
        ).show(width, height);


        hud.set(new Splitting(
                new Gridding(new EmptySurface(), new EmptySurface(), new EmptySurface(), log),
                new Gridding(new PushButton("+"),
                        //new OmniBox(),
                        new CheckBox("Log", log::visible)),
                0.1f
        ));

        return s;
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

        W.step(dt, solverIterations, solverIterations);

        return true;
    }

    @Override
    protected void paintBelow(GL2 gl) {
        super.paintBelow(gl);

        Dynamics2D w = this.W;

        long now = System.currentTimeMillis();
        for (Joint j = w.joints(); j != null; j = j.next)
            drawJoint(j, gl, now);

        for (Body2D b = w.bodies(); b != null; b = b.next())
            drawBody(b, gl);

    }

    private void drawJoint(Joint joint, GL2 g, long now) {
        Object data = joint.data();
        if (data instanceof Wire) {
            Wire w = (Wire) data;

            float activity = w.activity(now, 250);

            g.glColor4f(0.3f + activity * 0.75f, 0.3f - 0.1f * activity, 0.3f, 0.5f + 0.25f * activity);
            g.glLineWidth(15f + activity * 5f);

            Draw.line(g, w.a.cx(), w.a.cy(), w.b.cx(), w.b.cy());
            return;
        } else {

            g.glColor4f(0.9f, 0.8f, 0.1f, 0.5f);
            g.glLineWidth(10f);
        }
        Tuple2f v1 = new v2(), v2 = new v2();
        switch (joint.getType()) {
            default:
                joint.getAnchorA(v1);
                joint.getAnchorB(v2);
                break;
        }
        Draw.line(g, v1.x, v1.y, v2.x, v2.y);

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
            ((Consumer)body).accept(gl);
            return;
        }

        //boolean active = body.isActive();
        boolean awake = body.isAwake();
        gl.glColor4f(0.5f, 0.5f, 0.5f, awake ? 0.5f : 0.3f);

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

                        Draw.drawPoly(body, gl, (PolygonShape) shape);
                        break;
                    case CIRCLE:

//                                    CircleShape circle = (CircleShape) shape;
//                                    float r = circle.m_radius;
//                                    body.getWorldPointToOut(circle.m_p, v);
//                                    Point p = getPoint(v);
//                                    int wr = (int) (r * zoom);
//                                    g.fillOval(p.x - wr, p.y - wr, wr * 2, wr * 2);
                        break;
                    case EDGE:
//                                    EdgeShape edge = (EdgeShape) shape;
//                                    Tuple2f v1 = edge.m_vertex1;
//                                    Tuple2f v2 = edge.m_vertex2;
//                                    Point p1 = getPoint(v1);
//                                    Point p2 = getPoint(v2);
//                                    g.drawLine(p1.x, p1.y, p2.x, p2.y);
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
    public void stop() {
        synchronized (this) {
            on.off();
            on = null;
            super.stop();
        }
    }


    final Random rng = new XoRoShiRo128PlusRandom(1);

    float rngPolar(float scale) {
        return //2f*(rng.nextFloat()*scale-0.5f);
                (float) rng.nextGaussian() * scale;
    }

    float rngNormal(float scale) {
        return rng.nextFloat() * scale;
    }

    public PhyWindow addWindow(Surface content, RectFloat2D initialBounds) {
        PhyWindow s = new PhyWindow(initialBounds);
        //s.children(new Scale(content, 1f - Windo.resizeBorder));
        add(s);
        s.content(content);

        return s;
    }

    protected Snake snake(Surface source, Surface target) {
        Body2D from = source.parent(PhyWindow.class).body;
        Body2D to = target.parent(PhyWindow.class).body;
        assert(from!=to);
        Snake s = new Snake(from, to, 8, 0.2f, 0.05f);

        PhyWindow contW = addWindow(new PushButton("o"), RectFloat2D.mid(source.bounds, target.bounds, 0.1f));
        s.attach(contW.body, 4);

        return s;
    }

    protected RopeJoint rope(Surface source, Surface target) {

        RopeJointDef jd = new RopeJointDef(source.parent(PhyWindow.class).body, target.parent(PhyWindow.class).body);

        jd.collideConnected = true;
        jd.maxLength = Float.NaN; //should be effectively ignored by the implementation below

        RopeJoint ropeJoint = new RopeJoint(PhyWall.this.W.pool, jd) {

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

    public class PhyWindow extends Windo {
        public final Body2D body;
        private final PolygonShape shape;
//        public final SimpleSpatial<String> spatial;

        PhyWindow(RectFloat2D initialBounds) {
            super();
            pos(initialBounds);

            this.shape = PolygonShape.box(initialBounds.w / 2, initialBounds.h / 2);

            FixtureDef fd = new FixtureDef(shape, 1f, 0f);
            fd.setRestitution(0f);

            W.addBody(this.body = new WallBody(initialBounds.x, initialBounds.y), fd);

        }

        public void remove() {
            synchronized (links) {
                links.removeNode(this);
            }
            W.removeBody(this.body);
            PhyWall.this.remove(this);
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

            return addWindow(target, sproutSize.move(dx, dy, EPSILON));
        }


        /**
         * assumes the PhyWindow wraps *THE* source
         */
        public Wire link(Surface target) {
            assert (content().length == 1);
            return link(get(), target);
        }

        public Iterable<ImmutableDirectedEdge<Surface, Wire>> edges(Surface s, boolean in, boolean out) {
            NodeGraph.Node<Surface, Wire> n = links.node(s);
            return n != null ? n.edges(in, out) : Collections.emptyList();
        }

        public Wire unlink(Surface source, Surface target) {
            synchronized (links) {
                Wire wire = new Wire(source, target);
                boolean removed = links.edgeRemove(new ImmutableDirectedEdge(
                        links.node(wire.a), links.node(wire.b), wire)
                );
                return removed ? wire : null;
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

        /** undirected link */
        public Wire link(Wire wire) {

            Surface aa = wire.a;
            Surface bb = wire.b;

            synchronized (links) {

                NodeGraph.MutableNode<Surface, Wire> A = links.addNode(aa);

                Iterable<ImmutableDirectedEdge<Surface, Wire>> edges = A.edges(false, true); //only need to scan the out of the src (triangular half of the graph 'matrix')
                if (edges != null) {
                    for (ImmutableDirectedEdge<Surface, Wire> e : edges) {
                        Wire ee = e.id;
                        if (wire.equals(ee))
                            return ee; //already exists
                    }
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
                    Snake s = snake(aa, bb);
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
                synchronized (toggle) {
                    if (enabled) {
                        for (Surface x : children) {
                            built.add(toggleWindo.getOne().sprout(x, childScale).getOne());
                        }
                    } else {
                        //TODO remove what is shown
                        built.forEach(PhyWindow::remove);
                        built.clear();
                    }
                }
            });
        }

        public void sproutBranch(String label, float scale, float childScale, Supplier<Surface[]> children) {
            sproutBranch(label, scale, childScale, () -> ArrayIterator.get(children.get()));
        }


        private class WallBody extends Body2D {

            RectFloat2D physBounds = null;

            public WallBody(float cx, float cy) {
                super(new BodyDef(BodyType.DYNAMIC, new v2(cx, cy)), PhyWall.this.W);

                setData(this);

                setFixedRotation(true);
                this.physBounds = bounds;
            }


            @Override
            public void preUpdate() {


                RectFloat2D r = bounds;
                if (r != physBounds) {

                    if (!Util.equals(r.w, physBounds.w, SHAPE_SIZE_EPSILON) ||
                            !Util.equals(r.h, physBounds.h, SHAPE_SIZE_EPSILON)) {
                        updateFixtures((f) -> {
                            //HACK assumes the first is the only one
                            //if (f.m_shape == shape) {
                            f.setShape(shape.setAsBox(r.w / 2, r.h / 2));
                            //}


                        });
                    }


                    v2 target = new v2(r.cx(), r.cy());

                    if (setTransform(target, 0, EPSILON))
                        setAwake(true);
                }


            }

            @Override
            public void postUpdate() {


                Transform t = this;
                Tuple2f p = t.pos;
                //float rot = t.q.getAngle();

                float w = w(), h = h(); //HACK re-use the known width/height assumes that the physics engine cant change the shape's size

                RectFloat2D r = RectFloat2D.XYWH(p.x, p.y, w, h);
                if (!r.equals(physBounds, EPSILON)) {
                    pos(physBounds = r);
                }

            }
        }
    }

}
//    private volatile MouseJointDef mjdef;
//    private volatile MouseJoint mj;
//    private volatile boolean destroyMj = false;
//private volatile Tuple2f mousePosition = new Vec2();

//    private void initMouse() {
//                    addMouseWheelListener((MouseWheelEvent e) -> {
//                        if (e.getWheelRotation() < 0) {
//                            zoom *= 1.25f * -e.getWheelRotation();
//                        } else {
//                            zoom /= 1.25f * e.getWheelRotation();
//                        }
//
//                        zoom = Math.min(zoom, 100);
//                        zoom = Math.max(zoom, 0.1f);
//                        repaint();
//                    });

//                    addMouseMotionListener(new MouseMotionListener() {
//                        @Override
//                        public void mouseDragged(MouseEvent e) {
//                            Point p = e.getPoint();
//                            mousePosition = getPoint(p);
//                            if (clickedPoint != null) {
//                                p.x -= clickedPoint.x;
//                                p.y -= clickedPoint.y;
//                                center.x = startCenter.x - p.x / zoom;
//                                center.y = startCenter.y + p.y / zoom;
//                            } else {
//                                if (mj != null) {
//                                    mj.setTarget(mousePosition);
//                                }
//                            }
//                            if (!running) {
//                                repaint();
//                            }
//                        }
//
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
//                                            if (f.testPoint(v)) {
//                                                MouseJointDef def = new MouseJointDef();
//
//                                                def.bodyA = ground;
//                                                def.bodyB = b;
//                                                def.collideConnected = true;
//
//                                                def.target.set(v);
//
//                                                def.maxForce = 500f * b.getMass();
//                                                def.dampingRatio = 0;
//
//                                                mjdef = def;
//                                                break bodyFor;
//                                            }
//                                        }
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
//                                    if (mj != null) {
//                                        destroyMj = true;
//                                    }
//                                    break;
//                            }
//                            //}
//                        }
//                    });
//        mj = null;
//        destroyMj = false;
//        mjdef = null;
//    }

