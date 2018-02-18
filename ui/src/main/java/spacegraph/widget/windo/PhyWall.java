package spacegraph.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.event.On;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.*;
import org.jbox2d.fracture.PolygonFixture;
import spacegraph.Ortho;
import spacegraph.Scale;
import spacegraph.Surface;
import spacegraph.SurfaceBase;
import spacegraph.input.Fingering;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;
import spacegraph.phys.util.Animated;
import spacegraph.render.Draw;
import spacegraph.widget.slider.XYSlider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * wall which organizes its sub-surfaces according to 2D phys dynamics
 */
public class PhyWall extends Wall implements Animated {
    public static final float SHAPE_SIZE_EPSILON = 0.001f;

    /** set to smaller values to make more precise with respect to Box2D's default configuration */
    static final float WORLD_SCALE =
            1f;
            //1/100f;

    final Dynamics2D W;


    private On on;

    public PhyWall() {
        super();
        W = new Dynamics2D(new v2(0, 0));
        W.setParticleRadius(0.2f);
        W.setParticleDensity(1.0f);

        W.setAllowSleep(true);
        W.setContinuousPhysics(true);
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
    protected void paintBelow(GL2 gl) {
        super.paintBelow(gl);
        //vykresli tuhe telesa
        Dynamics2D w = this.W;
        for (Body2D b = w.bodies(); b != null; b = b.next()) {
            drawBody(b, gl);
        }

        //vykresli joiny
        for (Joint j = w.joints(); j != null; j = j.next) {
            drawJoint(j, gl);
        }
    }

    private void drawJoint(Joint joint, GL2 g) {
        g.glColor3f(0.6f, 1f, 0.1f);
        g.glLineWidth(10f);
        Tuple2f v1 = new Vec2();
        Tuple2f v2 = new Vec2();
        switch (joint.getType()) {
            case DISTANCE:
                DistanceJoint dj = (DistanceJoint) joint;
                v1 = joint.getBodyA().getWorldPoint(dj.getLocalAnchorA());
                v2 = joint.getBodyB().getWorldPoint(dj.getLocalAnchorB());
                break;
            case ROPE:
                RopeJoint rj = (RopeJoint) joint;
                v1 = joint.getBodyA().getWorldPoint(rj.getLocalAnchorA());
                v2 = joint.getBodyB().getWorldPoint(rj.getLocalAnchorB());
                break;
            case MOUSE:
                MouseJoint localMj = (MouseJoint) joint;
                localMj.getAnchorA(v1);
                localMj.getAnchorB(v2);
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
        gl.glColor3f(0.5f, 0.5f, 0.5f);

        Tuple2f v = new v2();
        //List<PolygonFixture> generalPolygons = new FasterList<>();
        for (Fixture f = body.fixtures; f != null; f = f.next) {
            PolygonFixture pg = f.polygon;
            if (pg != null) {
                //generalPolygons.add(pg);
            } else {
                Shape shape = f.shape();
                switch (shape.m_type) {
                    case POLYGON:
                        PolygonShape poly = (PolygonShape) shape;
                        gl.glBegin(GL2.GL_TRIANGLE_FAN);
                        int n = poly.vertices;
                        for (int i = 0; i <= n; ++i) {
                            boolean done = false;
                            if (i == n) {
                                i = 0;
                                done = true;
                            }
                            body.getWorldPointToOut(poly.vertex[i], v);
                            gl.glVertex2f(v.x, v.y);
                            if (done)
                                break;
                        }
                        gl.glEnd();
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


    private volatile MouseJointDef mjdef;
    private volatile MouseJoint mj;
    private volatile boolean destroyMj = false;
    private volatile Tuple2f mousePosition = new Vec2();

    private void initMouse() {
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
        mj = null;
        destroyMj = false;
        mjdef = null;
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


    public PhyWindow newWindow(Surface content, RectFloat2D initialBounds) {
        PhyWindow s = new PhyWindow("w" + i.getAndIncrement(), initialBounds);
//        objects.put(s.spatial.id, s.spatial);
        add(s);
        s.children(new Scale(content, 1f - Windo.resizeBorder));
        return s;
    }

    final Map<String, PhyWindow> spatials = new ConcurrentHashMap<>();

    class PhyWindow extends Windo {
        private final Body2D body;
        private final PolygonShape shape;
//        public final SimpleSpatial<String> spatial;

        PhyWindow(String id, RectFloat2D initialBounds) {

            pos(initialBounds);

            this.shape = PolygonShape.box(initialBounds.w/2*WORLD_SCALE, initialBounds.h/2*WORLD_SCALE);

            FixtureDef fd = new FixtureDef(shape, 0.1f, 0f);
            fd.setRestitution(0f);

            BodyDef bd = new BodyDef();
            bd.type = BodyType.DYNAMIC;
            Body2D body = new MyBody2D(bd);

            this.body = body;

            spatials.put(id, this);

            W.invokeLater(()->{
                W.newBody(body);
                body.addFixture(fd);
            });

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

        public void newPort() {
            float w = w()/10f;
            float h = h()/10f;
            float dx = -w()/1.9f;
            float dy = -h()/1.9f;
            PhyWindow port = newWindow(new XYSlider(), RectFloat2D.XYWH(cx() + dx, cy() + dy, w, h));

            RopeJointDef jd = new RopeJointDef();
            jd.collideConnected = true;
            jd.bodyA = port.body;
            jd.bodyB = this.body;
            jd.maxLength = Math.max(h(),w())/1.5f;
            Joint link = W.newJoint(jd);


        }


        private class MyBody2D extends Body2D {

            RectFloat2D physBounds = null;


            public MyBody2D(BodyDef bd) {
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
                                    f.setShape(shape.setAsBox(r.w / 2*WORLD_SCALE, r.h / 2*WORLD_SCALE));
                                //}


                            });
                        }


                    }

//                    Tuple2f p = getWorldCenter();
//                    float px = p.x;
//                    float py = p.y;
                    //setLinearVelocity(new v2(r.x - px, r.y - py));
                    v2 target = new v2(r.cx()*WORLD_SCALE, r.cy()*WORLD_SCALE);
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


                Transform t = getXform();
                Tuple2f p = t.pos;
                //float rot = t.q.getAngle();

                float w = w(), h = h(); //HACK re-use the known width/height assumes that the physics engine cant change the shape's size

                pos(physBounds = RectFloat2D.XYWH(p.x/WORLD_SCALE, p.y/WORLD_SCALE, w, h));

            }
        }
    }

}
