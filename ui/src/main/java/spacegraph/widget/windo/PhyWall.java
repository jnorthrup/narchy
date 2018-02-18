package spacegraph.widget.windo;

import jcog.Util;
import jcog.event.On;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.MouseJoint;
import org.jbox2d.dynamics.joints.MouseJointDef;
import spacegraph.Ortho;
import spacegraph.Scale;
import spacegraph.Surface;
import spacegraph.SurfaceBase;
import spacegraph.input.Fingering;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;
import spacegraph.phys.util.Animated;

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


    public SpatialWindo newWindow(Surface content, RectFloat2D initialBounds) {
        SpatialWindo s = new SpatialWindo("w" + i.getAndIncrement(), initialBounds);
//        objects.put(s.spatial.id, s.spatial);
        add(s);
        s.children(new Scale(content, 1f - Windo.resizeBorder));
        return s;
    }

    final Map<String, SpatialWindo> spatials = new ConcurrentHashMap<>();

    class SpatialWindo extends Windo {
        private final Body2D body;
        private final PolygonShape shape;
//        public final SimpleSpatial<String> spatial;

        SpatialWindo(String id, RectFloat2D initialBounds) {

            pos(initialBounds);

            this.shape = PolygonShape.box(initialBounds.w/2, initialBounds.h/2);

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


                Transform t = getXform();
                Tuple2f p = t.pos;
                //float rot = t.q.getAngle();

                float w = w(), h = h(); //HACK re-use the known width/height assumes that the physics engine cant change the shape's size

                pos(physBounds = RectFloat2D.XYWH(p.x, p.y, w, h));

            }
        }
    }

}
