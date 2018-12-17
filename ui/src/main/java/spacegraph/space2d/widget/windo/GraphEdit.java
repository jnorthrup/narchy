package spacegraph.space2d.widget.windo;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import jcog.data.graph.*;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.DoubleClicking;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.collection.MutableListContainer;
import spacegraph.space2d.container.collection.MutableMapContainer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.Animating;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meta.ProtoWidget;
import spacegraph.space2d.widget.meta.WizardFrame;
import spacegraph.space2d.widget.port.util.Wire;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.windo.util.Box2DGraphEditPhysics;
import spacegraph.space2d.widget.windo.util.GraphEditPhysics;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * wall which organizes its sub-surfaces according to 2D phys dynamics
 */
public class GraphEdit<S extends Surface> extends MutableMapContainer<Surface, Windo> {

    final GraphEditPhysics physics =
            //new VerletGraphEditPhysics();
            new Box2DGraphEditPhysics();

    public GraphEdit() {
        super();
        doubleClicking = new DoubleClicking(0, this::doubleClick, this);
    }

    public GraphEdit(float w, float h) {
        this(RectFloat.X0Y0WH(0, 0, w, h));
    }

    public GraphEdit(RectFloat bounds) {
        this();
        pos(bounds);
    }

    /**
     * TODO use more efficient graph representation
     */
    public final MapNodeGraph<Surface, Wire> links = new MapNodeGraph<>() {
        @Override
        protected void onRemoved(Node<Surface, Wire> r) {
            r.edges(true, true).forEach(e -> e.id().remove());
        }
    };


    /**
     * for links and other supporting geometry that is self-managed
     */
    public final MutableListContainer raw = new MutableListContainer(); /* {
        @Override
        protected boolean tangible() {
            return true;
        }
    };*/


    private final DoubleClicking doubleClicking;

    @Override
    protected void starting() {


        physics.surface = physics.start(this);

        raw.start(this);

        super.starting();

    }

    public Windo add(Surface x) {
        return add(x, (xx) -> new ManagedWindo(xx));
    }

    private void removingComponent(Surface s) {
        synchronized (links) {
            links.removeNode(s);
        }
    }


    @Override
    public void doLayout(int dtMS) {
        physics.surface.pos(bounds);
        raw.pos(bounds);
        //w.fence(bounds);
        forEach(w -> {
            if (w.parent==null)
                w.start(GraphEdit.this);
            w.layout();
        });
    }
    public @Nullable Windo get(Surface t) {
        return getValue(t);
    }

    @Override
    public Windo remove(Object key) {
        Windo w = super.remove(key);
        if (w!=null) {
            w.stop();
            return w;
        }
        return null;
    }



    /** uses put() semantics */
    public final Windo add(Surface x, Function<Surface,Windo> windowize) {
        Windo w = computeIfAbsent(x, (xx) -> {
            Windo ww = windowize.apply(xx);
            if (ww!=null && parent!=null) {
                ww.start(this);
            }
            return ww;
        }).value;
        return w;
    }

    public final Windo add(S x, float w, float h) {
        Windo y = add(x);
        y.size(w, h);
        return y;
    }

    class Debugger extends Gridding {

        private final BitmapLabel boundsInfo, children;

        {
            add(boundsInfo = new BitmapLabel());
            add(children = new BitmapLabel());

        }

        void update() {
            boundsInfo.text(GraphEdit.this.bounds.toString());

            children.text(Joiner.on("\n").join(Iterables.transform(
                    GraphEdit.this.keySet(), t -> info(t, GraphEdit.this.get(t)))));
        }

        protected String info(Surface x, Windo w) {
            return x + "\n  " + (w != null ? w.bounds : "?");
        }

    }



    public Animating<Debugger> debugger() {
        Debugger d = new Debugger();
        return new Animating<>(d, d::update, 0.25f);
    }
    @Override
    protected final void stopping() {
        physics.stop();
        raw.stop();
        super.stopping();
    }

//    @Override
//    public boolean tangible() {
//        return true;
//    }

    public final void addRaw(Surface s) {
        raw.add(s);
    }


//    @Override
//    public boolean whileEach(Predicate<Surface> o) {
//        return super.whileEach(o);
//    }
//
//    @Override
//    public boolean whileEachReverse(Predicate<Surface> o) {
//        return super.whileEachReverse(o);
//    }


    @Override
    public void forEach(Consumer<Surface> each) {
        each.accept(physics.surface);
        each.accept(raw);
        super.forEach(each);
    }


//    @Override
//    protected void paintBelow(GL2 gl, SurfaceRender r) {
//        raw.renderContents(gl, r);
//    }


    //    /**
//     * create a static box around the content, which moves along with the surface's bounds
//     */
//    public Dyn2DSurface enclose() {
//        new StaticBox(this::bounds);
//        return this;
//    }

//    private RectFloat2D bounds() {
//        return bounds;
//    }


//    public float rngPolar(float scale) {
//        return
//                (float) rng.nextGaussian() * scale;
//    }
//
//    public float rngNormal(float scale) {
//        return rng.nextFloat() * scale;
//    }

//    /**
//     * spawns in view center at the given size
//     */
//    public PhyWindow put(Surface content, float w, float h) {
//        //Ortho view = (Ortho) root();
//        return put(content, RectFloat2D.XYWH(0, 0, w, h)); //view.x(), view.y(),
//    }
//
//    public PhyWindow frame(Surface content, float w, float h) {
//        return put(new MetaFrame(content), w, h);
//    }
//
//    public PhyWindow put(Surface content, RectFloat2D initialBounds) {
//        return put(content, initialBounds, true);
//    }
//
//    private PhyWindow put(Surface content, RectFloat2D initialBounds, boolean collides) {
//        PhyWindow s = new PhyWindow(initialBounds, collides);
//
//        s.add(content);
//
//        return s;
//    }

//    private Snake snake(Wire wire, Runnable onRemove) {
//        Surface source = wire.a;
//        Surface target = wire.b;
//
//        assert (source != target);
//
//        float sa = source.bounds.area();
//        float ta = target.bounds.area();
//        float areaDiff = Math.abs(sa - ta) / (sa + ta);
//
//        int segments = Util.lerp(areaDiff, 8, 6);
//
//        float EXPAND_SCALE_FACTOR = 4;
//
//        PushButton deleteButton = new PushButton("x");
//        Surface menu = new TabPane(Map.of("o", () -> new Gridding(
//                new VectorLabel(source.toString()),
//                new VectorLabel(target.toString()),
//                deleteButton
//        )), (l) -> new CheckBox(l) {
//            @Override
//            protected String label(String text, boolean on) {
//                return text;
//            }
//
//            @Override
//            public ToggleButton set(boolean expanded) {
//
//                super.set(expanded);
//
//                synchronized (wire) {
//
//                    PhyWindow w = parent(PhyWindow.class);
//                    if (w == null)
//                        return this;
//                    float cx = w.cx();
//                    float cy = w.cy();
//                    float ww, hh;
//                    if (expanded) {
//
//                        ww = w.w() * EXPAND_SCALE_FACTOR;
//                        hh = w.h() * EXPAND_SCALE_FACTOR;
//                    } else {
//
//                        ww = w.w() / EXPAND_SCALE_FACTOR;
//                        hh = w.h() / EXPAND_SCALE_FACTOR;
//                    }
//                    w.pos(cx - ww / 2, cy - hh / 2, cx + ww / 2, cy + hh / 2);
//                }
//
//                return this;
//            }
//        });
//
//        PhyWindow menuBody = put(menu,
//                RectFloat2D.mid(source.bounds, target.bounds, 0.1f));
//
//        float mw = menuBody.radius();
//
////        Snake s = new Snake(source, target, segments, 1.618f * 2 * mw, mw) {
////
////            @Override
////            public void remove() {
////                onRemove.run();
////                super.remove();
////            }
////        };
//
//
//        //s.attach(menuBody.body, segments / 2 - 1);
//
//        deleteButton.click(s::remove);
//
//        int jj = 0;
//        for (Joint j : s.joints) {
//
//            float p = ((float) jj) / (segments - 1);
//
//
//            j.setData((ObjectLongProcedure<GL2>) (g, now) -> {
//
//                int TIME_DECAY_MS = 250;
//                boolean side = p < 0.5f;
//                float activity =
//                        wire.activity(side, now, TIME_DECAY_MS);
//
//
//                int th = wire.typeHash(side);
//                if (th == 0) {
//                    g.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
//                } else {
//                    Draw.colorHash(g, th, 0.9f, 0.5f + 0.5f * activity, 0.5f + 0.4f * activity);
//                }
//
//                g.glLineWidth(10f + activity * 10f);
//
//
//            });
//            jj++;
//        }
//
//        return s;
//    }

    public Iterable<FromTo<Node<spacegraph.space2d.Surface, Wire>, Wire>> edges(Surface s) {
        Node<spacegraph.space2d.Surface, Wire> n = links.node(s);
        return n != null ? n.edges(true, true) : List.of();
    }


    @Override
    public Surface finger(Finger finger) {

        Surface s = super.finger(finger);
//        if (s != null && s != this && !(s instanceof PhyWindow))
//            return s;

        if (s == null || s == raw) {
            if (doubleClicking.update(finger))
                return this;
        } else {
            doubleClicking.reset();
        }


//        if (finger.tryFingering(jointDrag))
//            return this;


        return s != null ? s : null;

    }

    protected void doubleClick(v2 pos) {
        float h = 100;
        float w = 100;
        Windo z = add(
                new WizardFrame(new ProtoWidget()) {
                    @Override
                    protected void become(Surface next) {
                        super.become(next);

                        //GraphEdit pp = parent(GraphEdit.class);
//                        if (next instanceof ProtoWidget) {
//                            pp.setCollidable(false);
//                        } else {
//                            pp.setCollidable(true);
//                        }

                    }
                });
        z.pos(RectFloat.XYWH(pos.x, pos.y, w, h));
        z.root().zoom(z);
    }

//    public void removeRaw(Surface x) {
//        raw.remove(x);
//    }

//    public Windo sprout(S from, S toAdd, float scale) {
//        Windo to = add(toAdd);
//        to.pos(RectFloat.XYWH(from.cx(), from.cy(), from.w() * scale, from.h() * scale));
//
//        VerletParticle2D toParticle = physics.bind(to, VerletSurface.VerletSurfaceBinding.Center, false);
//        toParticle.addBehaviorGlobal(new AttractionBehavior2D<>(toParticle, 100 /* TODO auto radius*/, -20));
//
//        VerletParticle2D fromParticle = physics.bind(from, VerletSurface.VerletSurfaceBinding.NearestSurfaceEdge);
//
//
//        physics.physics.addSpring(new VerletSpring2D(fromParticle, toParticle, 10, 0.5f));
//
////        cable(from, fromParticle, to, toParticle);
//
//        return to;
//    }


    /**
     * returns the grip window
     */
    public Link link(Wire w) {
        Surface a = w.a;
        Surface b = w.b;
        return link(w, a, b);
    }


    private Link link(Wire w, Surface a, Surface b) {
        if (w == null)
            w = new Wire(a, b);
        return physics.link(w, a, b);
    }


    /**
     * undirected link
     */
    @Nullable
    public Wire addWire(final Wire wire) {

        Surface aa = wire.a, bb = wire.b;

        synchronized (links) {


            NodeGraph.MutableNode<Surface, Wire> A = links.addNode(aa);
            NodeGraph.MutableNode<Surface, Wire> B = links.addNode(bb);
            if (!links.addEdge(A, wire, B)) {
                //already exists
                return null;
            }

//            Iterable<FromTo<Node<spacegraph.space2d.Surface, Wire>, Wire>> edges = A.edges(false, true);
//            if (edges != null) {
//
//                for (FromTo<Node<spacegraph.space2d.Surface, Wire>, Wire> e : edges) {
//                    Wire ee = e.id();
//                    if (wire.equals(ee))
//                        return ee;
//                }
//            }

            if (!wire.connect()) {
                return null;
            }


//                W.invoke(() -> {
//
//
//                    {
//
//
//                        Snake s = snake(wire, () -> unlink(aa, bb));
//
//                    }
//
//
//                });
        }

        link(wire);

        return wire;

    }

    protected Wire removeWire(Surface source, Surface target) {
        synchronized (links) {
            Wire wire = new Wire(source, target);
            Node<spacegraph.space2d.Surface, Wire> an = links.node(wire.a);
            if (an != null) {
                Node<spacegraph.space2d.Surface, Wire> bn = links.node(wire.b);
                if (bn != null) {
                    boolean removed = links.edgeRemove(new ImmutableDirectedEdge<>(
                            an, wire, bn)
                    );
                    if (removed) {
                        //TODO log( unwire(..) )
                    }
                    return removed ? wire : null;
                }
            }
            return null;
        }
    }


    class ManagedWindo extends Windo {

        private final Surface content;


        public ManagedWindo(Surface content) {
            super(new Scale(new MetaFrame(content), 0.98f));
            this.content = content;
        }

        @Override
        protected void starting() {
            super.starting();
            physics.add(this);
        }

        @Override
        protected void stopping() {
            physics.remove(this);

            //remove any associated links, recursively
            if (content instanceof Container) {
                ((Container) content).forEachRecursively(GraphEdit.this::removingComponent);
            } else {
                removingComponent(content);
            }

            super.stopping();
        }
    }
}










































/*
     content = new Graph2D<>();
                //.render()...
        W.setParticleRadius(0.2f);
        W.setParticleDensity(1.0f);

        W.setWarmStarting(true);
        W.setAllowSleep(true);
        W.setContinuousPhysics(true);


 */

//    public final Dynamics2D W = new Dynamics2D(new v2(0, 0));
//    public final Random rng = new XoRoShiRo128PlusRandom(1);
//
//    private FingerDragging jointDrag = new FingerDragging(MOUSE_JOINT_BUTTON) {
//
//        final Body2D ground = W.addBody(new BodyDef(BodyType.STATIC),
//                new FixtureDef(PolygonShape.box(0, 0), 0, 0).noCollide());
//
//        private volatile MouseJoint mj;
//
//        @Override
//        protected boolean startDrag(Finger f) {
//            if (super.startDrag(f)) {
//                Body2D touched2D;
//                if (((touched2D = pick(f)) != null)) {
//                    MouseJointDef def = new MouseJointDef();
//
//                    def.bodyA = ground;
//                    def.bodyB = touched2D;
//                    def.collideConnected = true;
//
//
//                    def.target.set(f.pos);
//
//                    def.maxForce = 500f * touched2D.getMass();
//                    def.dampingRatio = 0;
//
//                    mj = (MouseJoint) W.addJoint(new MouseJoint(W.pool, def));
//                    return true;
//                }
//            }
//            return false;
//        }
//
//
//        Body2D pick(Finger ff) {
//            v2 p = ff.pos.scale(scaling);
//
//
//            float w = 0;
//            float h = 0;
//
//
//            final Fixture[] found = {null};
//            W.queryAABB((Fixture f) -> {
//                if (f.body.type != BodyType.STATIC &&
//                        f.filter.maskBits != 0 /* filter non-colllidables */ && f.testPoint(p)) {
//                    found[0] = f;
//                    return false;
//                }
//
//                return true;
//            }, new AABB(new v2(p.x - w, p.y - h), new v2(p.x + w, p.y + h), false));
//
//
//
//
//
//
//
//
//
//
//
//
//            return found[0] != null ? found[0].body : null;
//        }
//
//        @Override
//        public void stop(Finger finger) {
//            super.stop(finger);
//            if (mj != null) {
//                W.removeJoint(mj);
//                mj = null;
//            }
//        }
//
//        @Override
//        protected boolean drag(Finger f) {
//            if (mj != null) {
//                v2 p = f.pos.scale(scaling);
//
//                /*if (clickedPoint != null)*/
//
//
//
//
//                mj.setTarget(p);
//            }
//
//
//
//
//
//
//
//            return true;
//
//        }
//
//    };

//    protected RopeJoint rope(Surface source, Surface target) {
//
//        RopeJointDef jd = new RopeJointDef(source.parent(PhyWindow.class).body, target.parent(PhyWindow.class).body);
//
//        jd.collideConnected = true;
//        jd.maxLength = Float.NaN;
//
//        RopeJoint ropeJoint = new RopeJoint(Dyn2DSurface.this.W.pool, jd) {
//
//            float lengthScale = 2.05f;
//
//            @Override
//            public float targetLength() {
//
//
//                return ((source.radius() + target.radius()) * lengthScale)
//
//                        ;
//
//
//            }
//        };
//
//
//        W.addJoint(ropeJoint);
//        return ropeJoint;
//    }
//

//    @Override
//    protected void paintBelow(GL2 gl) {
//        super.paintBelow(gl);
//
//        Dynamics2D w = this.W;
//
//        long now = System.currentTimeMillis();
//
//
//        w.joints(j -> drawJoint(j, gl, now));
//
//        w.bodies(b -> drawBody(b, gl));
//
//        drawParticleSystem(gl, w.particles);
//
//
//    }
//
//    private void drawParticleSystem(GL2 gl, ParticleSystem system) {
//
//        int particleCount = system.getParticleCount();
//        if (particleCount != 0) {
//            float particleRadius = system.getParticleRadius();
//            Tuple2f[] positionBuffer = system.getParticlePositionBuffer();
//            ParticleColor[] colorBuffer = null;
//            if (system.m_colorBuffer.data != null) {
//                colorBuffer = system.getParticleColorBuffer();
//            }
//
//
//            Draw.particles(gl, positionBuffer, particleRadius, 6, colorBuffer, particleCount);
//
//        }
//    }
//
//    private void drawJoint(Joint joint, GL2 g, long now) {
//        Object data = joint.data();
//        if (data instanceof ObjectLongProcedure) {
//            ((ObjLongConsumer) data).accept(g, now);
//        } else {
//
//            Draw.colorHash(g, joint.getClass().hashCode(), 0.5f);
//            g.glLineWidth(10f);
//        }
//        Tuple2f v1 = new v2(), v2 = new v2();
//        switch (joint.getType()) {
//            default:
//                joint.getAnchorA(v1);
//                joint.getAnchorB(v2);
//                break;
//        }
//        Draw.line(g, v1.x * scaling, v1.y * scaling, v2.x * scaling, v2.y * scaling);
//
//    }
//
//    private void drawBody(Body2D body, GL2 gl) {
//
//
////        if (body.data() instanceof PhyWindow.WallBody) {
////            return;
////        }
//        if (body instanceof Consumer) {
//            ((Consumer) body).accept(gl);
//            return;
//        }
//
//
//        boolean awake = body.isAwake();
//        gl.glColor4f(0.5f, 0.5f, 0.5f, awake ? 0.75f : 0.65f);
//
//
//        for (Fixture f = body.fixtures; f != null; f = f.next) {
//            PolygonFixture pg = f.polygon;
//            if (pg != null) {
//
//            } else {
//                Shape shape = f.shape();
//                switch (shape.m_type) {
//                    case POLYGON:
//                        Draw.poly(body, gl, scaling, (PolygonShape) shape);
//                        break;
//                    case CIRCLE:
//
//                        CircleShape circle = (CircleShape) shape;
//                        float r = circle.radius;
//                        v2 v = new v2();
//                        body.getWorldPointToOut(circle.center, v);
//                        v.scale(scaling);
//
//
//                        Draw.circle(gl, v, true, r * scaling, 9);
//                        break;
//                    case EDGE:
//                        EdgeShape edge = (EdgeShape) shape;
//                        Tuple2f p1 = edge.m_vertex1;
//                        Tuple2f p2 = edge.m_vertex2;
//                        gl.glLineWidth(4f);
//                        Draw.line(gl, p1.x * scaling, p1.y * scaling, p2.x * scaling, p2.y * scaling);
//                        break;
//                }
//            }
//        }
//
//
//    }
//class StaticBox {
//
//    private final Body2D body;
//    private final Fixture bottom;
//    private final Fixture top;
//    private final Fixture left;
//    private final Fixture right;
//
//    StaticBox(Supplier<RectFloat2D> bounds) {
//
//        float w = 1, h = 1, thick = 0.5f;
//
//        body = W.addBody(new Body2D(new BodyDef(BodyType.STATIC), W) {
//            @Override
//            public boolean preUpdate() {
//                update(bounds.get());
//                synchronizeFixtures();
//                return true;
//            }
//        });
//        bottom = body.addFixture(
//                new FixtureDef(PolygonShape.box(w / 2 - thick / 2, thick / 2),
//                        0, 0)
//        );
//        top = body.addFixture(
//                new FixtureDef(PolygonShape.box(w / 2 - thick / 2, thick / 2),
//                        0, 0)
//        );
//        left = body.addFixture(
//                new FixtureDef(PolygonShape.box(thick / 2, h / 2 - thick / 2),
//                        1, 0)
//        );
//        right = body.addFixture(
//                new FixtureDef(PolygonShape.box(thick / 2, h / 2 - thick / 2),
//                        1, 0)
//        );
//
//
//    }
//
//    void update(RectFloat2D bounds) {
//
//        body.updateFixtures(f -> {
//
//            float cx = bounds.cx() / scaling;
//            float cy = bounds.cy() / scaling;
//            float thick = Math.min(bounds.w, bounds.h) / 16f / scaling;
//
//            float W = bounds.w / scaling;
//            float H = bounds.h / scaling;
//            ((PolygonShape) top.shape).setAsBox(W, thick, new v2(cx / 2, +H), 0);
//            ((PolygonShape) right.shape).setAsBox(thick, H, new v2(+W, cy / 2), 0);
//            ((PolygonShape) bottom.shape).setAsBox(W, thick, new v2(cx, 0), 0);
//            ((PolygonShape) left.shape).setAsBox(thick, H, new v2(0, cy), 0);
//        });
//
//    }
//}
