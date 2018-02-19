package spacegraph.widget.windo;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.event.On;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.MouseJoint;
import org.jbox2d.dynamics.joints.MouseJointDef;
import org.jbox2d.dynamics.joints.RopeJointDef;
import org.jbox2d.fracture.PolygonFixture;
import spacegraph.*;
import spacegraph.input.Finger;
import spacegraph.input.FingerDragging;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;
import spacegraph.phys.util.Animated;
import spacegraph.render.Draw;
import spacegraph.render.SpaceGraphFlat;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * wall which organizes its sub-surfaces according to 2D phys dynamics
 */
public class PhyWall extends Wall implements Animated {
    public static final float SHAPE_SIZE_EPSILON = 0.001f;

    final Dynamics2D W;


    private On on;

    private PhyWall() {
        super();

        W = new Dynamics2D(new v2(0, 0));
        W.setParticleRadius(0.2f);
        W.setParticleDensity(1.0f);

        W.setAllowSleep(true);
        W.setContinuousPhysics(true);
        //W.setSubStepping(true);
    }

    /** HACK */
    public static PhyWall window(int width, int height) {
        PhyWall s = new PhyWall();
        s.pos(-1,-1,1,1);

        new SpaceGraphFlat(
                new ZoomOrtho(s) {

                    @Override
                    public boolean autoresize() {
                        zoom(s);
                        return false;
                    }

                }
        ).show(width, height);

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

        W.step(dt*8, 8, 8);


        return true;
    }

    @Override
    protected void paintBelow(GL2 gl) {
        super.paintBelow(gl);

        Dynamics2D w = this.W;

        for (Joint j = w.joints(); j != null; j = j.next)
            drawJoint(j, gl);

        for (Body2D b = w.bodies(); b != null; b = b.next())
            drawBody(b, gl);

    }

    private void drawJoint(Joint joint, GL2 g) {
        g.glColor4f(0.9f, 0.8f, 0.1f, 0.75f);
        g.glLineWidth(10f);
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

                        PolygonShape poly = (PolygonShape) shape;

                        gl.glBegin(GL2.GL_TRIANGLE_FAN);
                        int n = poly.vertices;
                        Tuple2f[] pv = poly.vertex;
                        float preScale = 1.1f;

                        for (int i = 0; i < n; ++i)
                            body.getWorldPointToGL(pv[i], preScale, gl);
                        body.getWorldPointToGL(pv[0], preScale, gl); //close

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



    final Random rng = new XoRoShiRo128PlusRandom(1);
    float rngPolar(float scale) {
        return //2f*(rng.nextFloat()*scale-0.5f);
                (float) rng.nextGaussian() * scale;
    }
    float rngNormal(float scale) {
        return rng.nextFloat() * scale;
    }

    public PhyWindow newWindow(Surface content, RectFloat2D initialBounds) {
        PhyWindow s = new PhyWindow(initialBounds);
        //s.children(new Scale(content, 1f - Windo.resizeBorder));
        add(s);
        s.add(content);

        return s;
    }

    //final Map<String, PhyWindow> spatials = new ConcurrentHashMap<>();

    static class Sketching extends Surface {

        private final Path2D path;

        public Sketching(Path2D path) {
            this.path = path;
        }

        @Override
        protected void paint(GL2 gl, int dtMS) {
            if (path.points() > 1) {
                gl.glLineWidth(8);
                gl.glColor4f(0.5f, 0.5f, 1f, 0.8f);
                gl.glBegin(GL.GL_LINE_STRIP);
                path.vertex2f(gl);
                gl.glEnd();
            }
        }

    }

    interface Wireable {
        void onWireIn(@Nullable Wiring w, boolean active);
        void onWireOut(@Nullable Wiring w, boolean active);
    }

    static class Wiring extends FingerDragging {


        Path2D path;

        final Surface start;
        private Sketching pathVis;
        private Surface end = null;

        Wiring(Surface start) {
            super(0);
            this.start = start;
        }

        @Override
        public boolean start(Finger f) {
            if (super.start(f)) {

                if (this.start instanceof Wireable)
                    ((Wireable)start).onWireOut(this, true);

                return true;
            }
            return false;
        }

        @Override
        public boolean escapes() {
            return true;
        }

        @Override
        protected boolean drag(Finger f) {
            if (path == null) {
                path = new Path2D(16);
                ((Ortho)(start.root())).window.add(pathVis = new Sketching(path));
            }

            path.add(f.pos, 32);

            updateEnd(f);

            return true;
        }

        @Override
        public void stop(Finger finger) {
            if (pathVis!=null) {
                ((Ortho)(start.root())).window.remove(pathVis);
                pathVis = null;
            }

            //updateEnd(finger);

            if (this.start instanceof Wireable)
                ((Wireable)start).onWireOut(this, false);

            if (this.end instanceof Wireable)
                ((Wireable)end).onWireIn(this, false);

            start.root().debug(start, 1, ()->"WIRE: " + start + " -> " + end);
        }

        private synchronized void updateEnd(Finger finger) {
            Surface nextEnd = ((Ortho) start.root()).onTouch(finger, null);
            if (nextEnd!=end) {
                if (end instanceof Wireable) {
                    ((Wireable)end).onWireIn(this, false);
                }
                this.end = nextEnd;
                if (end instanceof Wireable) {
                    ((Wireable)end).onWireIn(this, true);
                }
            }
        }
    }

    class PhyWindow extends Windo {
        private final Body2D body;
        private final PolygonShape shape;
//        public final SimpleSpatial<String> spatial;

        PhyWindow(RectFloat2D initialBounds) {
            super();
            pos(initialBounds);

            this.shape = PolygonShape.box(initialBounds.w / 2, initialBounds.h / 2);

            FixtureDef fd = new FixtureDef(shape, 0.1f, 0f);
            fd.setRestitution(0.1f);
            W.addBody(this.body = new WallBody(), fd);

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



        class PortSurface extends Widget implements Wireable {

            protected Wiring wiringOut = null;
            protected Wiring wiringIn = null;

//            final FingerDragging dragInit = new FingerDragging(0) {
//
//                @Override
//                public void start(Finger f) {
//                    //root().debug(this, 1f, ()->"fingering " + this);
//                }
//
//                @Override
//                protected boolean drag(Finger f) {
//                    SurfaceRoot root = root();
//                    root.debug(this, 1f, ()->"drag " + this);
//                    if (f.tryFingering(this))
//                        return false;
//                    else
//                        return true;
//                }
//            };


            @Override
            protected void paintWidget(GL2 gl, RectFloat2D bounds) {

                if (wiringOut !=null) {
                    gl.glColor4f(0, 1, 0, 0.35f);
                    Draw.rect(gl, bounds);
                }
                if (wiringIn!=null) {
                    gl.glColor4f(0, 0, 1, 0.35f);
                    Draw.rect(gl, bounds);
                }


            }

            @Override
            public Surface onTouch(Finger finger, short[] buttons) {

                if (finger!=null && buttons!=null) {
                    if (finger.tryFingering(new Wiring(this)))
                        return this;
                }

//                Surface c = super.onTouch(finger, buttons);
//                if (c != null)
//                    return c;

                return this;
            }

            @Override
            public void onWireIn(@Nullable Wiring w, boolean active) {
                this.wiringIn = active ? w : null;
            }

            @Override
            public void onWireOut(@Nullable Wiring w, boolean active) {
                this.wiringOut = active ? w : null;
            }
        }

        public void newPort() {
            float w = w()/10f;
            float h = h()/10f;
            float dx = rngPolar(w/2f); //-w()/1.9f;
            float dy = rngPolar(h/2f);

            PhyWindow port = newWindow(new PortSurface(),
                    RectFloat2D.XYWH(cx() + dx, cy() + dy, w, h));

            RopeJointDef jd = new RopeJointDef(port.body, this.body);
            jd.collideConnected = true;
            jd.maxLength = Math.max(h(),w())/1.25f; //some slack
            Joint link = W.addJoint(jd);


        }



        private class WallBody extends Body2D {

            RectFloat2D physBounds = null;

            public WallBody() {
                super(new BodyDef(BodyType.DYNAMIC), PhyWall.this.W);

                setFixedRotation(true);
                this.physBounds = bounds;
            }


            @Override
            public void preUpdate() {


                RectFloat2D r = bounds;
                if (physBounds == null || r!=physBounds) {

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


                Transform t = getXform();
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
