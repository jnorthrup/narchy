package nars.experiment;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.learn.ql.HaiQAgent;
import jcog.signal.ArrayBitmap2D;
import jcog.tree.rtree.rect.RectFloat2D;
import nars.*;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.gui.Vis;
import nars.index.term.map.CaffeineIndex;
import nars.op.ArithmeticIntroduction;
import nars.op.RLBooster;
import nars.op.stm.ConjClustering;
import nars.term.Term;
import nars.util.signal.Bitmap2DSensor;
import nars.video.CameraSensorView;
import spacegraph.layout.Gridding;
import spacegraph.render.Draw;
import spacegraph.widget.meta.AutoSurface;

import static nars.Op.BELIEF;
import static spacegraph.render.JoglSpace.window;

/* 1D and 2D grid tracking */
public class TrackXY extends NAgent {

    final ArrayBitmap2D view;

    //target position, to reach
    float tx, ty;

    //source position, to control
    float sx, sy;
    public Bitmap2DSensor cam;


    private float controlSpeed = 1f, targetSpeed = 0.5f;
    private float visionContrast = 0.9f;

    public static void main(String[] args) {

        float fps = 40;

        boolean nars = true;
        boolean rl = false;

        NARS nb = NARS
                .realtime(fps)
                .index(
                    //new HijackConceptIndex(4 * 1024, 4)
                    new CaffeineIndex(8*1024)
                );



        NAR n = nb.get();

        n.termVolumeMax.set(36);
        n.conceptActivation.set(0.9f);
        n.forgetRate.set(0.75f);

        TrackXY t = new TrackXY(6, 6);
        n.on(t);

        n.time.synch(n);



        if (nars) {
            Deriver d = new Deriver(Derivers.rules(1, 8, n), n);
            ConjClustering cj = new ConjClustering(n, BELIEF, (tt)->true, 8, 64);
            ArithmeticIntroduction ai = new ArithmeticIntroduction(32,n);
            window(new Gridding(
                    new AutoSurface(d),
                    new AutoSurface(cj),
                    new AutoSurface(ai)
            ), 400, 300);
        }
        window(Vis.top(n), 800, 250);
        NAgentX.chart(t);
        window(new CameraSensorView(t.cam, n) {
            @Override
            protected void paint(GL2 gl, int dtMS) {
                super.paint(gl, dtMS);
                RectFloat2D at = cellRect(t.sx, t.sy, 0.5f, 0.5f);
                gl.glColor4f(1, 0, 0, 0.9f);
                Draw.rect(gl, at);
            }
        }, 800, 800);

        n.startFPS(fps);
        t.runFPS(fps);

        if (rl) {
            new RLBooster(t,
                    //DQN::new,
                    HaiQAgent::new,
                    //RandomAgent::new,
                    1);
        }

        //n.log();

    }

    protected TrackXY(int x, int y) {
        super("trackXY", null);
        this.view = new ArrayBitmap2D(x, y);
    }

    @Override
    protected void start(NAR nar) {
        super.start(nar);

        if (view.height() > 1) {
            actionToggle($.the("up"), () -> {
                sy = Util.clamp(sy + controlSpeed, 0, view.height()-1);

            });
            actionToggle($.the("down"), () -> {
                sy = Util.clamp(sy - controlSpeed, 0, view.height()-1);
            });
        }

        actionToggle($.the("right"), ()->{
            sx = Util.clamp(sx + controlSpeed, 0, view.width()-1);
        });
        actionToggle($.the("left"), ()->{
            sx = Util.clamp(sx - controlSpeed, 0, view.width()-1);
        });

        this.cam = new Bitmap2DSensor((Term)null, view, nar);
        senseNumber($.the("x"), ()->sx/(view.width()-1));
        senseNumber($.the("y"), ()->sy/(view.height()-1));


        randomize();
    }

    protected void randomize() {
        this.tx = random().nextInt(view.width());
        this.sx = random().nextInt(view.width());
        if (view.height() > 1) {
            this.ty = random().nextInt(view.height());
            this.sy = random().nextInt(view.height());
        } else {
            this.ty = this.sy = 0;
        }
    }

    protected void update() {
        synchronized (view) {
            this.tx = Util.clamp(tx + 2 * targetSpeed * (random().nextFloat()-0.5f), 0, view.width() - 1);
            if (view.height() > 1)
                this.ty = Util.clamp(ty + 2 * targetSpeed * (random().nextFloat()-0.5f), 0, view.height() - 1);
        }
    }

    @Override
    protected synchronized float act() {

        synchronized (view) {
            view.set((x,y)->{
                float dist = (float) Math.sqrt(Util.sqr(tx-x) + Util.sqr(ty-y));
                return Math.max(0, 1-dist* visionContrast);
            });
            update();
        }

        cam.input();



        float dist = (float) Math.sqrt(Util.sqr(tx-sx) + Util.sqr(ty-sy));


        //return 1f/(1f+dist);
        return controlSpeed - dist; //controlSpeed is margin of tolerance

    }
}

