package spacegraph.space2d.widget.windo.util;

import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.event.Off;
import jcog.math.v2;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.dynamics.BodyType;
import spacegraph.space2d.phys.dynamics.Dynamics2D;
import spacegraph.space2d.widget.port.util.Wire;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.space2d.widget.windo.Link;
import spacegraph.space2d.widget.windo.Windo;

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
        return new EmptySurface(); //TODO
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
    public Link link(Wire w, Surface a, Surface b) {
        //TODO
        return new Link(w) {

        };
    }


//private class WallBody extends Body2D {
//
//    RectFloat2D physBounds = null;
//
//    WallBody(float cx, float cy) {
//        super(new BodyDef(BodyType.DYNAMIC, new v2(cx / scaling, cy / scaling)), Dyn2DSurface.this.W);
//
//        setData(this);
//
//        setFixedRotation(true);
//        this.physBounds = bounds;
//    }
//
//    @Override
//    protected void onRemoval() {
//
//        Dyn2DSurface.PhyWindow.this.remove();
//    }
//
//    @Override
//    public boolean preUpdate() {
//
//        RectFloat2D r = bounds;
//        if (r != physBounds) {
//
//            if (!Util.equals(r.w, physBounds.w, SHAPE_SIZE_EPSILON) ||
//                    !Util.equals(r.h, physBounds.h, SHAPE_SIZE_EPSILON)) {
//                updateFixtures((f) -> f.setShape(
//                        shape.setAsBox(r.w / 2 / scaling, r.h / 2 / scaling)
//
//                ));
//            }
//
//
//            v2 target = new v2(r.cx() / scaling, r.cy() / scaling);
//
//            if (setTransform(target, 0, Spatialization.EPSILONf))
//                setAwake(true);
//        }
//
//        return true;
//    }
//
//    @Override
//    public void postUpdate() {
//
//

//
//    }
//}


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
////        public PhyWindow grow(Surface target, float scale, float targetAspect, Tuple2f normal) {
////
////            PhyWindow x = spawn(target, scale, targetAspect);
////
////            W.invoke(() -> {
////                Tuple2f myWeldLocal, theirWeldLocal;
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
////                    Tuple2f hitPoint = (input.p2.sub(input.p1)).scaled(output.fraction).added(input.p1);
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
////                    Tuple2f hitPoint = (input.p2.sub(input.p1)).scaled(output.fraction).added(input.p1);
////                    theirWeldLocal = hitPoint;
////                }
////
////                WeldJoint j = weld(x, myWeldLocal, theirWeldLocal);
////
////            });
////            return x;
////        }
//
////        private WeldJoint weld(PhyWindow x, Tuple2f myLocal, Tuple2f theirLocal) {
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


}
