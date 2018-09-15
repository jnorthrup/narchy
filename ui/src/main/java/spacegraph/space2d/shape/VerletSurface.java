package spacegraph.space2d.shape;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.util.animate.Animated;
import spacegraph.video.Draw;
import toxi.geom.QuadtreeIndex;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.VerletSpring2D;
import toxi.physics2d.constraints.ParticleConstraint2D;

import java.lang.ref.WeakReference;
import java.util.List;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

public class VerletSurface extends Surface implements Animated {

    private Off update;

    float timeScale = 1f;

    public VerletPhysics2D physics;

    private boolean animateWhenInvisible = false;

    /** constrained to the surface's rectangular bounds */
    private boolean bounded = true;

    public VerletSurface(float w, float h) {
        this(RectFloat2D.X0Y0WH(0,0 , w, h));
    }

    public VerletSurface() {
        this(1,1);
    }

    public VerletSurface(RectFloat2D bounds) {
        super();


        pos(bounds);

        physics = new VerletPhysics2D(null, 2, 0);
        physics.setDrag(0.05f);

        physics.setIndex(
                new QuadtreeIndex(bounds.x-1, bounds.y-1, bounds.w+1, bounds.h+1)
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
        update.off();
        update = null;
    }

    @Override
    public boolean animate(float dt) {
        if (animateWhenInvisible || showing()) {

            if (bounded)
                physics.setBounds(bounds);
            else
                physics.setBounds(null);

            physics.update(dt * timeScale);
        }
        return true;
    }

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        VerletSurface.render(physics, gl);
    }

    public VerletParticle2D addParticleBind(Surface a) {
        VerletParticle2D ap = new VerletParticle2D(a.cx(), a.cy());
        bind(a, ap, true);

        physics.addParticle(ap);
        return ap;
    }

    @Override
    public <S extends Surface> S pos(RectFloat2D next) {
        if (physics!=null)
            physics.bounds(next);
        return super.pos(next);
    }

    public ParticleConstraint2D bind(Surface a, VerletParticle2D ap, boolean surfaceMaster) {

        WeakReference<Surface> aa = new WeakReference<>(a);



        if (!surfaceMaster) {
            //pre
            ap.addBehavior(p->{
                Surface aaa = aa.get();
                p.next.set(aaa.cx(), aaa.cy());
                p.constrainAll(physics.bounds);
            });
        } else {
            ap.set(a.cx(), a.cy());
            ap.constrainAll(physics.bounds);
        }

        //post
        return ap.addConstraint(p -> {
            Surface aaa = aa.get();
            if (aaa == null) {
                physics.removeParticle(p);
            } else {
                if (surfaceMaster) {
                    p.set(aaa.cx(), aaa.cy());
                    //p.constrain(physics.bounds);
                } else
                    aaa.pos(p.x - aaa.w()/2, p.y - aaa.h()/2);
            }
        });
    }

    public final Pair<List<VerletParticle2D>, List<VerletSpring2D>> addParticleChain(VerletParticle2D x, VerletParticle2D y, int num, float strength) {
        return addParticleChain(x, y, num, Float.NaN, strength);
    }

    public Pair<List<VerletParticle2D>, List<VerletSpring2D>> addParticleChain(VerletParticle2D a, VerletParticle2D b, int num, float chainLength, float strength) {
        assert(num > 0);
        assert(a!=b);

        if (chainLength != chainLength) {
            //auto
            chainLength = a.distanceTo(b);
        }
        float linkLength = chainLength/(num+1);
        VerletParticle2D prev = a;
        FasterList pp = new FasterList(num);
        FasterList ss = new FasterList(num+1);
        for (int i = 0; i < num; i++) {
            float p = ((float) i+1) / (num + 1);
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

        return pair(pp,ss);
    }

    /** basic renderer */
    public static void render(VerletPhysics2D physics, GL2 gl) {
        for (VerletParticle2D p : physics.particles) {
            float t = 2 * p.mass();
            Draw.colorGrays(gl, 0.3f + 0.7f * Util.tanhFast(p.getSpeed()), 0.7f);
            Draw.rect(gl, p.x - t / 2, p.y - t / 2, t, t);
        }

        gl.glColor3f(0, 0.5f, 0);
        for (VerletSpring2D s : physics.springs) {
            VerletParticle2D a = s.a, b = s.b;
            gl.glLineWidth(Math.min(a.mass(), b.mass()));
            Draw.line(gl, a.x, a.y, b.x, b.y);
        }
    }
}
