package spacegraph.space2d.widget.shape;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.util.animate.Animated;
import spacegraph.video.Draw;
import toxi.geom.QuadtreeIndex;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.constraint.ParticleConstraint2D;
import toxi.physics2d.spring.VerletSpring2D;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

public class VerletSurface extends PaintSurface implements Animated {

    private Off update;

    float timeScale = 1f;

    public VerletPhysics2D physics;

    public final AtomicBoolean debugRender = new AtomicBoolean(true);

    public VerletSurface(float w, float h) {
        this(RectFloat.X0Y0WH((float) 0, (float) 0, w, h));
    }

    public VerletSurface() {
        this(1.0F, 1.0F);
    }

    public VerletSurface(RectFloat bounds) {
        super();


        pos(bounds);

        physics = new VerletPhysics2D(2, (float) 0);
//        physics.setDrag(0.05f);

        physics.setIndex(
                new QuadtreeIndex(bounds.x - 1.0F, bounds.y - 1.0F, bounds.w + 1.0F, bounds.h + 1.0F)
                //new RTreeQuadTree()
        );

//            physics.addBehavior(new GravityBehavior2D(new Vec2D(0, 0.1f)));
    }

    @Override
    protected void starting() {
        update = root().animate(this);
    }

    @Override
    protected void stopping() {
        update.close();
        update = null;
    }

    @Override
    public boolean animate(float dt) {
        boolean animateWhenInvisible = false;
        if (animateWhenInvisible || showing()) {

            /**
             * constrained to the surface's rectangular bounds
             */
            boolean bounded = true;
            if (bounded)
                physics.setBounds(bounds);
            else
                physics.setBounds(null);

            physics.update(dt * timeScale);
        }
        return true;
    }

    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {
        if (debugRender.getOpaque())
            VerletSurface.render(physics, gl);
    }

    public enum VerletSurfaceBinding {

        Center {
            @Override
            Vec2D targetVerlet(VerletParticle2D particle, Surface s) {
                return new Vec2D(s.cx(), s.cy());
            }

            @Override
            public Vec2D targetSurface(VerletParticle2D p, Surface ss) {
                return new Vec2D(p.x, p.y);
            }
        },
        NearestSurfaceEdge {
            @Override
            Vec2D targetVerlet(VerletParticle2D p, Surface ss) {
                float px = p.x;
                float L = ss.left();
                float distLeft = Math.abs(px - L);
                float R = ss.right();
                float distRight = Math.abs(px - R);
                float distLR = Math.min(distLeft, distRight);
                float py = p.y;
                float T = ss.bottom();
                float distTop = Math.abs(py - T);
                float B = ss.top();
                float distBottom = Math.abs(py - B);
                float distTB = Math.min(distTop, distBottom);

                if (distLR < distTB) {
                    //along either left or right
                    return new Vec2D((distLeft < distRight) ? L : R, Util.clamp(py, T, B));
                } else {
                    //along either top or bottom
                    return new Vec2D(Util.clamp(px, L, R), (distTop < distBottom) ? T : B);
                }
//
//                float x = px < ss.cx() ? ss.left() : ss.right();
//                float y = py < ss.cy() ? ss.top() : ss.bottom();
//                return new Vec2D(x, y);
            }

            @Override
            public Vec2D targetSurface(VerletParticle2D p, Surface ss) {
//                if (p.distanceTo(new Vec2D(ss.cx(), ss.cy())) > Math.max(ss.w(), ss.h()))
//                    return new Vec2D(p.x, p.y); //TODO actually relative to nearest border point
                return null;
            }
        };

        abstract Vec2D targetVerlet(VerletParticle2D particle, Surface s);

        public abstract @Nullable Vec2D targetSurface(VerletParticle2D p, Surface ss);
    }


    public VerletParticle2D bind(Surface a, VerletSurfaceBinding b) {
        return bind(a, b, true);
    }

    public VerletParticle2D bind(Surface a, VerletSurfaceBinding b, boolean surfaceOverrides) {
        VerletParticle2D ap = new VerletParticle2D(a.cx(), a.cy());
        ap.constrainAll(physics.bounds);
        bind(a, ap, surfaceOverrides, b);

        physics.addParticle(ap);
        return ap;
    }

    @Override
    public <S extends Surface> S pos(RectFloat next) {
        if (physics != null)
            physics.bounds(next);
        return super.pos(next);
    }

    public ParticleConstraint2D bind(Surface s, VerletParticle2D v, boolean surfaceOverrides, VerletSurfaceBinding b) {

        WeakReference<Surface> wrs = new WeakReference<Surface>(s);


        v.addBehavior((vv) -> {
            Surface ss = wrs.get();
            if (ss == null || ss.parent == null) {
                vv.delete();
                return;
            }

            Vec2D pNext = b.targetVerlet(vv, ss);
            if (pNext != null) {
                //p.next.setAt(pNext);
                //System.out.println(vv.id + " " + vv.x + "," + vv.y);


                //gradual
//                    float force = 5.5f;
//                    vv.addForce(pNext.sub(vv).normalize().scaleSelf(force));

                //immediate
                vv.next.set(pNext);

                float density = 0.01f;
                vv.mass = ss.bounds.area() * density;

//                    vv.setAt(pNext);
//                    vv.prev.setAt(pNext);
                //vv.next.setAt(pNext);
            }
        });


        v.set(b.targetVerlet(v, s));
        v.constrainAll(physics.bounds);
        v.next.set(v);
        v.prev.set(v);


        if (!surfaceOverrides) {

            //pre
            v.addConstraint(vv -> {
                Surface ss = wrs.get();
//                vv.next.setAt(b.targetVerlet(vv, ss));
//                vv.constrainAll(physics.bounds);

                if (ss == null) {
                    physics.removeParticle(vv);
                    return;
                }

                Vec2D sNext = b.targetSurface(vv, ss);
                if (sNext != null) {
                    //ss.pos(Util.lerp(0.5f, sNext.x, ss.x()))
                    //ss.pos(RectFloat2D.XYWH(sNext.x, sNext.y, ss.w(), ss.h()));
                    //ss.pos(ss.bounds.posLerp(sNext.x, sNext.y, 0.75f));
                    ss.pos(ss.bounds.posLerp(sNext.x, sNext.y, 1f));
                }
//            } else {

//                Vec2D pNext = b.targetVerlet(vv, ss);
//                if (pNext != null) {
//                    //p.next.setAt(pNext);
//                    //float speed = 0.05f;
////                        System.out.println(vv.id + " " + vv.x + "," + vv.y);
//                    //vv.addForce(pNext.sub(vv).normalize().scaleSelf(speed));
////                    vv.clearForce();
////                    vv.clearVelocity();
//                    vv.next.setAt(pNext);
//                    vv.prev.setAt(pNext);
//                    vv.setAt(pNext);
//
//                }
            });
        }
        return null;
    }

    public final Pair<List<VerletParticle2D>, List<VerletSpring2D>> addParticleChain(VerletParticle2D x, VerletParticle2D y, int num, float strength) {
        return addParticleChain(x, y, num, Float.NaN, strength);
    }

    public Pair<List<VerletParticle2D>, List<VerletSpring2D>> addParticleChain(VerletParticle2D a, VerletParticle2D b, int num, float chainLength, float strength) {
        assert (num > 0);
        assert (a != b);

        if (chainLength != chainLength) {
            //auto
            chainLength = a.distanceTo(b);
        }
        float linkLength = chainLength / (float) (num + 1);
        VerletParticle2D prev = a;
        FasterList pp = new FasterList(num);
        FasterList ss = new FasterList(num + 1);
        for (int i = 0; i < num; i++) {
            float p = ((float) i + 1.0F) / (float) (num + 1);
            VerletParticle2D next =
                    new VerletParticle2D(
                            Util.lerp(p, a.x, b.x),
                            Util.lerp(p, a.y, b.y)
                    );
            next.mass(Util.lerp(p, a.mass(), b.mass()));
            pp.add(next);
            physics.addParticle(next);
            VerletSpring2D s = new VerletSpring2D(prev, next, linkLength, strength);
            ss.add(s);
            physics.addSpring(s);
            prev = next;
        }
        {
            VerletSpring2D s = new VerletSpring2D(prev, b, linkLength, strength);
            ss.add(s);
            physics.addSpring(s);
        }

        return pair(pp, ss);
    }

    /**
     * basic renderer
     */
    public static void render(VerletPhysics2D physics, GL2 gl) {
        for (VerletParticle2D p : physics.particles) {
            float t = 2.0F * p.mass();
            Draw.colorGrays(gl, 0.3f + 0.7f * Util.tanhFast(p.getSpeed()), 0.7f);
            Draw.rect(p.x - t / 2.0F, p.y - t / 2.0F, t, t, gl);
        }

        gl.glColor3f((float) 0, 0.5f, (float) 0);
        for (VerletSpring2D s : physics.springs) {
            VerletParticle2D a = s.a, b = s.b;
            gl.glLineWidth(Math.min(a.mass(), b.mass()));
            Draw.line(a.x, a.y, b.x, b.y, gl);
        }
    }
}
